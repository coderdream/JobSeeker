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
        try {
            Page page = page(userId);
            page.setDefaultTimeout(PlaywrightManager.DEFAULT_TIMEOUT);
            setupMonitoring(userId, page);

            if (hasAuthenticatedSession(page)) {
                setLoggedIn(userId, true);
                saveCookies(userId, "login success");
                return;
            }

            page.navigate(BossPageModel.LOGIN_URL, new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            setLoggedIn(userId, false);
        } catch (Exception e) {
            log.error("Trigger boss login failed: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("触发Boss登录流程失败", e);
        }
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        if (page == null) {
            return false;
        }
        String currentUrl = pageUrl(page);
        if (BossPageModel.isLoggedInUrl(currentUrl)) {
            return true;
        }
        if (isBlankUrl(currentUrl) || BossPageModel.isLoginUrl(currentUrl)) {
            return false;
        }
        return hasAuthenticatedSession(page);
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
