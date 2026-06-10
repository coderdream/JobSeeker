package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.yupao.YupaoPageModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

@Slf4j
public class YupaoPlaywrightHandler extends AbstractPlatformPlaywrightHandler {
    public static final String PLATFORM = "yupao";
    public static final String HOME_URL = "https://www.yupao.com";
    public static final String LOGIN_URL = YupaoPageModel.LOGIN_URL;
    public static final String DOMAIN = "yupao.com";

    private static final String LOGIN_ENTRY_SELECTOR = "a:has-text(\"登录\"), button:has-text(\"登录\"), [class*='login']";
    private static final String USER_ENTRY_SELECTOR = "a:has-text(\"个人中心\"), a:has-text(\"我的\"), [class*='user'], [class*='avatar']";

    private static final String LOGIN_ENTRY_SELECTOR_UTF8 = "a:has-text(\"\\u767b\\u5f55\"), " +
            "button:has-text(\"\\u767b\\u5f55\"), text=/\\u767b\\u5f55|\\u6ce8\\u518c/, [class*='login']";
    private static final String USER_ENTRY_SELECTOR_UTF8 = "a:has-text(\"\\u4e2a\\u4eba\\u4e2d\\u5fc3\"), " +
            "a:has-text(\"\\u6211\\u7684\"), [class*='user'], [class*='avatar']";

    private volatile boolean monitoringPaused = false;

    public YupaoPlaywrightHandler(PlaywrightAutomationContext automationContext) {
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
            page.navigate(LOGIN_URL, new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            setLoggedIn(userId, false);
            waitForLoginAsync(userId, page);
        } catch (Exception e) {
            log.error("Trigger yupao login failed: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("触发鱼泡登录流程失败", e);
        }
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        if (page == null) {
            return false;
        }
        String currentUrl = pageUrl(page);
        if (YupaoPageModel.isLoggedInUrl(currentUrl)) {
            return true;
        }
        if (hasAuthenticatedSession(page)) {
            return true;
        }
        if (isBlankUrl(currentUrl) || YupaoPageModel.isLoginUrl(currentUrl)) {
            return false;
        }
        try {
            if (inspectServerRenderedLoginFlag(page)) {
                return true;
            }
            Locator userEntry = page.locator(USER_ENTRY_SELECTOR_UTF8);
            int userCount = Math.min(userEntry.count(), 5);
            for (int i = 0; i < userCount; i++) {
                Locator candidate = userEntry.nth(i);
                if (candidate.isVisible()) {
                    String text = candidate.textContent();
                    if (text == null || !text.toLowerCase().contains("login")) {
                        return true;
                    }
                }
            }
            Locator loginEntry = page.locator(LOGIN_ENTRY_SELECTOR_UTF8).first();
            if (loginEntry.count() > 0 && loginEntry.isVisible()) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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
                    .filter(this::isYupaoCookie)
                    .map(cookie -> cookie.name)
                    .toList();
            return YupaoPageModel.hasAuthenticatedCookieNames(cookieNames);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isYupaoCookie(Cookie cookie) {
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
                            if (!monitoringPaused) {
                                automationContext.updateLoginStatusFromPage(userId, this, page);
                            }
                        }
                    });
                } catch (Exception e) {
                    log.debug("Check user yupao login status failed: userId={}, error={}", userId, e.getMessage());
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

    private boolean inspectServerRenderedLoginFlag(Page page) {
        try {
            String content = page.content();
            if (content == null || content.isBlank()) {
                return false;
            }
            String compact = content.replaceAll("\\s+", "");
            return compact.contains("\"isLogged\":true")
                    || compact.contains("\"isLoggedIn\":true")
                    || compact.contains("\"loggedIn\":true")
                    || compact.contains("\"loginStatus\":true");
        } catch (Exception e) {
            return false;
        }
    }
}
