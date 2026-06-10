package com.wh.jobsbackend.worker.job51;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Job51PageModelTest {

    @Test
    void selectorsShouldMatchCurrentSearchPageStructure() {
        assertTrue(Job51PageModel.JOB_CARD_SELECTOR.contains(".joblist-item"));
        assertTrue(Job51PageModel.PER_JOB_APPLY_BUTTON_SELECTOR.contains("button.btn.apply"));
        assertTrue(Job51PageModel.NEXT_PAGE_BUTTON_SELECTOR.contains("button.btn-next"));
    }

    @Test
    void selectorsShouldIncludeCurrentSuccessOverlayThatBlocksSubsequentClicks() {
        assertTrue(Job51PageModel.SUCCESS_DIALOG_BODY_SELECTOR.contains(".success-wrapper"));
        assertTrue(Job51PageModel.BLOCKING_OVERLAY_SELECTOR.contains(".van-overlay"));
        assertTrue(Job51PageModel.CLOSE_BUTTON_SELECTOR.contains(".van-icon-cross"));
    }

    @Test
    void parseSuccessfulDeliveryCountShouldUseConfirmedDialogTextOnly() {
        OptionalInt count = Job51PageModel.parseSuccessfulDeliveryCount("投递成功 3 个，未投递 1 个");

        assertTrue(count.isPresent());
        assertEquals(3, count.getAsInt());
    }

    @Test
    void parseSuccessfulDeliveryCountShouldReturnEmptyWhenDialogDoesNotConfirmSuccess() {
        assertTrue(Job51PageModel.parseSuccessfulDeliveryCount("全选 （已选20/20）").isEmpty());
        assertTrue(Job51PageModel.parseSuccessfulDeliveryCount("投递").isEmpty());
        assertTrue(Job51PageModel.parseSuccessfulDeliveryCount("简历已选择，是否确认投递").isEmpty());
    }
}
