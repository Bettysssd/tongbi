package com.tongbi.analysis;

import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionStore {
    private final ConcurrentMap<String, AnalysisSession> sessions = new ConcurrentHashMap<String, AnalysisSession>();

    public void save(AnalysisSession session) {
        sessions.put(session.getId(), session);
    }

    public AnalysisSession get(String id) {
        AnalysisSession session = sessions.get(id);
        if (session == null) {
            throw new NoSuchElementException("找不到这次分析，可能是服务重启了。");
        }
        return session;
    }
}
