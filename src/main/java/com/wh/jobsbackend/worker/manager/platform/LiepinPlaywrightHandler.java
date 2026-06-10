package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LiepinPlaywrightHandler extends AbstractPlatformPlaywrightHandler {

    public static final String PLATFORM = "liepin";
    public static final String HOME_URL = "https://www.liepin.com";
    public static final String DOMAIN = "liepin.com";

    private volatile boolean monitoringPaused = false;

    public LiepinPlaywrightHandler(PlaywrightAutomationContext automationContext) {
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
            page.navigate("https://www.liepin.com/login", new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            if (checkLoggedIn(page)) {
                setLoggedIn(userId, true);
                saveCookies(userId, "login success");
                return;
            }

            try {
                Locator qrSwitch = page.locator(".switch-type-mask-img-box, img[src*='qrcode-btn']").first();
                if (qrSwitch.isVisible()) {
                    qrSwitch.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
                }
            } catch (Exception e) {
                log.debug("Open liepin qr login failed: {}", e.getMessage());
            }

            waitForLoginAsync(userId, page);
        } catch (Exception e) {
            log.error("Trigger liepin login failed: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("触发猎聘登录流程失败", e);
        }
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        try {
            String url = page.url();
            if (url != null && (url.contains("/cresume/") || url.contains("/resume/") || url.contains("/user/"))) {
                return true;
            }
            if (page.locator(".user-info, .user-name, .header-user, [class*='user']").count() > 0) {
                return true;
            }
            Locator loginButton = page.locator("text=/登录|注册/").first();
            if (loginButton.count() > 0 && loginButton.isVisible()) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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
                    log.debug("Check user liepin login status failed: userId={}, error={}", userId, e.getMessage());
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
