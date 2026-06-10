package com.wh.jobsbackend.worker.manager;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.worker.manager.platform.PlatformPlaywrightHandler;
import com.wh.jobsbackend.worker.session.PlatformRuntime;
import com.wh.jobsbackend.worker.session.UserAutomationRegistry;
import com.wh.jobsbackend.worker.session.UserAutomationSession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PlaywrightManagerConcurrencyTest {

    @Test
    void cachedLoginStatusShouldNotInitializePlaywrightRuntime() {
        PlaywrightManager manager = new PlaywrightManager();

        boolean loggedIn = manager.getCachedLoginStatus(42L, "zhilian");

        assertFalse(loggedIn);
        assertNull(ReflectionTestUtils.getField(manager, "automationContext"));
        assertNull(ReflectionTestUtils.getField(manager, "platformRuntime"));
    }

    @Test
    void concurrentPageCreationShouldInitializeRuntimeOnlyOnce() throws Exception {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        AtomicInteger initCalls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        PlaywrightManager manager = new PlaywrightManager() {
            @Override
            public void init() {
                initCalls.incrementAndGet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ReflectionTestUtils.setField(this, "browser", browser);
            }
        };

        Future<Page> first = executor.submit(() -> {
            start.await();
            return manager.getPage(42L, "zhilian");
        });
        Future<Page> second = executor.submit(() -> {
            start.await();
            return manager.getPage(42L, "zhilian");
        });
        start.countDown();

        assertSame(page, first.get());
        assertSame(page, second.get());
        assertEquals(1, initCalls.get());
        executor.shutdownNow();
    }

    @Test
    void zhilianLoginNavigationShouldRunUnderPlatformLock() {
        Browser browser = mock(Browser.class);
        BrowserContext context = mock(BrowserContext.class);
        Page page = mock(Page.class);
        when(browser.newContext()).thenReturn(context);
        when(context.newPage()).thenReturn(page);

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser));
        PlatformRuntime runtime = new PlatformRuntime(registry);
        Object platformLock = runtime.getPlatformLock(42L, "zhilian");
        AtomicBoolean navigatedUnderLock = new AtomicBoolean(false);

        when(page.navigate(anyString(), any(Page.NavigateOptions.class))).thenAnswer(invocation -> {
            navigatedUnderLock.set(Thread.holdsLock(platformLock));
            throw new IllegalStateException("stop after lock check");
        });

        PlaywrightManager manager = new PlaywrightManager();
        ReflectionTestUtils.setField(manager, "platformRuntime", runtime);

        assertThrows(RuntimeException.class, () -> manager.triggerZhilianLogin(42L));
        assertTrue(navigatedUnderLock.get(), "Zhilian login navigation must hold the user/platform Playwright lock");
    }

    @Test
    void facadeShouldDelegateLoginToMatchingPlatformHandlerUnderLock() {
        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("zhilian");
        when(handler.domain()).thenReturn("zhaopin.com");
        when(handler.homeUrl()).thenReturn("https://www.zhaopin.com");
        AtomicBoolean triggeredUnderLock = new AtomicBoolean(false);

        Browser browser = mock(Browser.class);
        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser));
        PlatformRuntime runtime = new PlatformRuntime(registry);
        Object platformLock = runtime.getPlatformLock(42L, "zhilian");
        doAnswer(invocation -> {
            triggeredUnderLock.set(Thread.holdsLock(platformLock));
            return null;
        }).when(handler).triggerLogin(42L);

        PlaywrightManager manager = new PlaywrightManager(List.of(handler));
        ReflectionTestUtils.setField(manager, "platformRuntime", runtime);

        manager.triggerZhilianLogin(42L);

        verify(handler).triggerLogin(42L);
        assertTrue(triggeredUnderLock.get(), "Facade login delegation must hold the user/platform lock");
    }

    @Test
    void refreshLoginStatusShouldNotNavigateBlankPage() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        when(page.url()).thenReturn("about:blank");

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("boss");
        when(handler.domain()).thenReturn("zhipin.com");
        when(handler.homeUrl()).thenReturn("https://www.zhipin.com");
        when(handler.checkLoggedIn(page)).thenReturn(false);

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        PlatformRuntime runtime = new PlatformRuntime(registry, (session, platform) -> browserContext, (userId, platform, context) -> {
        });
        UserAutomationSession userSession = registry.getOrCreate(42L);
        userSession.getContexts().put("boss", browserContext);
        userSession.getPages().put("boss", page);
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (session, platform) -> browserContext,
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);

        automationContext.refreshLoginStatus(42L, handler);

        verify(page, never()).navigate(anyString(), any(Page.NavigateOptions.class));
    }

    @Test
    void refreshLoginStatusShouldNotCreateBlankPageWhenNoPageExists() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("yupao");
        when(handler.domain()).thenReturn("yupao.com");
        when(handler.homeUrl()).thenReturn("https://www.yupao.com");

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        PlatformRuntime runtime = new PlatformRuntime(registry, (session, platform) -> browserContext, (userId, platform, context) -> {
        });
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (session, platform) -> browserContext,
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);

        boolean loggedIn = automationContext.refreshLoginStatus(42L, handler);

        assertFalse(loggedIn);
        verify(browserContext, never()).newPage();
        verify(handler, never()).checkLoggedIn(any(Page.class));
    }

    @Test
    void refreshLoginStatusShouldNotReopenClosedLoginPage() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        Page closedPage = mock(Page.class);
        when(closedPage.isClosed()).thenReturn(true);
        when(browserContext.pages()).thenReturn(List.of());

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("yupao");
        when(handler.domain()).thenReturn("yupao.com");
        when(handler.homeUrl()).thenReturn("https://www.yupao.com");

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        UserAutomationSession userSession = registry.getOrCreate(42L);
        userSession.getContexts().put("yupao", browserContext);
        userSession.getPages().put("yupao", closedPage);
        PlatformRuntime runtime = new PlatformRuntime(registry, (existingSession, platform) -> browserContext, (userId, platform, context) -> {
        });
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (existingSession, platform) -> browserContext,
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);

        boolean loggedIn = automationContext.refreshLoginStatus(42L, handler);

        assertFalse(loggedIn);
        verify(browserContext, never()).newPage();
        verify(handler, never()).checkLoggedIn(closedPage);
    }

    @Test
    void refreshLoginStatusShouldRunPageChecksUnderGlobalPlaywrightLock() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        AtomicBoolean checkedUnderGlobalLock = new AtomicBoolean(false);

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("zhilian");
        when(handler.domain()).thenReturn("zhaopin.com");
        when(handler.homeUrl()).thenReturn("https://www.zhaopin.com");

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        PlatformRuntime runtime = new PlatformRuntime(registry, (session, platform) -> browserContext, (userId, platform, context) -> {
        });
        UserAutomationSession userSession = registry.getOrCreate(42L);
        userSession.getContexts().put("zhilian", browserContext);
        userSession.getPages().put("zhilian", page);
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (session, platform) -> browserContext,
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);
        Object playwrightLock = automationContext.playwrightLock();
        when(handler.checkLoggedIn(page)).thenAnswer(invocation -> {
            checkedUnderGlobalLock.set(Thread.holdsLock(playwrightLock));
            return false;
        });

        automationContext.refreshLoginStatus(42L, handler);

        assertTrue(checkedUnderGlobalLock.get(), "Login status page checks must hold the global Playwright lock");
    }

    @Test
    void refreshLoginStatusShouldInspectAdditionalPlatformPages() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        Page homePage = mock(Page.class);
        Page accountPage = mock(Page.class);
        when(browserContext.pages()).thenReturn(List.of(homePage, accountPage));

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("zhilian");
        when(handler.domain()).thenReturn("zhaopin.com");
        when(handler.homeUrl()).thenReturn("https://www.zhaopin.com");
        when(handler.checkLoggedIn(homePage)).thenReturn(false);
        when(handler.checkLoggedIn(accountPage)).thenReturn(true);

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        PlatformRuntime runtime = new PlatformRuntime(registry, (session, platform) -> browserContext, (userId, platform, context) -> {
        });
        UserAutomationSession userSession = registry.getOrCreate(42L);
        userSession.getContexts().put("zhilian", browserContext);
        userSession.getPages().put("zhilian", homePage);
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (session, platform) -> browserContext,
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);

        boolean loggedIn = automationContext.refreshLoginStatus(42L, handler);

        assertTrue(loggedIn);
        verify(handler).checkLoggedIn(homePage);
        verify(handler).checkLoggedIn(accountPage);
    }

    @Test
    void singlePageLoginCheckShouldNotDowngradeCachedLoginStatus() {
        Browser browser = mock(Browser.class);
        Page stalePage = mock(Page.class);

        PlatformPlaywrightHandler handler = mock(PlatformPlaywrightHandler.class);
        when(handler.platform()).thenReturn("zhilian");
        when(handler.domain()).thenReturn("zhaopin.com");
        when(handler.homeUrl()).thenReturn("https://www.zhaopin.com");
        when(handler.checkLoggedIn(stalePage)).thenReturn(false);

        UserAutomationRegistry registry = new UserAutomationRegistry(userId -> new UserAutomationSession(userId, browser, false));
        PlatformRuntime runtime = new PlatformRuntime(registry);
        runtime.setLoginStatus(42L, "zhilian", true);
        PlaywrightAutomationContext automationContext = new PlaywrightAutomationContext(
                mock(CookieService.class),
                () -> browser,
                () -> {
                },
                (session, platform) -> mock(BrowserContext.class),
                (userId, platform, context) -> {
                }
        );
        automationContext.setRuntime(runtime);

        boolean loggedIn = automationContext.updateLoginStatusFromPage(42L, handler, stalePage);

        assertTrue(loggedIn);
        assertTrue(runtime.isLoggedIn(42L, "zhilian"));
    }

    @Test
    void zhilianContextShouldNotSendBossRefererHeader() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);

        PlaywrightManager manager = new PlaywrightManager();
        UserAutomationSession session = new UserAutomationSession(42L, browser, false);

        BrowserContext createdContext = ReflectionTestUtils.invokeMethod(manager, "newUserContext", session, "zhilian");

        assertSame(browserContext, createdContext);
        org.mockito.ArgumentCaptor<Browser.NewContextOptions> optionsCaptor =
                org.mockito.ArgumentCaptor.forClass(Browser.NewContextOptions.class);
        verify(browser).newContext(optionsCaptor.capture());
        assertEquals("https://www.zhaopin.com/", optionsCaptor.getValue().extraHTTPHeaders.get("referer"));
    }

    @Test
    void job51ContextShouldNotForceWwwRefererHeader() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);

        PlaywrightManager manager = new PlaywrightManager();
        UserAutomationSession session = new UserAutomationSession(42L, browser, false);

        BrowserContext createdContext = ReflectionTestUtils.invokeMethod(manager, "newUserContext", session, "51job");

        assertSame(browserContext, createdContext);
        org.mockito.ArgumentCaptor<Browser.NewContextOptions> optionsCaptor =
                org.mockito.ArgumentCaptor.forClass(Browser.NewContextOptions.class);
        verify(browser).newContext(optionsCaptor.capture());
        assertFalse(optionsCaptor.getValue().extraHTTPHeaders.containsKey("referer"));
    }

    @Test
    void yupaoContextShouldUseYupaoRefererHeader() {
        Browser browser = mock(Browser.class);
        BrowserContext browserContext = mock(BrowserContext.class);
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        when(browser.version()).thenReturn("148.0.4758.102");

        PlaywrightManager manager = new PlaywrightManager();
        UserAutomationSession session = new UserAutomationSession(42L, browser, false);

        BrowserContext createdContext = ReflectionTestUtils.invokeMethod(manager, "newUserContext", session, "yupao");

        assertSame(browserContext, createdContext);
        org.mockito.ArgumentCaptor<Browser.NewContextOptions> optionsCaptor =
                org.mockito.ArgumentCaptor.forClass(Browser.NewContextOptions.class);
        verify(browser).newContext(optionsCaptor.capture());
        assertEquals("https://www.yupao.com/", optionsCaptor.getValue().extraHTTPHeaders.get("referer"));
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
                optionsCaptor.getValue().userAgent);
    }
}
