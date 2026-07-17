package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.Job51Service;
import com.wh.jobsbackend.application.service.LiepinService;
import com.wh.jobsbackend.application.service.ZhilianService;
import com.wh.jobsbackend.application.stream.ProgressStreamService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.BossJobService;
import com.wh.jobsbackend.worker.service.Job51JobService;
import com.wh.jobsbackend.worker.service.LiepinJobService;
import com.wh.jobsbackend.worker.service.ZhilianJobService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserScopedSseControllerTest {

    @Test
    void bossStreamShouldOpenForCurrentUser() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("boss-progress"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new BossController(
                mock(BossJobService.class),
                mock(com.wh.jobsbackend.application.service.BossService.class),
                mock(PlaywrightManager.class),
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/boss/stream"));

        verify(progressStreamService).open(eq(42L), eq("boss-progress"), any());
    }

    @Test
    void bossStartShouldPassCurrentUserToJobService() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        BossJobService bossJobService = mock(BossJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.isLoggedIn(42L, "boss")).thenReturn(true);
        when(bossJobService.startDeliveryAsync(eq(42L), any(Executor.class), any())).thenReturn(true);

        MockMvc mockMvc = standaloneSetup(new BossController(
                bossJobService,
                mock(com.wh.jobsbackend.application.service.BossService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/boss/start")).andExpect(status().isOk());

        verify(bossJobService).startDeliveryAsync(eq(42L), any(Executor.class), any());
    }

    @Test
    void bossStatusShouldRefreshLoginStatusForCurrentUser() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        BossJobService bossJobService = mock(BossJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.refreshLoginStatus(42L, "boss")).thenReturn(true);
        when(bossJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "boss",
                "isRunning", false,
                "isLoggedIn", false
        )));

        MockMvc mockMvc = standaloneSetup(new BossController(
                bossJobService,
                mock(com.wh.jobsbackend.application.service.BossService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/boss/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true));

        verify(playwrightManager).refreshLoginStatus(42L, "boss");
    }

    @Test
    void bossLoginTriggerShouldPassCurrentUserToPlaywrightManager() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);

        MockMvc mockMvc = standaloneSetup(new BossController(
                mock(BossJobService.class),
                mock(com.wh.jobsbackend.application.service.BossService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                mock(ProgressStreamService.class),
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/boss/login")).andExpect(status().isOk());

        verify(playwrightManager).triggerBossLogin(42L);
    }

    @Test
    void loginStatusStreamShouldOpenForCurrentUser() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("login-status"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new JobController(
                mock(Job51Service.class),
                mock(Job51JobService.class),
                mock(PlaywrightManager.class),
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/jobs/login-status/stream"));

        verify(progressStreamService).open(eq(42L), eq("login-status"), any());
    }

    @Test
    void loginStatusStreamShouldReadCachedStatusesOnly() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("login-status"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new JobController(
                mock(Job51Service.class),
                mock(Job51JobService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/jobs/login-status/stream"));

        verify(playwrightManager).getCachedLoginStatus(42L, "boss");
        verify(playwrightManager).getCachedLoginStatus(42L, "liepin");
        verify(playwrightManager).getCachedLoginStatus(42L, "51job");
        verify(playwrightManager).getCachedLoginStatus(42L, "zhilian");
        verify(playwrightManager, never()).isLoggedIn(any(), any());
    }

    @Test
    void job51LoginTriggerShouldPassCurrentUserToPlaywrightManager() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);

        MockMvc mockMvc = standaloneSetup(new JobController(
                mock(Job51Service.class),
                mock(Job51JobService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                mock(ProgressStreamService.class),
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/51job/login")).andExpect(status().isOk());

        verify(playwrightManager).trigger51jobLogin(42L);
    }

    @Test
    void zhilianLoginTriggerShouldPassCurrentUserToPlaywrightManager() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                mock(ZhilianJobService.class),
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/zhilian/login")).andExpect(status().isOk());

        verify(playwrightManager).triggerZhilianLogin(42L);
    }

    @Test
    void zhilianLoginStatusShouldRefreshLoginStatusForCurrentUser() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.refreshLoginStatus(42L, "zhilian")).thenReturn(true);

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                mock(ZhilianJobService.class),
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/zhilian/login-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true));

        verify(playwrightManager).refreshLoginStatus(42L, "zhilian");
    }

    @Test
    void zhilianStatusShouldNotRefreshLoginStatusWhileTaskIsRunning() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        ZhilianJobService zhilianJobService = mock(ZhilianJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(zhilianJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "zhilian",
                "isRunning", true,
                "isLoggedIn", true
        )));

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                zhilianJobService,
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/zhilian/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true))
                .andExpect(jsonPath("$.isRunning").value(true));

        verify(playwrightManager, never()).refreshLoginStatus(42L, "zhilian");
    }

    @Test
    void zhilianStatusShouldKeepCachedLoginTrueWithoutRefreshingPage() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        ZhilianJobService zhilianJobService = mock(ZhilianJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.getCachedLoginStatus(42L, "zhilian")).thenReturn(true);
        when(zhilianJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "zhilian",
                "isRunning", false,
                "isLoggedIn", true
        )));

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                zhilianJobService,
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/zhilian/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true))
                .andExpect(jsonPath("$.isRunning").value(false));

        verify(playwrightManager).getCachedLoginStatus(42L, "zhilian");
        verify(playwrightManager, never()).refreshLoginStatus(42L, "zhilian");
    }

    @Test
    void zhilianStatusShouldUseCachedLoginFalseWithoutRefreshingPageByDefault() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        ZhilianJobService zhilianJobService = mock(ZhilianJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.getCachedLoginStatus(42L, "zhilian")).thenReturn(false);
        when(zhilianJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "zhilian",
                "isRunning", false,
                "isLoggedIn", false
        )));

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                zhilianJobService,
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/zhilian/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(false))
                .andExpect(jsonPath("$.isRunning").value(false));

        verify(playwrightManager).getCachedLoginStatus(42L, "zhilian");
        verify(playwrightManager, never()).refreshLoginStatus(42L, "zhilian");
    }

    @Test
    void zhilianStatusShouldRefreshLoginStatusWhenRequested() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        ZhilianJobService zhilianJobService = mock(ZhilianJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.getCachedLoginStatus(42L, "zhilian")).thenReturn(false);
        when(playwrightManager.refreshLoginStatus(42L, "zhilian")).thenReturn(true);
        when(zhilianJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "zhilian",
                "isRunning", false,
                "isLoggedIn", false
        )));

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                playwrightManager,
                mock(CookieService.class),
                zhilianJobService,
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/zhilian/status").param("refreshLogin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true))
                .andExpect(jsonPath("$.isRunning").value(false));

        verify(playwrightManager).getCachedLoginStatus(42L, "zhilian");
        verify(playwrightManager).refreshLoginStatus(42L, "zhilian");
    }

    @Test
    void zhilianApplyButtonCheckShouldInspectButtonsForCurrentUserWithoutStartingDelivery() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ZhilianJobService zhilianJobService = mock(ZhilianJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(zhilianJobService.inspectApplyButtons(42L)).thenReturn(new ZhilianJobService.ApplyButtonInspection(
                "java",
                3,
                3,
                2,
                "立即投递",
                "找到 2 个可见投递按钮"
        ));

        MockMvc mockMvc = standaloneSetup(new ZhilianController(
                mock(ZhilianService.class),
                mock(PlaywrightManager.class),
                mock(CookieService.class),
                zhilianJobService,
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/zhilian/apply-button-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("java"))
                .andExpect(jsonPath("$.jobCardCount").value(3))
                .andExpect(jsonPath("$.applyButtonCount").value(3))
                .andExpect(jsonPath("$.visibleApplyButtonCount").value(2))
                .andExpect(jsonPath("$.firstButtonText").value("立即投递"));

        verify(zhilianJobService).inspectApplyButtons(42L);
        verify(zhilianJobService, never()).startDeliveryAsync(eq(42L), any(Executor.class), any());
    }

    @Test
    void liepinLoginTriggerShouldPassCurrentUserToPlaywrightManager() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);

        MockMvc mockMvc = standaloneSetup(new LiepinController(
                mock(LiepinJobService.class),
                playwrightManager,
                mock(CookieService.class),
                mock(LiepinService.class),
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(post("/api/liepin/login")).andExpect(status().isOk());

        verify(playwrightManager).triggerLiepinLogin(42L);
    }

    @Test
    void liepinLoginStatusShouldRefreshLoginStatusForCurrentUser() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(playwrightManager.refreshLoginStatus(42L, "liepin")).thenReturn(true);

        MockMvc mockMvc = standaloneSetup(new LiepinController(
                mock(LiepinJobService.class),
                playwrightManager,
                mock(CookieService.class),
                mock(LiepinService.class),
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/liepin/login-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true));

        verify(playwrightManager).refreshLoginStatus(42L, "liepin");
    }

    @Test
    void liepinStatusShouldNotRefreshLoginStatusWhileTaskIsRunning() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        LiepinJobService liepinJobService = mock(LiepinJobService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(liepinJobService.getStatus(42L)).thenReturn(new java.util.HashMap<>(java.util.Map.of(
                "platform", "liepin",
                "isRunning", true,
                "isLoggedIn", true
        )));

        MockMvc mockMvc = standaloneSetup(new LiepinController(
                liepinJobService,
                playwrightManager,
                mock(CookieService.class),
                mock(LiepinService.class),
                currentUserService,
                Runnable::run
        )).build();

        mockMvc.perform(get("/api/liepin/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true))
                .andExpect(jsonPath("$.isRunning").value(true));

        verify(playwrightManager, never()).refreshLoginStatus(42L, "liepin");
    }
}
