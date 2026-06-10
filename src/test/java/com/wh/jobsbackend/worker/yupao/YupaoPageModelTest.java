package com.wh.jobsbackend.worker.yupao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YupaoPageModelTest {

    @Test
    void selectorsShouldCoverCardsApplyPaginationAndSuccessDialogs() {
        assertTrue(YupaoPageModel.JOB_CARD_SELECTOR.contains("job"));
        assertTrue(YupaoPageModel.APPLY_BUTTON_SELECTOR.contains("沟通"));
        assertTrue(YupaoPageModel.NEXT_PAGE_SELECTOR.contains("下一页"));
        assertTrue(YupaoPageModel.SUCCESS_TEXT_SELECTOR.contains("已沟通"));
    }

    @Test
    void loginDetectionShouldIdentifyLoginAndAccountUrls() {
        assertTrue(YupaoPageModel.isLoginUrl(YupaoPageModel.LOGIN_URL));
        assertTrue(YupaoPageModel.isLoggedInUrl("https://www.yupao.com/user-center/profile"));

        assertFalse(YupaoPageModel.isLoggedInUrl(YupaoPageModel.LOGIN_URL));
        assertFalse(YupaoPageModel.isLoggedInUrl("about:blank"));
    }

    @Test
    void loginDetectionShouldAcceptAuthCookies() {
        assertTrue(YupaoPageModel.hasAuthenticatedCookieNames(List.of("Hm_lvt", "yupao_token")));
        assertTrue(YupaoPageModel.hasAuthenticatedCookieNames(List.of("access_token")));

        assertFalse(YupaoPageModel.hasAuthenticatedCookieNames(List.of("Hm_lvt", "acw_tc")));
        assertFalse(YupaoPageModel.hasAuthenticatedCookieNames(List.of()));
    }

    @Test
    void deliveryActionTextShouldAcceptOnlyPrimaryApplyActions() {
        assertTrue(YupaoPageModel.isDeliveryActionText("立即沟通"));
        assertTrue(YupaoPageModel.isDeliveryActionText("沟通"));
        assertTrue(YupaoPageModel.isDeliveryActionText("申请职位"));
        assertTrue(YupaoPageModel.isDeliveryActionText("投递"));

        assertFalse(YupaoPageModel.isDeliveryActionText("已沟通"));
        assertFalse(YupaoPageModel.isDeliveryActionText("收藏"));
        assertFalse(YupaoPageModel.isDeliveryActionText("分享"));
    }

    @Test
    void deliveredTextShouldDetectAlreadyAppliedStates() {
        assertTrue(YupaoPageModel.isDeliveredText("已沟通"));
        assertTrue(YupaoPageModel.isDeliveredText("投递成功"));
        assertTrue(YupaoPageModel.isDeliveredText("已申请"));

        assertFalse(YupaoPageModel.isDeliveredText("立即沟通"));
        assertFalse(YupaoPageModel.isDeliveredText("申请职位"));
    }

    @Test
    void searchUrlShouldIncludeEncodedParameters() {
        YupaoConfig config = new YupaoConfig();
        config.setKeywords(List.of("Java", "后端"));
        config.setCityCode("beijing");
        config.setSalary("10-15K");
        config.setJobType("fulltime");

        String url = YupaoPageModel.buildSearchUrl(config, "Java");

        assertTrue(url.startsWith("https://www.yupao.com/"));
        assertTrue(url.contains("keyword=Java"));
        assertTrue(url.contains("city=beijing"));
        assertTrue(url.contains("salary=10-15K"));
        assertTrue(url.contains("jobType=fulltime"));
    }

    @Test
    void keywordsShouldDefaultToSingleBlankSearchWhenMissing() {
        YupaoConfig config = new YupaoConfig();

        assertEquals(List.of(""), YupaoPageModel.normalizedKeywords(config));
    }
}
