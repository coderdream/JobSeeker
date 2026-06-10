package com.wh.jobsbackend.worker.liepin;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LiepinTest {

    @Test
    void buildSearchUrlShouldEncodeKeyword() {
        Liepin liepin = new Liepin();
        LiepinConfig config = new LiepinConfig();
        config.setCityCode("010");
        config.setSalary("20$40");
        config.setKeywords(List.of("大模型 算法"));
        liepin.setConfig(config);

        String url = liepin.buildSearchUrl("大模型 算法");

        assertTrue(url.contains("city=010"));
        assertTrue(url.contains("salary=20%2440"));
        assertTrue(url.contains("key=%E5%A4%A7%E6%A8%A1%E5%9E%8B+%E7%AE%97%E6%B3%95"));
    }

    @Test
    void buildSearchUrlShouldIncludeConfiguredFilters() {
        Liepin liepin = new Liepin();
        LiepinConfig config = new LiepinConfig();
        config.setCityCode("010");
        config.setSalary("20$40");
        config.setCompTag("500");
        config.setPubTime("3");
        config.setWorkYearCode("2$5");
        config.setEduLevel("040");
        config.setIndustry("040");
        config.setJobKind("2");
        config.setCompScale("050");
        config.setCompStage("020");
        config.setCompKind("010");
        liepin.setConfig(config);

        String url = liepin.buildSearchUrl("Java");

        assertTrue(url.contains("city=010"));
        assertTrue(url.contains("dq=010"));
        assertTrue(url.contains("salary=20%2440"));
        assertTrue(url.contains("compTag=500"));
        assertTrue(url.contains("pubTime=3"));
        assertTrue(url.contains("workYearCode=2%245"));
        assertTrue(url.contains("eduLevel=040"));
        assertTrue(url.contains("industry=040"));
        assertTrue(url.contains("jobKind=2"));
        assertTrue(url.contains("compScale=050"));
        assertTrue(url.contains("compStage=020"));
        assertTrue(url.contains("compKind=010"));
    }

    @Test
    void buildSearchUrlShouldOmitUnlimitedFilters() {
        Liepin liepin = new Liepin();
        LiepinConfig config = new LiepinConfig();
        config.setCityCode("0");
        config.setSalary("不限");
        config.setCompTag("");
        config.setPubTime("0");
        config.setWorkYearCode("不限");
        config.setEduLevel("0");
        config.setIndustry("0");
        config.setJobKind("0");
        config.setCompScale("0");
        config.setCompStage("0");
        config.setCompKind("0");
        liepin.setConfig(config);

        String url = liepin.buildSearchUrl("Java");

        assertFalse(url.contains("city="));
        assertFalse(url.contains("dq="));
        assertFalse(url.contains("salary="));
        assertFalse(url.contains("compTag="));
        assertFalse(url.contains("pubTime="));
        assertFalse(url.contains("workYearCode="));
        assertFalse(url.contains("eduLevel="));
        assertFalse(url.contains("industry="));
        assertFalse(url.contains("jobKind="));
        assertFalse(url.contains("compScale="));
        assertFalse(url.contains("compStage="));
        assertFalse(url.contains("compKind="));
        assertTrue(url.contains("currentPage=0"));
        assertTrue(url.contains("key=Java"));
    }

    @Test
    void prepareShouldNotRegisterPersistentResponseListener() {
        Liepin liepin = new Liepin();
        Page page = mock(Page.class);
        liepin.setPage(page);

        liepin.prepare();

        verify(page, never()).onResponse(any());
    }
}
