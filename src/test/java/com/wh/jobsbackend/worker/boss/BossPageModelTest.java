package com.wh.jobsbackend.worker.boss;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPageModelTest {

    @Test
    void selectorsShouldCoverCardsChatAndBlockingDialogs() {
        assertTrue(BossPageModel.JOB_CARD_SELECTOR.contains("job-card-box"));
        assertTrue(BossPageModel.CHAT_BUTTON_SELECTOR.contains("btn-startchat"));
        assertTrue(BossPageModel.SEND_BUTTON_SELECTOR.contains("btn-send"));
        assertTrue(BossPageModel.BLOCKING_OVERLAY_SELECTOR.contains("dialog"));
    }

    @Test
    void confirmedDeliveryTextShouldRejectClickOnlyStates() {
        assertTrue(BossPageModel.isConfirmedDeliveryText("消息已发送"));
        assertTrue(BossPageModel.isConfirmedDeliveryText("立即沟通成功"));

        assertFalse(BossPageModel.isConfirmedDeliveryText("立即沟通"));
        assertFalse(BossPageModel.isConfirmedDeliveryText("发送"));
        assertFalse(BossPageModel.isConfirmedDeliveryText("已选中 3 个岗位"));
    }

    @Test
    void blockingTextShouldDetectLoginRiskAndDownloadPrompts() {
        assertTrue(BossPageModel.isBlockingText("请先登录后继续"));
        assertTrue(BossPageModel.isBlockingText("请完成滑块验证"));
        assertTrue(BossPageModel.isBlockingText("扫码下载APP"));

        assertFalse(BossPageModel.isBlockingText("消息已发送"));
    }

    @Test
    void bossLoginDetectionShouldAcceptPostQrRedirectUrls() {
        assertTrue(BossPageModel.isLoggedInUrl("https://www.zhipin.com/web/geek/jobs?ka=header-job"));
        assertTrue(BossPageModel.isLoggedInUrl("https://www.zhipin.com/web/geek/chat"));

        assertFalse(BossPageModel.isLoggedInUrl("https://www.zhipin.com/shenzhen/?seoRefer=index"));
        assertFalse(BossPageModel.isLoggedInUrl("https://www.zhipin.com/web/user/?ka=header-login"));
        assertFalse(BossPageModel.isLoggedInUrl("https://www.zhipin.com/web/user/safe/verify-slider"));
    }

    @Test
    void bossLoginEntryShouldBypassCitySeoRedirects() {
        assertTrue(BossPageModel.LOGIN_URL.contains("/web/user/"));
        assertFalse(BossPageModel.LOGIN_URL.contains("/shenzhen"));
    }

    @Test
    void bossLoginDetectionShouldUseLoginEntryAsNegativeSignal() {
        assertFalse(BossPageModel.isLoggedInEntryText("登录 / 注册"));
        assertFalse(BossPageModel.isLoggedInEntryText("APP扫码登录"));

        assertTrue(BossPageModel.isLoggedInEntryText("消息"));
        assertTrue(BossPageModel.isLoggedInEntryText(null));
    }

    @Test
    void bossLoginDetectionShouldAcceptSessionCookies() {
        assertTrue(BossPageModel.hasAuthenticatedCookieNames(List.of("lastCity", "wt2")));
        assertTrue(BossPageModel.hasAuthenticatedCookieNames(List.of("zp_token")));

        assertFalse(BossPageModel.hasAuthenticatedCookieNames(List.of("lastCity", "__zp_stoken__")));
        assertFalse(BossPageModel.hasAuthenticatedCookieNames(List.of()));
    }
}
