package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public class ZhilianPlaywrightHandler extends AbstractPlatformPlaywrightHandler {

    public static final String PLATFORM = "zhilian";
    public static final String HOME_URL = "https://www.zhaopin.com";
    public static final String DOMAIN = "zhaopin.com";
    private static final String LOGIN_ENTRY_SELECTOR = "a.home-header__c-no-login";
    private static final String USER_IDENTITY_SELECTOR = ".user-info, .user-name, .username-text, " +
            ".home-header__c-user, .home-header__c-name, [class*='user-name'], " +
            "[class*='username'], [class*='UserName']";
    private static final String AUTHENTICATED_ENTRY_SELECTOR = "a[href*='i.zhaopin.com'], " +
            "a[href*='/resume'], .home-header__c-avatar, [class*='avatar'], [class*='Avatar']";

    private volatile boolean monitoringPaused = false;

    public ZhilianPlaywrightHandler(PlaywrightAutomationContext automationContext) {
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
            page.navigate(HOME_URL, new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            Locator noLoginAnchor = page.locator(LOGIN_ENTRY_SELECTOR).first();
            if (noLoginAnchor.isVisible()) {
                Locator qrToggle = page.locator("div.zppp-panel-normal-bar__img").first();
                if (qrToggle.isVisible()) {
                    qrToggle.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
                }
            } else if (checkLoggedIn(page)) {
                setLoggedIn(userId, true);
                saveCookies(userId, "login success");
                return;
            }

            waitForLoginAsync(userId, page);
        } catch (Exception e) {
            log.error("Trigger zhilian login failed: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("触发智联登录流程失败", e);
        }
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        if (page == null) {
            return false;
        }
        try {
            Boolean userIdentity = inspectUserIdentity(page);
            if (Boolean.TRUE.equals(userIdentity)) {
                return true;
            }
            Boolean authenticatedEntry = inspectAuthenticatedEntry(page);
            if (authenticatedEntry != null) {
                return authenticatedEntry;
            }
            if (inspectServerRenderedLoginFlag(page)) {
                return true;
            }
            Locator loginButton = page.locator(LOGIN_ENTRY_SELECTOR).first();
            if (loginButton.count() > 0 && loginButton.isVisible()) {
                return false;
            }
            if (isAccountUrl(page.url())) {
                return true;
            }
            if (Boolean.FALSE.equals(userIdentity)) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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
                    || compact.contains("\"loggedIn\":true");
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean inspectUserIdentity(Page page) {
        try {
            Locator candidates = page.locator(USER_IDENTITY_SELECTOR);
            int count = Math.min(candidates.count(), 5);
            boolean sawInvalidIdentity = false;
            for (int i = 0; i < count; i++) {
                Locator candidate = candidates.nth(i);
                if (!candidate.isVisible()) {
                    continue;
                }
                String text = candidate.textContent();
                if (isValidUserIdentityText(text)) {
                    return true;
                }
                if (isPlaceholderIdentityText(text)) {
                    sawInvalidIdentity = true;
                }
            }
            return sawInvalidIdentity ? Boolean.FALSE : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean inspectAuthenticatedEntry(Page page) {
        try {
            Locator candidates = page.locator(AUTHENTICATED_ENTRY_SELECTOR);
            int count = Math.min(candidates.count(), 5);
            for (int i = 0; i < count; i++) {
                Locator candidate = candidates.nth(i);
                if (!candidate.isVisible()) {
                    continue;
                }
                return true;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidUserIdentityText(String text) {
        return text != null && !text.isBlank() && !isPlaceholderIdentityText(text);
    }

    private boolean isPlaceholderIdentityText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return "undefined".equals(normalized)
                || "undefine".equals(normalized)
                || "null".equals(normalized)
                || "nan".equals(normalized);
    }

    private boolean isAccountUrl(String url) {
        return url != null && url.contains("i.zhaopin.com");
    }

    @Override
    public void setupMonitoring(Long userId, Page page) {
        page.onFrameNavigated(frame -> {
            if (frame == page.mainFrame() && !monitoringPaused) {
                try {
                    automationContext.withPlaywrightLock(() -> {
                        synchronized (automationContext.platformLock(userId, PLATFORM)) {
                            if (monitoringPaused) {
                                return;
                            }
                            automationContext.updateLoginStatusFromPage(userId, this, page);
                        }
                    });
                } catch (Exception e) {
                    log.debug("Check user zhilian login status failed: userId={}, error={}", userId, e.getMessage());
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
