package com.communitcation.rest.service;

import com.communitcation.rest.client.CommunicationInfo;
import com.communitcation.rest.client.RequestFormat;
import com.communitcation.rest.session.CacheEntry;
import com.communitcation.rest.session.SessionConfig;
import com.communitcation.rest.exception.RateLimitException;
import com.communitcation.rest.lock.ReqeustLock;
import com.communitcation.rest.model.ImageRequest;
import com.communitcation.rest.model.PollinationsRequest;
import com.communitcation.rest.util.TimeUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateImageService {

    private final CommunicationService communicationService;

    @Value("${pollination.publish-key}")
    private String POLLINATION_PUBLISH_KEY;

    private final Map<String, Map<String, CacheEntry>> sessionCache;

    private String translateKoToEng(String prompt) throws IOException {
        String message = """
              Translate Korean to English, respond only translation: %s
              """.formatted(prompt);

        CommunicationInfo communicationInfo = CommunicationInfo
                .builder()
                .scheme("https")
                .host("text.pollinations.ai")
                .path("/" + message)
                .method(Method.GET)
                .requestFormat(RequestFormat.PATH)
                .build();

        String translatePrompt = communicationService.communicate(communicationInfo);
        String translated = """
                [%s]\ntranslate: %s""".formatted(prompt, translatePrompt);
        log.info(translated);
        return translatePrompt;
    }

    @ReqeustLock
    public String generateImageUrl(ImageRequest request, HttpSession session) throws IOException {
        String sessionId = session.getId();
        String prompt = request.getPrompt();

        long now = System.currentTimeMillis();

        Map<String, CacheEntry> cache
                = sessionCache.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());

        CacheEntry cached = cache.get(prompt);


        if (cached != null && TimeUtil.difference(now, cached.timestamp()) < SessionConfig.CACHE_TTL) {
            return cached.imageUrl();
        }

        long lastRequestTime = cache.values().stream()
                .mapToLong(CacheEntry::timestamp)
                .max()
                .orElse(0);

        if ((now - lastRequestTime) < SessionConfig.COOL_DOWN) {
            long waitSec = (SessionConfig.COOL_DOWN - TimeUtil.difference(now, lastRequestTime)) / 1000 + 1;
            throw new RateLimitException(waitSec);
        }

        String message = translateKoToEng(request.getPrompt());

        CommunicationInfo communicationInfo = CommunicationInfo
                .builder()
                .scheme("https")
                .host("gen.pollinations.ai")
                .path("/image/" + message)
                .method(Method.GET)
                .requestFormat(RequestFormat.QUERY_PARAM)
                .headers(Map.of(
                        "Authorization", "Bearer " + POLLINATION_PUBLISH_KEY
                ))
                .responseClazz(byte[].class)
                .build();

        for (PollinationsRequest.Model model : PollinationsRequest.Model.values()) {
            PollinationsRequest pollinationsRequest
                    = PollinationsRequest.builder()
                    .model(model.getCode())
                    .width(512)
                    .height(512)
                    .seed(0)
                    .enhance(false)
                    .build();

            communicationInfo.setRequestData(pollinationsRequest);

            byte[] imageBytes = communicationService.communicate(communicationInfo);

            if (imageBytes == null) {
               log.warn("이미지 생성 실패: {}",model.getCode());
            }
            else {
                String base64 = Base64.getEncoder().encodeToString(imageBytes);

                String imageUrl = "data:image/png;base64," + base64;

                cache.put(prompt, new CacheEntry(imageUrl, now));

                return imageUrl;
            }
        }
        return null;
    }

}


