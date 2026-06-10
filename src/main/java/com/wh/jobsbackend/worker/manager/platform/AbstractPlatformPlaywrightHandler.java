package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;

public abstract class AbstractPlatformPlaywrightHandler implements PlatformPlaywrightHandler {

    protected final PlaywrightAutomationContext automationContext;

    protected AbstractPlatformPlaywrightHandler(PlaywrightAutomationContext automationContext) {
        this.automationContext = automationContext;
    }

    @Override
    public void pauseMonitoring() {
    }

    @Override
    public void resumeMonitoring() {
    }

    @Override
    public void clearCookies() {
        throw new UnsupportedOperationException("Legacy shared context cookie clearing is handled by PlaywrightManager");
    }

    protected Page page(Long userId) {
        return automationContext.getPage(userId, this);
    }

    protected BrowserContext browserContext(Long userId) {
        return automationContext.getContext(userId, this);
    }

    protected void setLoggedIn(Long userId, boolean loggedIn) {
        automationContext.setLoginStatus(userId, platform(), loggedIn);
    }

    protected boolean isLoggedIn(Long userId) {
        return automationContext.isLoggedIn(userId, platform());
    }

    protected void saveCookies(Long userId, String remark) {
        automationContext.saveCookiesToDb(userId, this, remark);
    }

    protected void waitForLoginAsync(Long userId, Page page) {
        Thread waitThread = new Thread(() -> {
            try {
                int maxSeconds = 300;
                for (int i = 0; i < maxSeconds; i++) {
                    automationContext.withPlaywrightLock(() -> {
                        synchronized (automationContext.platformLock(userId, platform())) {
                            if (automationContext.updateLoginStatusFromPage(userId, this, page)) {
                                throw new LoginDetected();
                            }
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } catch (LoginDetected ignored) {
                return;
            } catch (Exception ignored) {
            }
        }, "wait-" + platform() + "-login-user-" + userId);

        waitThread.setDaemon(true);
        waitThread.start();
    }

    private static final class LoginDetected extends RuntimeException {
        private LoginDetected() {
        }
    }
}
