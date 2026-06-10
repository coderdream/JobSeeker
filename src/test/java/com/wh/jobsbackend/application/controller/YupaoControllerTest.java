package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.YupaoService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.YupaoJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class YupaoControllerTest {
    private MockMvc mockMvc;
    private YupaoJobService yupaoJobService;
    private PlaywrightManager playwrightManager;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        yupaoJobService = mock(YupaoJobService.class);
        playwrightManager = mock(PlaywrightManager.class);
        CookieService cookieService = mock(CookieService.class);
        YupaoService yupaoService = mock(YupaoService.class);
        currentUserService = mock(CurrentUserService.class);
        Executor executor = Runnable::run;
        mockMvc = MockMvcBuilders.standaloneSetup(new YupaoController(
                yupaoJobService,
                playwrightManager,
                cookieService,
                yupaoService,
                currentUserService,
                executor
        )).build();
    }

    @Test
    void statusShouldUseCachedLoginStatusByDefault() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(42L);
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", false);
        when(yupaoJobService.getStatus(42L)).thenReturn(status);
        when(playwrightManager.getCachedLoginStatus(42L, "yupao")).thenReturn(false);

        mockMvc.perform(get("/api/yupao/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(false));

        verify(playwrightManager, never()).refreshLoginStatus(42L, "yupao");
    }

    @Test
    void statusShouldRefreshLoginStatusWhenRequested() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(42L);
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", false);
        when(yupaoJobService.getStatus(42L)).thenReturn(status);
        when(playwrightManager.refreshLoginStatus(42L, "yupao")).thenReturn(true);

        mockMvc.perform(get("/api/yupao/status").param("refreshLogin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoggedIn").value(true));

        verify(playwrightManager).refreshLoginStatus(42L, "yupao");
    }
}
