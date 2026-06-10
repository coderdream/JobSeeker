package com.wh.jobsbackend.worker.session;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

public class PlatformRuntime {
    private final UserAutomationRegistry registry;
    private final ContextFactory contextFactory;
    private final ContextInitializer contextInitializer;
    private final java.util.concurrent.ConcurrentMap<String, Object> platformLocks = new java.util.concurrent.ConcurrentHashMap<>();

    public PlatformRuntime(UserAutomationRegistry registry) {
        this(registry, (userId, platform, context) -> {
        });
    }

    public PlatformRuntime(UserAutomationRegistry registry, ContextInitializer contextInitializer) {
        this(registry, (session, platform) -> session.getBrowser().newContext(), contextInitializer);
    }

    public PlatformRuntime(UserAutomationRegistry registry, ContextFactory contextFactory, ContextInitializer contextInitializer) {
        this.registry = registry;
        this.contextFactory = contextFactory;
        this.contextInitializer = contextInitializer;
    }

    public Page getOrCreatePage(Long userId, String platform) {
        UserAutomationSession session = registry.getOrCreate(userId);
        return session.getPages().compute(platform, (key, existingPage) -> {
            if (existingPage != null && !isClosed(existingPage)) {
                return existingPage;
            }
            try {
                return newPage(session, userId, key);
            } catch (RuntimeException e) {
                if (!isTargetClosed(e)) {
                    throw e;
                }
                discardContext(session, key);
                return newPage(session, userId, key);
            }
        });
    }

    public BrowserContext getOrCreateContext(Long userId, String platform) {
        UserAutomationSession session = registry.getOrCreate(userId);
        return getOrCreateContext(session, userId, platform);
    }

    public Page getExistingOpenPage(Long userId, String platform) {
        UserAutomationSession session = registry.getExisting(userId);
        if (session == null) {
            return null;
        }
        Page page = session.getPages().get(platform);
        if (page == null) {
            return null;
        }
        if (!isClosed(page)) {
            return page;
        }
        session.getPages().remove(platform, page);
        return null;
    }

    public BrowserContext getExistingContext(Long userId, String platform) {
        UserAutomationSession session = registry.getExisting(userId);
        if (session == null) {
            return null;
        }
        return session.getContexts().get(platform);
    }

    private BrowserContext getOrCreateContext(UserAutomationSession session, Long userId, String platform) {
        return session.getContexts().computeIfAbsent(platform, key -> {
            BrowserContext context = contextFactory.create(session, key);
            contextInitializer.initialize(userId, platform, context);
            return context;
        });
    }

    public boolean isLoggedIn(Long userId, String platform) {
        return registry.getOrCreate(userId).getLoginStatus().getOrDefault(platform, false);
    }

    public boolean getCachedLoginStatus(Long userId, String platform) {
        UserAutomationSession session = registry.getExisting(userId);
        return session != null && session.getLoginStatus().getOrDefault(platform, false);
    }

    public void setLoginStatus(Long userId, String platform, boolean loggedIn) {
        registry.getOrCreate(userId).getLoginStatus().put(platform, loggedIn);
    }

    public Object getPlatformLock(Long userId, String platform) {
        return platformLocks.computeIfAbsent(userId + ":" + platform, key -> new Object());
    }

    private boolean isClosed(Page page) {
        try {
            return page.isClosed();
        } catch (Exception e) {
            return true;
        }
    }

    private Page newPage(UserAutomationSession session, Long userId, String platform) {
        return getOrCreateContext(session, userId, platform).newPage();
    }

    private void discardContext(UserAutomationSession session, String platform) {
        BrowserContext context = session.getContexts().remove(platform);
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Exception ignored) {
        }
    }

    private boolean isTargetClosed(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage();
            if (className.contains("TargetClosed")
                    || message != null && message.contains("Target page, context or browser has been closed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public void closeUser(Long userId) {
        registry.closeUser(userId);
        platformLocks.keySet().removeIf(key -> key.startsWith(userId + ":"));
    }

    @FunctionalInterface
    public interface ContextInitializer {
        void initialize(Long userId, String platform, BrowserContext context);
    }

    @FunctionalInterface
    public interface ContextFactory {
        BrowserContext create(UserAutomationSession session, String platform);
    }
}
