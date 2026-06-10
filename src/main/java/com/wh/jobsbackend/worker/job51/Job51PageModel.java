package com.wh.jobsbackend.worker.job51;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Job51PageModel {
    static final String JOB_CARD_SELECTOR = ".joblist-item";
    static final String CHECKBOX_SELECTOR = "div.ick";
    static final String PER_JOB_APPLY_BUTTON_SELECTOR = "button.btn.apply";
    static final String NEXT_PAGE_BUTTON_SELECTOR = "button.btn-next:not([disabled])";
    static final String DISABLED_NEXT_PAGE_BUTTON_SELECTOR = "button.btn-next[disabled], button.btn-next.is-disabled";
    static final String SUCCESS_DIALOG_BODY_SELECTOR = ".el-dialog__body, .el-message-box__message, .success-wrapper, .successContent";
    static final String BLOCKING_OVERLAY_SELECTOR = ".van-overlay, .van-popup, .success-wrapper, .successContent, .el-dialog__wrapper";
    static final String CLOSE_BUTTON_SELECTOR = "button.el-dialog__headerbtn, button[aria-label='Close'], .van-popup__close-icon, .van-icon-cross, [class*='close']";

    private static final Pattern SUCCESS_COUNT_PATTERN = Pattern.compile("投递成功\\D*(\\d+)");

    private Job51PageModel() {
    }

    static OptionalInt parseSuccessfulDeliveryCount(String text) {
        if (text == null || !text.contains("投递成功")) {
            return OptionalInt.empty();
        }
        Matcher matcher = SUCCESS_COUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(matcher.group(1)));
    }
}
