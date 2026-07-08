package com.wh.jobsbackend.worker.boss;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public final class BossPageModel {
    public static final String LOGIN_URL = "https://www.zhipin.com/web/user/?ka=header-login";
    static final String JOB_LIST_CONTAINER_SELECTOR = "ul.rec-job-list, div.job-list-container";
    static final String JOB_CARD_SELECTOR = "ul.rec-job-list li.job-card-box, li.job-card-box";
    static final String MORE_INFO_BUTTON_SELECTOR = "a.more-job-btn";
    static final String CHAT_BUTTON_SELECTOR = "a.btn-startchat, a.op-btn-chat";
    static final String CHAT_INPUT_SELECTOR = "div#chat-input.chat-input[contenteditable='true'], div[contenteditable='true'].chat-input, textarea.input-area";
    static final String SEND_BUTTON_SELECTOR = "div.send-message, button[type='send'].btn-send, button.btn-send:not(.btn-sendimg)";
    static final String SUCCESS_CONFIRM_SELECTOR = ".chat-message, .message-content, .toast, .tip, .dialog-con, .dialog-container";
    static final String BLOCKING_OVERLAY_SELECTOR = ".dialog-container, .dialog-wrap, .modal, .toast, .verify-dialog, .app-download, [class*='dialog'], [class*='modal']";
    static final String CLOSE_BUTTON_SELECTOR = "i.icon-close, button[aria-label='Close'], [class*='close']";

    private BossPageModel() {
    }

    static boolean isConfirmedDeliveryText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("消息已发送")
                || text.contains("发送成功")
                || text.contains("沟通成功")
                || text.contains("立即沟通成功")
                || text.contains("已发出")
                || text.contains("送达")
                || text.contains("已读")
                || text.contains("继续沟通");
    }

    static boolean isBlockingText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("登录")
                || text.contains("滑块")
                || text.contains("安全验证")
                || text.contains("访问验证")
                || text.contains("扫码")
                || text.toLowerCase().contains("app")
                || text.contains("风控");
    }
    public static boolean isLoggedInUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        if (isLoginUrl(normalized)) {
            return false;
        }
        return normalized.contains("zhipin.com/web/geek/");
    }

    public static boolean isLoginUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        return normalized.contains("/web/user/")
                || normalized.contains("/web/user?")
                || normalized.contains("verify-slider")
                || normalized.contains("login");
    }

    public static boolean isLoggedInEntryText(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String normalized = text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return !normalized.contains("登录")
                && !normalized.contains("注册")
                && !normalized.contains("扫码")
                && !normalized.contains("login");
    }

    public static boolean hasAuthenticatedCookieNames(Collection<String> cookieNames) {
        if (cookieNames == null || cookieNames.isEmpty()) {
            return false;
        }
        Set<String> authenticatedCookieNames = Set.of("wt2", "wbg", "zp_token", "zp_at");
        return cookieNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(authenticatedCookieNames::contains);
    }
}
