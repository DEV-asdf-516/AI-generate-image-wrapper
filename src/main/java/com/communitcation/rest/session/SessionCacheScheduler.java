package com.communitcation.rest.session;

import com.communitcation.rest.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCacheScheduler {

    private final Map<String, Map<String, CacheEntry>> sessionCache;

    @Scheduled(fixedRate = 600_000)
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();

        int removedEntries = 0;
        int removedSessions = 0;

        for (var sessionEntry : sessionCache.entrySet()) {
            Map<String, CacheEntry> cache = sessionEntry.getValue();

            int total = cache.size();

            cache.entrySet().removeIf(
                    entry ->
                    TimeUtil.difference(now, entry.getValue().timestamp()) > SessionConfig.CACHE_TTL
            );

            removedEntries += (total - cache.size());

            if (cache.isEmpty()) {
                sessionCache.remove(sessionEntry.getKey());
                removedSessions++;
            }
        }

        if (removedEntries > 0 || removedSessions > 0) {
            log.debug("Cache cleanup: removed {} entries, {} sessions", removedEntries, removedSessions);
        }
    }

}
