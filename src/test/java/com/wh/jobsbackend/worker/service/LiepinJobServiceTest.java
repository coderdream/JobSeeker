package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.entity.UserJobTaskEntity;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.liepin.Liepin;
import com.wh.jobsbackend.worker.liepin.LiepinConfig;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiepinJobServiceTest {

    @Test
    void executeDeliveryShouldRunLiepinExecutionUnderGlobalPlaywrightLock() {
        PlaywrightManager playwrightManager = mock(PlaywrightManager.class);
        ConfigService configService = mock(ConfigService.class);
        Liepin liepin = mock(Liepin.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<Liepin> liepinProvider = mock(ObjectProvider.class);
        UserTaskService userTaskService = mock(UserTaskService.class);
        Page page = mock(Page.class);
        Object globalLock = new Object();
        AtomicBoolean getPageUnderLock = new AtomicBoolean(false);
        AtomicBoolean executeUnderLock = new AtomicBoolean(false);

        when(userTaskService.tryStart(7L, "liepin")).thenReturn(Optional.of(task(70L, 7L)));
        when(playwrightManager.isLoggedIn(7L, "liepin")).thenReturn(true);
        when(playwrightManager.getPage(7L, "liepin")).thenAnswer(invocation -> {
            getPageUnderLock.set(Thread.holdsLock(globalLock));
            return page;
        });
        when(configService.getLiepinConfig()).thenReturn(new LiepinConfig());
        when(liepinProvider.getObject()).thenReturn(liepin);
        when(liepin.execute()).thenAnswer(invocation -> {
            executeUnderLock.set(Thread.holdsLock(globalLock));
            return 0;
        });
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0, Runnable.class);
            synchronized (globalLock) {
                action.run();
            }
            return null;
        }).when(playwrightManager).withPlaywrightLock(any(Runnable.class));

        LiepinJobService service = new LiepinJobService(
                playwrightManager,
                configService,
                liepinProvider,
                userTaskService
        );
        service.executeDelivery(7L, message -> {
        });

        assertTrue(getPageUnderLock.get(), "Liepin page lookup must run under the global Playwright lock");
        assertTrue(executeUnderLock.get(), "Liepin execution must run under the global Playwright lock");
        verify(userTaskService).markSuccess(70L, "completed");
    }

    private static UserJobTaskEntity task(Long taskId, Long userId) {
        UserJobTaskEntity task = new UserJobTaskEntity();
        task.setId(taskId);
        task.setUserId(userId);
        task.setPlatform("liepin");
        task.setStatus(UserTaskService.STATUS_RUNNING);
        return task;
    }
}
