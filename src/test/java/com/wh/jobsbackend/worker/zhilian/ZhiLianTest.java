package com.wh.jobsbackend.worker.zhilian;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.application.service.ZhilianService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZhiLianTest {

    @Test
    void deliverByKeywordShouldNotWaitForFullPageLoadWhenOpeningSearchPage() {
        Page page = mock(Page.class);
        Locator missingInput = mock(Locator.class);
        when(missingInput.count()).thenReturn(0);
        when(missingInput.first()).thenReturn(missingInput);
        when(page.locator(anyString())).thenReturn(missingInput);

        ZhiLian zhiLian = new ZhiLian(mock(ZhilianService.class));
        zhiLian.setPage(page);

        ReflectionTestUtils.invokeMethod(zhiLian, "deliverByKeyword", "Java", "https://www.zhaopin.com/sou/jl538/p1?sl=10000,15000");

        ArgumentCaptor<Page.NavigateOptions> options = ArgumentCaptor.forClass(Page.NavigateOptions.class);
        verify(page).navigate(eq("https://www.zhaopin.com/sou/jl538/p1?sl=10000,15000"), options.capture());
        assertEquals(WaitUntilState.DOMCONTENTLOADED, options.getValue().waitUntil);
    }
}
