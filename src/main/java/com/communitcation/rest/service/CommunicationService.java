package com.communitcation.rest.service;

import com.communitcation.rest.client.CommunicationInfo;
import com.communitcation.rest.client.RequestFormat;
import com.communitcation.rest.client.RestTemplateComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Method;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import static com.communitcation.rest.util.JsonUtil.toJson;

@Service
@RequiredArgsConstructor
public class CommunicationService {

    private List<Method> paramList = List.of(Method.GET, Method.DELETE);
    private List<Method> bodyList = List.of(Method.POST, Method.PUT);

    private final RestTemplateComponent restClient;

    // 통신 메서드
    public <T> T communicate(CommunicationInfo communicationInfo) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try{
            RequestFormat format = findRequestFormat(communicationInfo.getMethod(),communicationInfo.getRequestFormat());
            communicationInfo.setRequestFormat(format);
            ResponseEntity<?> responseData = getResponse(communicationInfo);
            return (T) responseData.getBody();
        }catch (HttpClientErrorException | HttpServerErrorException  e){
            e.printStackTrace();
        }
        finally {
            httpClient.close();
        }
        return null;
    }

    // 요청 형태 설정
    private RequestFormat findRequestFormat(Method method,RequestFormat format){
        if(format == null){
            if(paramList.contains(method)){
                return  RequestFormat.QUERY_PARAM;
            }
            else{
                return RequestFormat.BODY;
            }
        }
        return format;
    }

    // 헤더 생성
    private HttpHeaders createHeaders(CommunicationInfo communicationInfo){
        HttpHeaders headers;
        RequestFormat format = communicationInfo.getRequestFormat();
        Map<String,String> customHeaders = communicationInfo.getHeaders();
        if(format == RequestFormat.QUERY_PARAM){
            headers =  restClient.createQueryParamHeader();
        }
        else {
            if(communicationInfo.getMediaType() == null){
                headers = restClient.createBodyHeader(MediaType.APPLICATION_JSON);
            }
            else {
                headers = restClient.createBodyHeader(communicationInfo.getMediaType());
            }
        }
        if (customHeaders != null){
            MultiValueMap<String,String> multiValueMapHeader = new LinkedMultiValueMap<>();
            multiValueMapHeader.setAll(customHeaders);
            headers.addAll(multiValueMapHeader);
        }
        return headers;
    }

    // uri 생성
    private String createUri(CommunicationInfo communicationInfo){
        RequestFormat format = communicationInfo.getRequestFormat();
        if(format == RequestFormat.QUERY_PARAM){
            return  UriComponentsBuilder
                    .newInstance()
                    .scheme(communicationInfo.getScheme())
                    .host(communicationInfo.getHost())
                    .port(communicationInfo.getPort())
                    .path(communicationInfo.getPath())
                    .queryParams(convertBodyToMultiValueMap(toJson(communicationInfo.getRequestData())))
                    .build()
                    .encode()
                    .toUriString();
        }
        else if(format == RequestFormat.PATH) {
            return communicationInfo.getScheme() + "://"
                    + communicationInfo.getHost()
                    + communicationInfo.getPath();
        }
        else {
            return UriComponentsBuilder
                    .newInstance()
                    .scheme(communicationInfo.getScheme())
                    .host(communicationInfo.getHost())
                    .port(communicationInfo.getPort())
                    .path(communicationInfo.getPath())
                    .build()
                    .encode()
                    .toUriString();
        }
    }

    // response 
    private <T> ResponseEntity<?>  getResponse(CommunicationInfo communicationInfo) {
        Method method = communicationInfo.getMethod();
        HttpHeaders headers = createHeaders(communicationInfo); // 헤더 생성

        String uri = createUri(communicationInfo); // uri 생성

        Class<?> clazz = communicationInfo.getResponseClazz();

        if(clazz == null){
            clazz = String.class;
        }

        RequestFormat format = communicationInfo.getRequestFormat();

        Object requestData = (format == RequestFormat.QUERY_PARAM)?
                        null : communicationInfo.getRequestData();

        ResponseEntity<?> response
                = byMethod(restClient,method,headers,uri,requestData,clazz);

        if (response == null) {
            throw new NullPointerException();
        }

        return response;
    }

    // 메서드 유형에 따라서 GET, POST, PUT, DELETE 호출
    private  ResponseEntity<?>  byMethod(
            RestTemplateComponent client,
            Method method,
            HttpHeaders headers,
            String uri,
            @Nullable Object body,
            Class<?> clazz
        ){
       return switch(method){
           case GET -> client.getMethod(headers,uri,clazz);
           case POST -> client.postMethod(headers,uri,body,clazz);
           case PUT -> client.putMethod(headers,uri,body,clazz);
           case DELETE -> client.deleteMethod(headers,uri,clazz);
           default -> null;
        };
    }

    // query param 생성
    public MultiValueMap<String, String> convertBodyToMultiValueMap(String body){
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(body);
            jsonNode.fields().forEachRemaining(entry -> {
                multiValueMap.add(entry.getKey(), entry.getValue().asText());
            });
        }catch(Exception e){
            e.printStackTrace();
        }
        return multiValueMap;
    }
}