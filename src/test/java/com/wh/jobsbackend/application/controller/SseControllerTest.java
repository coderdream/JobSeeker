package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.Job51Service;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.stream.ProgressStreamService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.BossJobService;
import com.wh.jobsbackend.worker.service.Job51JobService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;

class SseControllerTest {

    private final ProgressStreamService progressStreamService = mock(ProgressStreamService.class);
    private final PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final Executor sameThreadExecutor = Runnable::run;

    @Test
    void bossProgressStreamShouldKeepEventStreamContentType() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("boss-progress"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new BossController(
                mock(BossJobService.class),
                mock(com.wh.jobsbackend.application.service.BossService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                sameThreadExecutor
        )).build();

        MvcResult result = mockMvc.perform(get("/api/boss/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEventStreamMapping(result);
    }

    @Test
    void job51ProgressStreamShouldKeepEventStreamContentType() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("51job-progress"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new JobController(
                mock(Job51Service.class),
                mock(Job51JobService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                sameThreadExecutor
        )).build();

        MvcResult result = mockMvc.perform(get("/api/51job/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEventStreamMapping(result);
    }

    @Test
    void loginStatusStreamShouldKeepEventStreamContentType() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(progressStreamService.open(eq(42L), eq("login-status"), any())).thenReturn(new SseEmitter());

        MockMvc mockMvc = standaloneSetup(new JobController(
                mock(Job51Service.class),
                mock(Job51JobService.class),
                playwrightManager,
                mock(CookieService.class),
                currentUserService,
                progressStreamService,
                sameThreadExecutor
        )).build();

        MvcResult result = mockMvc.perform(get("/api/jobs/login-status/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEventStreamMapping(result);
    }

    private void assertEventStreamMapping(MvcResult result) {
        HandlerMethod handler = (HandlerMethod) result.getHandler();
        GetMapping mapping = handler.getMethodAnnotation(GetMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(Arrays.asList(mapping.produces())).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
