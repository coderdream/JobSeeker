package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.worker.boss.BossPageModel;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BossPlaywrightHandlerTest {

    @Test
    void loginPageCheckShouldAvoidDomInspection() {
        Page page = mock(Page.class);
        when(page.url()).thenReturn(BossPageModel.LOGIN_URL);

        BossPlaywrightHandler handler = new BossPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertFalse(handler.checkLoggedIn(page));
        verify(page, never()).locator(anyString());
        verify(page, never()).context();
    }

    @Test
    void triggerLoginShouldOpenLoginUrlWithoutTouchingLoginDom() {
        PlaywrightAutomationContext automationContext = mock(PlaywrightAutomationContext.class);
        Page page = mock(Page.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        BossPlaywrightHandler handler = new BossPlaywrightHandler(automationContext);
        when(automationContext.getPage(eq(42L), same(handler))).thenReturn(page);
        when(page.url()).thenReturn("about:blank");
        when(page.context()).thenReturn(browserContext);
        when(browserContext.cookies()).thenReturn(List.of());

        handler.triggerLogin(42L);

        verify(page).navigate(eq(BossPageModel.LOGIN_URL), any(Page.NavigateOptions.class));
        verify(page, never()).locator(anyString());
    }
}
