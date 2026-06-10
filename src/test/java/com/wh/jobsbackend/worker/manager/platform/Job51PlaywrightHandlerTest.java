package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Job51PlaywrightHandlerTest {

    @Test
    void personalMyJobUrlShouldCountAsLoggedInWhenSpaIsBlank() {
        Page page = mock(Page.class);
        Locator emptyLocator = emptyLocator();
        when(page.url()).thenReturn("https://we.51job.com/pc/my/myjob");
        when(page.locator(anyString())).thenReturn(emptyLocator);
        Job51PlaywrightHandler handler = new Job51PlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    private static Locator emptyLocator() {
        Locator locator = mock(Locator.class);
        when(locator.first()).thenReturn(locator);
        when(locator.isVisible()).thenReturn(false);
        when(locator.count()).thenReturn(0);
        when(locator.textContent()).thenReturn("");
        return locator;
    }
}
