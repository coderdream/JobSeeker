package com.wh.jobsbackend.worker.zhilian;

public final class ZhilianPageModel {
    public static final String JOB_LIST_CONTAINER_SELECTOR = "div.positionlist, div[class*='positionlist']";
    public static final String JOB_CARD_SELECTOR = "div.joblist-box__item";
    public static final String APPLY_BUTTON_SELECTOR = "button.collect-and-apply__btn";
    public static final String NEXT_PAGE_SELECTOR = "a.soupager__btn:has-text(\"下一页\")";
    public static final String DISABLED_NEXT_PAGE_CLASS = "soupager__btn--disable";
    public static final String SUCCESS_DIALOG_SELECTOR = "div.deliver-dialog, div.a-job-apply-workflow, .toast, .message, [class*='success']";
    public static final String BLOCKING_OVERLAY_SELECTOR = "div.a-job-apply-workflow, div.deliver-dialog, .modal, .popup, .toast, [class*='download'], [class*='login'], [class*='verify']";
    public static final String CLOSE_BUTTON_SELECTOR = "img[title='close-icon'], button[aria-label='Close'], [class*='close']";
    public static final String RESUME_SELECTION_SELECTOR = "div.a-attachment-select";
    public static final String RESUME_DELIVERY_BUTTON_SELECTOR = ".a-attachment-select__action-btn__delivery, a:has-text(\"投递简历\"), button:has-text(\"投递简历\")";

    private ZhilianPageModel() {
    }

    public static boolean isConfirmedDeliveryText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("申请成功")
                || text.contains("投递成功")
                || text.contains("已成功投递");
    }

    public static boolean isApplyButtonText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return (normalized.contains("立即投递") || "投递".equals(normalized))
                && !normalized.contains("已投递")
                && !normalized.contains("成功");
    }

    public static boolean isBlockingText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("达到上限")
                || normalized.contains("请登录后")
                || normalized.contains("未登录")
                || normalized.contains("需要登录")
                || text.contains("安全验证")
                || text.contains("访问验证")
                || normalized.contains("下载智联招聘APP")
                || normalized.contains("下载APP")
                || text.toLowerCase().contains("captcha")
                || text.toLowerCase().contains("geetest")
                || text.contains("风控");
    }

    public static boolean isResumeSelectionText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("请选择要投递的简历")
                && normalized.contains("投递简历");
    }
}
