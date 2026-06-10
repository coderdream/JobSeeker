package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.worker.yupao.YupaoPageModel;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YupaoPlaywrightHandlerTest {

    @Test
    void loginPageCheckShouldAvoidDomInspection() {
        Page page = mock(Page.class);
        when(page.url()).thenReturn(YupaoPageModel.LOGIN_URL);

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertFalse(handler.checkLoggedIn(page));
        verify(page, never()).content();
        verify(page, never()).locator(anyString());
    }

    @Test
    void blankPageCheckShouldAvoidDomInspection() {
        Page page = mock(Page.class);
        when(page.url()).thenReturn("about:blank");

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertFalse(handler.checkLoggedIn(page));
        verify(page, never()).content();
        verify(page, never()).locator(anyString());
    }

    @Test
    void triggerLoginShouldOpenLoginUrlWithoutTouchingLoginDom() {
        PlaywrightAutomationContext automationContext = mock(PlaywrightAutomationContext.class);
        Page page = mock(Page.class);
        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(automationContext);
        when(automationContext.getPage(eq(42L), same(handler))).thenReturn(page);
        when(page.url()).thenReturn("about:blank");

        handler.triggerLogin(42L);

        verify(page).navigate(eq(YupaoPageModel.LOGIN_URL), any(Page.NavigateOptions.class));
        verify(page, never()).content();
        verify(page, never()).locator(anyString());
    }

    @Test
    void loggedInCheckShouldAcceptVisibleUserEntry() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userEntry = locator(1, true, "个人中心");
        when(page.url()).thenReturn("https://www.yupao.com/");
        stubPageLocators(page, defaultLocator, loginEntry, userEntry);

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldRejectVisibleLoginEntryWithoutUserEntry() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(1, true, "登录/注册");
        Locator userEntry = locator(0, false, "");
        when(page.url()).thenReturn("https://www.yupao.com/web/login/");
        stubPageLocators(page, defaultLocator, loginEntry, userEntry);

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertFalse(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldRejectUnknownPageWithoutReliableSignal() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userEntry = locator(0, false, "");
        when(page.url()).thenReturn("about:blank");
        when(page.content()).thenReturn("<html></html>");
        stubPageLocators(page, defaultLocator, loginEntry, userEntry);

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertFalse(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldAcceptServerRenderedLoginFlag() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userEntry = locator(0, false, "");
        when(page.url()).thenReturn("https://www.yupao.com/");
        when(page.content()).thenReturn("<script>window.__INITIAL_STATE__={\"isLoggedIn\":true}</script>");
        stubPageLocators(page, defaultLocator, loginEntry, userEntry);

        YupaoPlaywrightHandler handler = new YupaoPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    private static Locator locator(int count, boolean visible, String text) {
        Locator locator = mock(Locator.class);
        when(locator.first()).thenReturn(locator);
        when(locator.nth(anyInt())).thenReturn(locator);
        when(locator.count()).thenReturn(count);
        when(locator.isVisible()).thenReturn(visible);
        when(locator.textContent()).thenReturn(text);
        return locator;
    }

    private static void stubPageLocators(Page page, Locator defaultLocator, Locator loginEntry, Locator userEntry) {
        when(page.locator(anyString())).thenAnswer(invocation -> {
            String selector = invocation.getArgument(0, String.class);
            if (selector.contains("登录") || selector.contains("login")) {
                return loginEntry;
            }
            if (selector.contains("user") || selector.contains("个人中心")) {
                return userEntry;
            }
            return defaultLocator;
        });
    }
}
