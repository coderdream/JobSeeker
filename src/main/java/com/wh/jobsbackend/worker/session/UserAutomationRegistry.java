package com.wh.jobsbackend.worker.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class UserAutomationRegistry {
    private final Map<Long, UserAutomationSession> sessions = new ConcurrentHashMap<>();
    private final Function<Long, UserAutomationSession> sessionFactory;

    public UserAutomationRegistry(Function<Long, UserAutomationSession> sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public UserAutomationSession getOrCreate(Long userId) {
        return sessions.computeIfAbsent(userId, sessionFactory);
    }

    public UserAutomationSession getExisting(Long userId) {
        return sessions.get(userId);
    }

    public void closeUser(Long userId) {
        UserAutomationSession session = sessions.remove(userId);
        if (session != null) {
            session.close();
        }
    }

    public void closeAll() {
        sessions.keySet().forEach(this::closeUser);
    }
}
