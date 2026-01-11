package com.communitcation.rest.session;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCacheListener implements HttpSessionListener {

    private final Map<String, Map<String, CacheEntry>> sessionCache;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String sessionId = event.getSession().getId();
        Map<String, CacheEntry> removed = sessionCache.remove(sessionId);

        if (removed == null || removed.isEmpty()) {
            return;
        }

        log.info("Session destroyed, removed {} cached entries for session: {}",
                removed.size(),
                sessionId
        );
    }
}
