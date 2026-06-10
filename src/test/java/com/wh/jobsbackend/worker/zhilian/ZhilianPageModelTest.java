package com.wh.jobsbackend.worker.zhilian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhilianPageModelTest {

    @Test
    void selectorsShouldCoverCardsApplyPaginationAndSuccessDialogs() {
        assertTrue(ZhilianPageModel.JOB_CARD_SELECTOR.contains("joblist-box__item"));
        assertTrue(ZhilianPageModel.APPLY_BUTTON_SELECTOR.contains("collect-and-apply"));
        assertTrue(ZhilianPageModel.NEXT_PAGE_SELECTOR.contains("下一页"));
        assertTrue(ZhilianPageModel.SUCCESS_DIALOG_SELECTOR.contains("deliver-dialog"));
    }

    @Test
    void applyButtonTextShouldAcceptOnlyDeliveryActions() {
        assertTrue(ZhilianPageModel.isApplyButtonText("立即投递"));
        assertTrue(ZhilianPageModel.isApplyButtonText("投递"));

        assertFalse(ZhilianPageModel.isApplyButtonText("已投递"));
        assertFalse(ZhilianPageModel.isApplyButtonText("收藏"));
        assertFalse(ZhilianPageModel.isApplyButtonText("申请成功"));
    }

    @Test
    void confirmedDeliveryTextShouldRejectSelectionAndClickStates() {
        assertTrue(ZhilianPageModel.isConfirmedDeliveryText("申请成功"));
        assertTrue(ZhilianPageModel.isConfirmedDeliveryText("投递成功"));

        assertFalse(ZhilianPageModel.isConfirmedDeliveryText("立即投递"));
        assertFalse(ZhilianPageModel.isConfirmedDeliveryText("已选中 5 个"));
        assertFalse(ZhilianPageModel.isConfirmedDeliveryText("加入列表"));
    }

    @Test
    void blockingTextShouldDetectLimitLoginAndDownloadPrompts() {
        assertTrue(ZhilianPageModel.isBlockingText("今日投递已达到上限"));
        assertTrue(ZhilianPageModel.isBlockingText("请登录后继续"));
        assertTrue(ZhilianPageModel.isBlockingText("下载智联招聘APP"));

        assertFalse(ZhilianPageModel.isBlockingText("申请成功"));
    }

    @Test
    void blockingTextShouldIgnoreOrdinarySearchPageNavigationText() {
        assertFalse(ZhilianPageModel.isBlockingText(
                "home jobs city enterprise campus messages login app download category industry salary filters"
        ));
    }

    @Test
    void resumeSelectionTextShouldDetectIntermediateResumeDialog() {
        assertTrue(ZhilianPageModel.isResumeSelectionText("请选择要投递的简历 在线简历 投递简历"));
        assertFalse(ZhilianPageModel.isResumeSelectionText("申请成功"));
    }
}
