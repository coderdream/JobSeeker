package com.wh.jobsbackend.worker.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.worker.manager.platform.PlatformPlaywrightHandler;
import com.wh.jobsbackend.worker.session.PlatformRuntime;
import com.wh.jobsbackend.worker.session.UserAutomationRegistry;
import com.wh.jobsbackend.worker.session.UserAutomationSession;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class PlaywrightAutomationContext {

    private final CookieService cookieService;
    private final Supplier<Browser> browserSupplier;
    private final Runnable initializer;
    private final PlatformRuntime.ContextFactory contextFactory;
    private final PlatformRuntime.ContextInitializer contextInitializer;
    private final List<Consumer<PlaywrightManager.LoginStatusChange>> loginStatusListeners = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object playwrightLock = new Object();
    private final Object runtimeLock = new Object();
    private volatile PlatformRuntime platformRuntime;

    public PlaywrightAutomationContext(CookieService cookieService,
                                       Supplier<Browser> browserSupplier,
                                       Runnable initializer,
                                       PlatformRuntime.ContextFactory contextFactory,
                                       PlatformRuntime.ContextInitializer contextInitializer) {
        this.cookieService = cookieService;
        this.browserSupplier = browserSupplier;
        this.initializer = initializer;
        this.contextFactory = contextFactory;
        this.contextInitializer = contextInitializer;
    }

    public PlatformRuntime runtime() {
        ensureRuntime();
        return platformRuntime;
    }

    public void setRuntime(PlatformRuntime platformRuntime) {
        synchronized (runtimeLock) {
            this.platformRuntime = platformRuntime;
        }
    }

    public Object playwrightLock() {
        return playwrightLock;
    }

    public void withPlaywrightLock(Runnable action) {
        synchronized (playwrightLock) {
            action.run();
        }
    }

    public <T> T withPlaywrightLock(Supplier<T> action) {
        synchronized (playwrightLock) {
            return action.get();
        }
    }

    public Page getPage(Long userId, PlatformPlaywrightHandler handler) {
        return withPlaywrightLock(() -> {
            ensureRuntime();
            return platformRuntime.getOrCreatePage(userId, handler.platform());
        });
    }

    public BrowserContext getContext(Long userId, PlatformPlaywrightHandler handler) {
        return withPlaywrightLock(() -> {
            ensureRuntime();
            return platformRuntime.getOrCreateContext(userId, handler.platform());
        });
    }

    public Object platformLock(Long userId, String platform) {
        ensureRuntime();
        return platformRuntime.getPlatformLock(userId, platform);
    }

    public boolean isLoggedIn(Long userId, String platform) {
        ensureRuntime();
        return platformRuntime.isLoggedIn(userId, platform);
    }

    public void setLoginStatus(Long userId, String platform, boolean loggedIn) {
        ensureRuntime();
        boolean previousStatus = platformRuntime.isLoggedIn(userId, platform);
        platformRuntime.setLoginStatus(userId, platform, loggedIn);
        if (previousStatus != loggedIn) {
            notifyLoginStatus(new PlaywrightManager.LoginStatusChange(userId, platform, loggedIn, System.currentTimeMillis()));
        }
    }

    public void addLoginStatusListener(Consumer<PlaywrightManager.LoginStatusChange> listener) {
        loginStatusListeners.add(listener);
    }

    public void removeLoginStatusListener(Consumer<PlaywrightManager.LoginStatusChange> listener) {
        loginStatusListeners.remove(listener);
    }

    public boolean refreshLoginStatus(Long userId, PlatformPlaywrightHandler handler) {
        ensureRuntime();
        return withPlaywrightLock(() -> {
            synchronized (platformRuntime.getPlatformLock(userId, handler.platform())) {
                try {
                    Page page = platformRuntime.getExistingOpenPage(userId, handler.platform());
                    List<Page> pages = pagesToInspect(userId, handler, page);
                    if (pages.isEmpty()) {
                        return isLoggedIn(userId, handler.platform());
                    }
                    return updateLoginStatusFromPages(userId, handler, pages);
                } catch (Exception e) {
                    log.warn("Refresh login status failed: userId={}, platform={}, error={}", userId, handler.platform(), e.getMessage());
                    return isLoggedIn(userId, handler.platform());
                }
            }
        });
    }

    private List<Page> pagesToInspect(Long userId, PlatformPlaywrightHandler handler, Page primaryPage) {
        Set<Page> pages = new LinkedHashSet<>();
        if (isOpenPage(primaryPage)) {
            pages.add(primaryPage);
        }
        try {
            BrowserContext context = platformRuntime.getExistingContext(userId, handler.platform());
            if (context != null) {
                for (Page page : context.pages()) {
                    if (isOpenPage(page)) {
                        pages.add(page);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("List platform pages failed: userId={}, platform={}, error={}", userId, handler.platform(), e.getMessage());
        }
        return new ArrayList<>(pages);
    }

    private boolean updateLoginStatusFromPages(Long userId, PlatformPlaywrightHandler handler, List<Page> pages) {
        boolean previous = isLoggedIn(userId, handler.platform());
        boolean detected = false;
        for (Page page : pages) {
            try {
                page.setDefaultTimeout(PlaywrightManager.DEFAULT_TIMEOUT);
                if (handler.checkLoggedIn(page)) {
                    detected = true;
                    break;
                }
            } catch (Exception e) {
                log.debug("Check login status page failed: userId={}, platform={}, error={}", userId, handler.platform(), e.getMessage());
            }
        }
        if (previous != detected) {
            setLoginStatus(userId, handler.platform(), detected);
        }
        if (detected && !previous) {
            saveCookiesToDb(userId, handler, "login success");
        }
        return detected;
    }

    private boolean isOpenPage(Page page) {
        if (page == null) {
            return false;
        }
        try {
            return !page.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateLoginStatusFromPage(Long userId, PlatformPlaywrightHandler handler, Page page) {
        boolean previous = isLoggedIn(userId, handler.platform());
        boolean detected = handler.checkLoggedIn(page);
        if (previous && !detected) {
            return true;
        }
        if (previous != detected) {
            setLoginStatus(userId, handler.platform(), detected);
        }
        if (detected && !previous) {
            saveCookiesToDb(userId, handler, "login success");
        }
        return detected;
    }

    public void saveCookiesToDb(Long userId, PlatformPlaywrightHandler handler, String remark) {
        ensureRuntime();
        withPlaywrightLock(() -> {
        synchronized (platformRuntime.getPlatformLock(userId, handler.platform())) {
            try {
                BrowserContext userContext = getContext(userId, handler);
                List<Cookie> cookies = filterCookiesByDomain(userContext.cookies(), handler.domain());
                String cookieJson = objectMapper.writeValueAsString(cookies);
                cookieService.saveOrUpdateCookie(userId, handler.platform(), cookieJson, remark);
            } catch (Exception e) {
                throw new RuntimeException("保存Cookie失败: " + handler.platform(), e);
            }
        }
        });
    }

    public void loadCookies(Long userId, String platform, BrowserContext targetContext, String domain) {
        try {
            CookieEntity cookieEntity = cookieService.getCookieByPlatform(userId, platform);
            if (cookieEntity == null || cookieEntity.getCookieValue() == null || cookieEntity.getCookieValue().isBlank()) {
                return;
            }
            List<Cookie> cookies = filterCookiesByDomain(parseCookiesFromString(cookieEntity.getCookieValue()), domain);
            if (!cookies.isEmpty()) {
                targetContext.addCookies(cookies);
            }
        } catch (Exception e) {
            log.warn("Load cookies failed: userId={}, platform={}, error={}", userId, platform, e.getMessage());
        }
    }

    public List<Cookie> parseCookiesFromString(String cookieJson) {
        List<Cookie> cookies = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.JsonNode jsonArray = objectMapper.readTree(cookieJson);
            for (com.fasterxml.jackson.databind.JsonNode node : jsonArray) {
                Cookie cookie = new Cookie(node.get("name").asText(), node.get("value").asText());
                if (node.has("domain") && !node.get("domain").isNull()) {
                    cookie.domain = node.get("domain").asText();
                }
                if (node.has("path") && !node.get("path").isNull()) {
                    cookie.path = node.get("path").asText();
                }
                if (node.has("expires") && !node.get("expires").isNull()) {
                    cookie.expires = node.get("expires").asDouble();
                }
                if (node.has("httpOnly") && !node.get("httpOnly").isNull()) {
                    cookie.httpOnly = node.get("httpOnly").asBoolean();
                }
                if (node.has("secure") && !node.get("secure").isNull()) {
                    cookie.secure = node.get("secure").asBoolean();
                }
                if (node.has("sameSite") && !node.get("sameSite").isNull()) {
                    String sameSite = node.get("sameSite").asText();
                    if (sameSite != null && !sameSite.isEmpty()) {
                        cookie.sameSite = com.microsoft.playwright.options.SameSiteAttribute.valueOf(sameSite.toUpperCase());
                    }
                }
                cookies.add(cookie);
            }
        } catch (Exception e) {
            log.error("解析Cookie JSON失败: {}", e.getMessage(), e);
        }

        return cookies;
    }

    public List<Cookie> filterCookiesByDomain(List<Cookie> cookies, String domainSuffix) {
        if (cookies == null || cookies.isEmpty()) {
            return new ArrayList<>();
        }
        String suffix = domainSuffix == null ? "" : domainSuffix.toLowerCase(Locale.ROOT);
        return cookies.stream()
                .filter(cookie -> matchesDomain(cookie, suffix))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean matchesDomain(Cookie cookie, String suffix) {
        if (cookie == null || cookie.domain == null || cookie.domain.isBlank()) {
            return false;
        }
        String domain = cookie.domain.toLowerCase(Locale.ROOT);
        return domain.equals(suffix) || domain.endsWith("." + suffix);
    }

    private void notifyLoginStatus(PlaywrightManager.LoginStatusChange change) {
        loginStatusListeners.forEach(listener -> {
            try {
                listener.accept(change);
            } catch (Exception e) {
                log.error("通知登录状态监听器失败: userId={}, platform={}, isLoggedIn={}",
                        change.userId(), change.platform(), change.isLoggedIn(), e);
            }
        });
    }

    private void ensureRuntime() {
        if (platformRuntime != null) {
            return;
        }
        synchronized (runtimeLock) {
            if (platformRuntime != null) {
                return;
            }
            Browser browser = browserSupplier.get();
            if (browser == null) {
                initializer.run();
                browser = browserSupplier.get();
            }
            Browser finalBrowser = Objects.requireNonNull(browser, "Browser is not initialized");
            UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, finalBrowser, false));
            platformRuntime = new PlatformRuntime(registry, contextFactory, contextInitializer);
        }
    }
}
