package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Page;

public interface PlatformPlaywrightHandler {

    String platform();

    String homeUrl();

    String domain();

    void triggerLogin(Long userId);

    boolean checkLoggedIn(Page page);

    void setupMonitoring(Long userId, Page page);

    void pauseMonitoring();

    void resumeMonitoring();

    void clearCookies();
}
