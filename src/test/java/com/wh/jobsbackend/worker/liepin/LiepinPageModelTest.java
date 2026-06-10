package com.wh.jobsbackend.worker.liepin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiepinPageModelTest {

    @Test
    void selectorsShouldCoverCardsPaginationChatAndOverlays() {
        assertTrue(LiepinPageModel.JOB_CARD_SELECTOR.contains("job-card"));
        assertTrue(LiepinPageModel.NEXT_PAGE_SELECTOR.contains("ant-pagination-next"));
        assertTrue(LiepinPageModel.CHAT_HEADER_SELECTOR.contains("__im_basic__header"));
        assertTrue(String.join(",", LiepinPageModel.SEND_RESUME_BUTTON_SELECTORS).contains("发简历"));
        assertTrue(String.join(",", LiepinPageModel.SEND_RESUME_BUTTON_SELECTORS).contains("发送简历"));
        assertTrue(String.join(",", LiepinPageModel.SEND_RESUME_BUTTON_SELECTORS).contains("投递简历"));
        assertFalse(String.join(",", LiepinPageModel.SEND_RESUME_BUTTON_SELECTORS).contains("立即投递"));
        assertTrue(String.join(",", LiepinPageModel.CONFIRM_RESUME_DELIVERY_BUTTON_SELECTORS).contains("立即投递"));
        assertTrue(LiepinPageModel.BLOCKING_OVERLAY_SELECTOR.contains("ant-modal"));
    }

    @Test
    void chatCloseSelectorShouldTargetTopRightCloseControl() {
        String selector = LiepinPageModel.CHAT_CLOSE_SELECTOR;

        assertTrue(selector.contains("__im_basic__contacts-title"));
        assertTrue(selector.contains("__im_basic__header-wrap"));
        assertTrue(selector.contains("button") || selector.contains("close") || selector.contains("aria-label"));
        assertTrue(selector.contains(","));
    }

    @Test
    void chatCloseSelectorsShouldPreferChatDetailHeaderBeforeContactList() {
        assertTrue(LiepinPageModel.CHAT_CLOSE_BUTTON_SELECTORS[0].contains("__im_basic__header-wrap"));
        assertFalse(LiepinPageModel.CHAT_CLOSE_BUTTON_SELECTORS[0].contains("__im_basic__contacts-title"));
        assertTrue(LiepinPageModel.CHAT_CLOSE_BUTTON_SELECTORS[LiepinPageModel.CHAT_CLOSE_BUTTON_SELECTORS.length - 1]
                .contains("__im_basic__contacts-title"));
    }

    @Test
    void chatReadySelectorsShouldWaitForSendResumeActionNotHeader() {
        String selector = String.join(",", LiepinPageModel.CHAT_READY_SELECTORS);

        assertTrue(selector.contains("发简历"));
        assertTrue(selector.contains("发送简历"));
        assertFalse(selector.contains("__im_basic__header-wrap"));
    }

    @Test
    void confirmationSelectorsShouldCoverAttachmentResumeDialog() {
        String selector = String.join(",", LiepinPageModel.CONFIRM_RESUME_DELIVERY_BUTTON_SELECTORS);

        assertTrue(LiepinPageModel.ATTACHMENT_RESUME_DIALOG_SELECTOR.contains("选择附件简历"));
        assertTrue(selector.contains("立即投递"));
        assertTrue(selector.contains("确认投递"));
        assertFalse(selector.contains("发简历"));
        assertTrue(LiepinPageModel.CONFIRM_RESUME_DELIVERY_WAIT_SECONDS >= 10);
    }

    @Test
    void confirmedDeliveryTextShouldOnlyAcceptPlatformConfirmation() {
        assertTrue(LiepinPageModel.isConfirmedDeliveryText("简历投递成功"));
        assertTrue(LiepinPageModel.isConfirmedDeliveryText("您的简历已投递"));
        assertTrue(LiepinPageModel.isConfirmedDeliveryText("已发送简历"));
        assertTrue(LiepinPageModel.isConfirmedDeliveryText("发送简历成功"));

        assertFalse(LiepinPageModel.isConfirmedDeliveryText("聊一聊已发送"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("沟通成功"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("发送成功"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("已发送"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("发简历"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("聊一聊"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("继续聊"));
        assertFalse(LiepinPageModel.isConfirmedDeliveryText("已选中 10 个"));
    }

    @Test
    void blockingTextShouldDetectKnownInterruptions() {
        assertTrue(LiepinPageModel.isBlockingText("登录后继续沟通"));
        assertTrue(LiepinPageModel.isBlockingText("请先登录后再继续沟通"));
        assertTrue(LiepinPageModel.isBlockingText("扫码登录"));
        assertTrue(LiepinPageModel.isBlockingText("安全验证"));
        assertTrue(LiepinPageModel.isBlockingText("访问验证"));
        assertTrue(LiepinPageModel.isBlockingText("下载猎聘APP"));

        assertFalse(LiepinPageModel.isBlockingText("岗位加载完成"));
        assertFalse(LiepinPageModel.isBlockingText("首页 职位 校园 海归 简历优化 猎聘APP 我要招人 你好， 王先生 搜索"));
    }
}
