package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Job51PlaywrightHandler extends AbstractPlatformPlaywrightHandler {

    public static final String PLATFORM = "51job";
    public static final String HOME_URL = "https://www.51job.com";
    public static final String DOMAIN = "51job.com";

    private volatile boolean monitoringPaused = false;

    public Job51PlaywrightHandler(PlaywrightAutomationContext automationContext) {
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

            if (checkLoggedIn(page)) {
                setLoggedIn(userId, true);
                return;
            }

            try {
                page.navigate(HOME_URL, new Page.NavigateOptions()
                        .setTimeout(60000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                Locator loginEntry = page.locator("span.login.loginBtnClick, text=/登录\\/注册|登录|注册/").first();
                if (loginEntry.isVisible()) {
                    loginEntry.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
                }
            } catch (Exception e) {
                log.debug("Open 51job login entry failed: {}", e.getMessage());
            }

            page.navigate("https://login.51job.com/login.php", new Page.NavigateOptions()
                    .setTimeout(60000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            Locator wechatScanBtn = page.locator(
                    "i.passIcon.custom-cursor-on-hover[data-sensor-id='sensor_login_wechatScan'], " +
                            "i.passIcon[data-sensor-id='sensor_login_wechatScan'], " +
                            "[data-sensor-id='sensor_login_wechatScan']"
            ).first();

            if (wechatScanBtn.isVisible()) {
                wechatScanBtn.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
            }
            waitForLoginAsync(userId, page);
        } catch (Exception e) {
            log.error("Trigger 51job login failed: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("触发51job登录流程失败", e);
        }
    }

    @Override
    public boolean checkLoggedIn(Page page) {
        try {
            Locator loginBtn = page.locator("span.login.loginBtnClick").first();
            if (loginBtn.isVisible()) {
                String txt = (loginBtn.textContent() == null ? "" : loginBtn.textContent()).trim();
                if (txt.contains("登录")) {
                    return false;
                }
            }
            if (isPersonalMyJobUrl(page.url())) {
                return true;
            }
            Locator userAnchor = page.locator("a.uname.e_icon.at");
            if (userAnchor.count() > 0 && userAnchor.first().isVisible()) {
                return true;
            }
            Locator myJobLink = page.locator("a[href*='/pc/my/myjob']");
            if (myJobLink.count() > 0 && myJobLink.first().isVisible()) {
                return true;
            }
            return page.locator(".login-info, .user-info, .username").count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPersonalMyJobUrl(String url) {
        return url != null
                && url.startsWith("https://we.51job.com/")
                && url.contains("/pc/my/myjob");
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
                    log.debug("Check user 51job login status failed: userId={}, error={}", userId, e.getMessage());
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
