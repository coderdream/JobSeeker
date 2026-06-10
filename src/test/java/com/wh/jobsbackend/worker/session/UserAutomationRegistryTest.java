package com.wh.jobsbackend.worker.session;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAutomationRegistryTest {

    @Test
    void samePlatformShouldUseSeparateSessionsPerUser() {
        Browser browserA = mock(Browser.class);
        Browser browserB = mock(Browser.class);
        BrowserContext contextA = mock(BrowserContext.class);
        BrowserContext contextB = mock(BrowserContext.class);
        Page pageA = mock(Page.class);
        Page pageB = mock(Page.class);
        when(browserA.newContext()).thenReturn(contextA);
        when(browserB.newContext()).thenReturn(contextB);
        when(contextA.newPage()).thenReturn(pageA);
        when(contextB.newPage()).thenReturn(pageB);
        UserAutomationRegistry registry = new UserAutomationRegistry(userId ->
                new UserAutomationSession(userId, userId == 1L ? browserA : browserB));
        PlatformRuntime runtime = new PlatformRuntime(registry);

        Page firstUserPage = runtime.getOrCreatePage(1L, "boss");
        Page secondUserPage = runtime.getOrCreatePage(2L, "boss");

        assertSame(pageA, firstUserPage);
        assertSame(pageB, secondUserPage);
        assertNotSame(firstUserPage, secondUserPage);
    }

    @Test
    void loginStatusShouldBeScopedByUserAndPlatform() {
        UserAutomationRegistry registry = new UserAutomationRegistry(userId ->
                new UserAutomationSession(userId, mock(Browser.class)));
        PlatformRuntime runtime = new PlatformRuntime(registry);

        runtime.setLoginStatus(1L, "boss", true);

        assertTrue(runtime.isLoggedIn(1L, "boss"));
        assertFalse(runtime.isLoggedIn(2L, "boss"));
        assertFalse(runtime.isLoggedIn(1L, "liepin"));
    }

    @Test
    void getOrCreatePageShouldReplaceClosedCachedPage() {
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        Page closedPage = mock(Page.class);
        Page freshPage = mock(Page.class);
        when(browser.newContext()).thenReturn(context);
        when(context.newPage()).thenReturn(closedPage, freshPage);
        when(closedPage.isClosed()).thenReturn(true);
        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser));
        PlatformRuntime runtime = new PlatformRuntime(registry);

        Page first = runtime.getOrCreatePage(1L, "zhilian");
        Page second = runtime.getOrCreatePage(1L, "zhilian");

        assertSame(closedPage, first);
        assertSame(freshPage, second);
    }

    @Test
    void getOrCreatePageShouldRecoverFromClosedCachedContext() {
        Browser browser = mock(Browser.class);
        BrowserContext closedContext = mock(BrowserContext.class);
        BrowserContext freshContext = mock(BrowserContext.class);
        Page freshPage = mock(Page.class);
        AtomicInteger contextCreations = new AtomicInteger();
        when(closedContext.newPage()).thenThrow(new RuntimeException("Target page, context or browser has been closed"));
        when(freshContext.newPage()).thenReturn(freshPage);
        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser));
        PlatformRuntime runtime = new PlatformRuntime(
                registry,
                (session, platform) -> contextCreations.incrementAndGet() == 1 ? closedContext : freshContext,
                (userId, platform, context) -> {
                }
        );

        Page page = runtime.getOrCreatePage(1L, "zhilian");

        assertSame(freshPage, page);
        assertSame(2, contextCreations.get());
    }

    @Test
    void platformLockShouldBeScopedByUserAndPlatform() {
        UserAutomationRegistry registry = new UserAutomationRegistry(userId ->
                new UserAutomationSession(userId, mock(Browser.class)));
        PlatformRuntime runtime = new PlatformRuntime(registry);

        Object first = runtime.getPlatformLock(1L, "liepin");
        Object same = runtime.getPlatformLock(1L, "liepin");
        Object otherPlatform = runtime.getPlatformLock(1L, "boss");
        Object otherUser = runtime.getPlatformLock(2L, "liepin");

        assertSame(first, same);
        assertNotSame(first, otherPlatform);
        assertNotSame(first, otherUser);
    }

    @Test
    void closeUserShouldRemovePlatformLocks() {
        UserAutomationRegistry registry = new UserAutomationRegistry(userId ->
                new UserAutomationSession(userId, mock(Browser.class)));
        PlatformRuntime runtime = new PlatformRuntime(registry);

        Object beforeClose = runtime.getPlatformLock(1L, "liepin");

        runtime.closeUser(1L);

        assertNotSame(beforeClose, runtime.getPlatformLock(1L, "liepin"));
    }

    @Test
    void closeUserShouldNotCloseOtherUserSessions() {
        Browser browserA = mock(Browser.class);
        Browser browserB = mock(Browser.class);
        UserAutomationRegistry registry = new UserAutomationRegistry(userId ->
                new UserAutomationSession(userId, userId == 1L ? browserA : browserB));
        registry.getOrCreate(1L);
        registry.getOrCreate(2L);

        registry.closeUser(1L);

        verify(browserA).close();
        assertSame(browserB, registry.getOrCreate(2L).getBrowser());
    }
}
