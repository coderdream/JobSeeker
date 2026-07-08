package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.worker.boss.BossPageModel;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

@Slf4j
public class BossPlaywrightHandler extends AbstractPlatformPlaywrightHandler {

    public static final String PLATFORM = "boss";
    public static final String HOME_URL = "https://www.zhipin.com";
    public static final String DOMAIN = "zhipin.com";

    private volatile boolean monitoringPaused = false;

    public BossPlaywrightHandler(PlaywrightAutomationContext automationContext) {
        super(automationContext);
    }

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String homeUrl() {
        return HOME_URL;
    }

    @Override
    public String domain() {
        return DOMAIN;
    }

    @Override
    public void triggerLogin(Long userId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                openLoginPage(userId, attempt > 0);
                return;
            } catch (Exception e) {
                if (attempt == 0 && isTargetClosed(e)) {
                    log.warn("Boss page was closed; recreating page and retrying login: userId={}", userId);
                    automationContext.resetPlatform(userId, PLATFORM);
                    continue;
                }
                log.error("Trigger boss login failed: userId={}, error={}", userId, e.getMessage(), e);
                throw new RuntimeException("Trigger Boss login flow failed", e);
            }
        }
    }

    private void openLoginPage(Long userId, boolean recovered) {
        Page page = page(userId);
        page.setDefaultTimeout(PlaywrightManager.DEFAULT_TIMEOUT);
        setupMonitoring(userId, page);

        String prefix = recovered ? "Opening Boss login page after recovery" : "Opening Boss login page";
        log.info("{}: userId={}, currentUrl={}", prefix, userId, pageUrl(page));
        page.navigate(BossPageModel.LOGIN_URL, new Page.NavigateOptions()
                .setTimeout(60000)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.bringToFront();
        boolean loggedIn = hasAuthenticatedSession(page) || checkLoggedIn(page);
        setLoggedIn(userId, loggedIn);
        if (loggedIn) {
            saveCookies(userId, "login success");
        }
        log.info("Boss login page opened: userId={}, currentUrl={}, loggedIn={}", userId, pageUrl(page), loggedIn);
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        if (page == null) {
            return false;
        }
        String currentUrl = pageUrl(page);
        if (hasAuthenticatedSession(page)) {
            return true;
        }
        if (BossPageModel.isLoggedInUrl(currentUrl)) {
            return true;
        }
        if (isBlankUrl(currentUrl) || BossPageModel.isLoginUrl(currentUrl)) {
            return false;
        }
        return false;
    }

    private String pageUrl(Page page) {
        try {
            return page.url();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isBlankUrl(String url) {
        return url == null || url.isBlank() || "about:blank".equalsIgnoreCase(url);
    }

    private boolean hasAuthenticatedSession(Page page) {
        try {
            List<String> cookieNames = page.context().cookies().stream()
                    .filter(this::isBossCookie)
                    .map(cookie -> cookie.name)
                    .toList();
            return BossPageModel.hasAuthenticatedCookieNames(cookieNames);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isBossCookie(Cookie cookie) {
        if (cookie == null || cookie.domain == null || cookie.domain.isBlank()) {
            return false;
        }
        String domain = cookie.domain.toLowerCase(Locale.ROOT);
        return domain.equals(DOMAIN) || domain.endsWith("." + DOMAIN);
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

    @Override
    public void setupMonitoring(Long userId, Page page) {
        page.onFrameNavigated(frame -> {
            if (frame == page.mainFrame() && !monitoringPaused) {
                try {
                    automationContext.withPlaywrightLock(() -> {
                        synchronized (automationContext.platformLock(userId, PLATFORM)) {
                            automationContext.updateLoginStatusFromPage(userId, this, page);
                        }
                    });
                } catch (Exception e) {
                    log.debug("Check user boss login status failed: userId={}, error={}", userId, e.getMessage());
                }
            }
        });
    }

    @Override
    public void pauseMonitoring() {
        monitoringPaused = true;
    }

    @Override
    public void resumeMonitoring() {
        monitoringPaused = false;
    }
}
