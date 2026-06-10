package com.wh.jobsbackend.worker.session;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class UserAutomationSession {
    private final Long userId;
    private final Browser browser;
    private final boolean ownsBrowser;
    private final Map<String, BrowserContext> contexts = new ConcurrentHashMap<>();
    private final Map<String, Page> pages = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loginStatus = new ConcurrentHashMap<>();

    public UserAutomationSession(Long userId, Browser browser) {
        this(userId, browser, true);
    }

    public UserAutomationSession(Long userId, Browser browser, boolean ownsBrowser) {
        this.userId = userId;
        this.browser = browser;
        this.ownsBrowser = ownsBrowser;
    }

    public void close() {
        pages.values().forEach(page -> {
            try {
                page.close();
            } catch (Exception ignored) {
            }
        });
        contexts.values().forEach(context -> {
            try {
                context.close();
            } catch (Exception ignored) {
            }
        });
        if (ownsBrowser) {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
        }
    }
}
