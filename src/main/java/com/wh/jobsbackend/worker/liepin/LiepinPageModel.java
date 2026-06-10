package com.wh.jobsbackend.worker.liepin;

final class LiepinPageModel {
    static final String PAGINATION_BOX_SELECTOR = ".list-pagination-box";
    static final String NEXT_PAGE_SELECTOR = "li.ant-pagination-next";
    static final String SUBSCRIBE_CLOSE_BUTTON_SELECTOR = "div[class*='subscribe-close-btn']";
    static final String JOB_CARD_SELECTOR = "div[class*='job-card-pc-container']";
    static final String CHAT_BUTTON_SELECTOR = "button.ant-btn.ant-btn-primary.ant-btn-round, button[class*='ant-btn'][class*='primary'], button:has-text('聊一聊')";
    static final String CHAT_HEADER_SELECTOR = ".__im_basic__header-wrap";
    static final String CHAT_PANEL_SELECTOR = ".__im_basic__chat, .__im_basic__content, .__im_basic__main, [class*='im_basic'], [class*='im-basic']";
    static final String ATTACHMENT_RESUME_DIALOG_SELECTOR = ".ant-modal:has-text('选择附件简历'), [role='dialog']:has-text('选择附件简历'), body:has-text('选择附件简历')";
    static final String[] CHAT_CLOSE_BUTTON_SELECTORS = {
            ".__im_basic__header-wrap [class*='close']",
            ".__im_basic__header-wrap [aria-label*='关闭']",
            ".__im_basic__header-wrap button[aria-label*='关闭']",
            ".__im_basic__header-wrap [role='button']:has(svg)",
            ".__im_basic__header-wrap button:has(svg)",
            ".__im_basic__header-wrap svg",
            ".__im_basic__contacts-title [class*='close']",
            ".__im_basic__contacts-title [aria-label*='关闭']",
            ".__im_basic__contacts-title button[aria-label*='关闭']",
            ".__im_basic__contacts-title button:has(svg)",
            "div.__im_basic__contacts-title svg"
    };
    static final String CHAT_CLOSE_SELECTOR = String.join(", ", CHAT_CLOSE_BUTTON_SELECTORS);
    static final String[] SEND_RESUME_BUTTON_SELECTORS = {
            "button:has-text('发简历')",
            "button:has-text('发送简历')",
            "button:has-text('投递简历')",
            "[role='button']:has-text('发简历')",
            "[role='button']:has-text('发送简历')",
            "[role='button']:has-text('投递简历')",
            ".ant-btn:has-text('发简历')",
            ".ant-btn:has-text('发送简历')",
            ".ant-btn:has-text('投递简历')",
            "text=发简历",
            "text=发送简历",
            "text=投递简历"
    };
    static final String[] CHAT_READY_SELECTORS = SEND_RESUME_BUTTON_SELECTORS;
    static final String[] CONFIRM_RESUME_DELIVERY_BUTTON_SELECTORS = {
            "button:has-text('立即投递')",
            "button:has-text('确认投递')",
            "[role='button']:has-text('立即投递')",
            "[role='button']:has-text('确认投递')",
            ".ant-btn:has-text('立即投递')",
            ".ant-btn:has-text('确认投递')",
            "text=立即投递",
            "text=确认投递"
    };
    static final int CONFIRM_RESUME_DELIVERY_WAIT_SECONDS = 15;
    static final String SUCCESS_CONFIRM_SELECTOR = ".__im_basic__header-wrap, .ant-message, .ant-notification, [class*='success']";
    static final String BLOCKING_OVERLAY_SELECTOR = ".ant-modal, .ant-drawer, .ant-message, .ant-notification, [class*='passport'], [class*='verify'], [class*='download']";
    static final String CLOSE_BUTTON_SELECTOR = ".ant-modal-close, .ant-drawer-close, div[class*='subscribe-close-btn'], [class*='close']";

    private LiepinPageModel() {
    }

    static boolean isConfirmedDeliveryText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("简历投递成功")
                || normalized.contains("简历已投递")
                || normalized.contains("已投递简历")
                || normalized.contains("已发送简历")
                || normalized.contains("简历发送成功")
                || normalized.contains("发送简历成功")
                || normalized.contains("投递简历成功")
                || normalized.contains("投递成功");
    }

    static boolean isBlockingText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("登录后继续沟通")
                || normalized.contains("请先登录")
                || normalized.contains("扫码登录")
                || normalized.contains("账号登录")
                || normalized.contains("安全验证")
                || text.contains("访问验证")
                || normalized.contains("下载猎聘APP")
                || normalized.contains("打开猎聘APP")
                || normalized.contains("猎聘APP扫码")
                || normalized.contains("风控");
    }
}
