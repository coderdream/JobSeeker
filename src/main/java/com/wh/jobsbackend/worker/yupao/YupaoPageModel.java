package com.wh.jobsbackend.worker.yupao;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class YupaoPageModel {
    public static final String HOME_URL = "https://www.yupao.com/";
    public static final String LOGIN_URL = "https://www.yupao.com/web/login/";
    public static final String SEARCH_URL = "https://www.yupao.com/zhaogong/";
    public static final String JOB_LIST_CONTAINER_SELECTOR = "main, body, [class*='list'], [class*='job']";
    public static final String JOB_CARD_SELECTOR = "[class*='job'], [class*='position'], [class*='item'], a[href*='yupao.com']";
    public static final String APPLY_BUTTON_SELECTOR = "button:has-text(\"沟通\"), a:has-text(\"沟通\"), button:has-text(\"申请职位\"), a:has-text(\"申请职位\"), button:has-text(\"投递\"), a:has-text(\"投递\")";
    public static final String NEXT_PAGE_SELECTOR = "button:has-text(\"下一页\"), a:has-text(\"下一页\"), .next";
    public static final String SUCCESS_TEXT_SELECTOR = "text=/已沟通|投递成功|已申请|沟通成功/";
    public static final String BLOCKING_TEXT_SELECTOR = "text=/登录|验证码|安全验证|下载APP|风控|上限/";

    private YupaoPageModel() {
    }

    public static boolean isLoginUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        return normalized.contains("/web/login")
                || normalized.contains("/login/")
                || normalized.contains("/login?");
    }

    public static boolean isLoggedInUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        if (isLoginUrl(normalized)) {
            return false;
        }
        return normalized.contains("/web/user")
                || normalized.contains("/user-center")
                || normalized.contains("/usercenter")
                || normalized.contains("/personal")
                || normalized.contains("/member")
                || normalized.contains("/resume");
    }

    public static boolean hasAuthenticatedCookieNames(Collection<String> cookieNames) {
        if (cookieNames == null || cookieNames.isEmpty()) {
            return false;
        }
        Set<String> exactAuthCookieNames = Set.of(
                "access_token",
                "auth_token",
                "authorization",
                "passport",
                "token",
                "uid",
                "user_id",
                "user_token",
                "userid",
                "yupao_token",
                "yp_token"
        );
        return cookieNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> exactAuthCookieNames.contains(name)
                        || name.contains("auth")
                        || name.contains("passport")
                        || name.contains("token"));
    }

    public static List<String> normalizedKeywords(YupaoConfig config) {
        if (config == null || config.getKeywords() == null || config.getKeywords().isEmpty()) {
            return List.of("");
        }
        List<String> values = config.getKeywords().stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .toList();
        return values.isEmpty() ? List.of("") : values;
    }

    public static String buildSearchUrl(YupaoConfig config, String keyword) {
        StringBuilder url = new StringBuilder(SEARCH_URL);
        boolean first = true;
        first = append(url, first, "keyword", keyword);
        if (config != null) {
            first = append(url, first, "city", config.getCityCode());
            first = append(url, first, "salary", config.getSalary());
            append(url, first, "jobType", config.getJobType());
        }
        return url.toString();
    }

    public static boolean isDeliveryActionText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return (normalized.contains("立即沟通")
                || "沟通".equals(normalized)
                || normalized.contains("申请职位")
                || "投递".equals(normalized))
                && !isDeliveredText(normalized)
                && !normalized.contains("收藏")
                && !normalized.contains("分享");
    }

    public static boolean isDeliveredText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.contains("已沟通")
                || normalized.contains("投递成功")
                || normalized.contains("已申请")
                || normalized.contains("沟通成功");
    }

    public static boolean isBlockingText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        String lower = normalized.toLowerCase();
        return normalized.contains("请登录")
                || normalized.contains("未登录")
                || normalized.contains("验证码")
                || normalized.contains("安全验证")
                || normalized.contains("下载APP")
                || normalized.contains("风控")
                || normalized.contains("上限")
                || lower.contains("captcha");
    }

    private static boolean append(StringBuilder url, boolean first, String key, String value) {
        if (value == null || value.isBlank()) {
            return first;
        }
        url.append(first ? '?' : '&')
                .append(key)
                .append('=')
                .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
        return false;
    }
}
