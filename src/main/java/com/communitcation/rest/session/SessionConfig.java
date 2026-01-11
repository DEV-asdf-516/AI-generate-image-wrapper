package com.communitcation.rest.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class SessionConfig {

    public static final long CACHE_TTL = 60_000 * 3;
    public static final long COOL_DOWN = 15_000;


    /**
     * 세션별 프롬프트 캐시
     * Key: sessionId
     * Value: Map<String, CacheEntry>
     */
    @Bean
    public Map<String, Map<String, CacheEntry>> sessionCache() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 동시 요청 방지용 락
     * Key: sessionId
     */
    @Bean
    public Map<String, ReentrantLock> inProgressLock() {
        return new ConcurrentHashMap<>();
    }
}