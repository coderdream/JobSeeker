package com.wh.jobsbackend.worker.manager.platform;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.worker.manager.PlaywrightAutomationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZhilianPlaywrightHandlerTest {

    @Test
    void loggedInCheckShouldAcceptAccountUrlWhenUserIdentityIsPlaceholder() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userIdentity = locator(1, true, "undefined");
        when(page.url()).thenReturn("https://i.zhaopin.com/account/center");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldAcceptVisibleConcreteUserIdentity() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userIdentity = locator(1, true, "Valid User");
        when(page.url()).thenReturn("https://i.zhaopin.com/account/center");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldAcceptVisibleAccountEntryWithoutUserText() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userIdentity = locator(0, false, "");
        Locator accountEntry = locator(1, true, "");
        when(page.url()).thenReturn("https://www.zhaopin.com/");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity, accountEntry);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldAcceptAccountEntryWhenUserIdentityIsPlaceholder() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userIdentity = locator(1, true, "undefined");
        Locator accountEntry = locator(1, true, "undefined");
        when(page.url()).thenReturn("https://www.zhaopin.com/");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity, accountEntry);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldPreferAuthenticatedEntryWhenLoginEntryStillVisible() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(1, true, "登录/注册");
        Locator userIdentity = locator(0, false, "");
        Locator accountEntry = locator(1, true, "个人中心");
        when(page.url()).thenReturn("https://www.zhaopin.com/");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity, accountEntry);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

        assertTrue(handler.checkLoggedIn(page));
    }

    @Test
    void loggedInCheckShouldAcceptServerRenderedIsLoggedFlag() {
        Page page = mock(Page.class);
        Locator defaultLocator = locator(0, false, "");
        Locator loginEntry = locator(0, false, "");
        Locator userIdentity = locator(0, false, "");
        when(page.url()).thenReturn("https://www.zhaopin.com/sou/");
        when(page.content()).thenReturn("<script>window.__INITIAL_STATE__={\"isLogged\":true}</script>");
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity);

        ZhilianPlaywrightHandler handler = new ZhilianPlaywrightHandler(mock(PlaywrightAutomationContext.class));

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

    private static void stubPageLocators(Page page, Locator defaultLocator, Locator loginEntry, Locator userIdentity) {
        stubPageLocators(page, defaultLocator, loginEntry, userIdentity, defaultLocator);
    }

    private static void stubPageLocators(Page page, Locator defaultLocator, Locator loginEntry, Locator userIdentity,
                                         Locator accountEntry) {
        when(page.locator(anyString())).thenAnswer(invocation -> {
            String selector = invocation.getArgument(0, String.class);
            if ("a.home-header__c-no-login".equals(selector)) {
                return loginEntry;
            }
            if (selector.contains(".user-info")) {
                return userIdentity;
            }
            if (selector.contains("i.zhaopin.com")) {
                return accountEntry;
            }
            return defaultLocator;
        });
    }
}
