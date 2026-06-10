package com.wh.jobsbackend.application.service;

import com.wh.jobsbackend.application.entity.UserJobTaskEntity;
import com.wh.jobsbackend.application.mapper.UserJobTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTaskServiceTest {

    @Test
    void tryStartShouldInsertRunningTaskForUserAndPlatform() {
        UserJobTaskMapper mapper = mock(UserJobTaskMapper.class);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(UserJobTaskEntity.class))).thenAnswer(invocation -> {
            UserJobTaskEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });
        UserTaskService service = new UserTaskService(mapper);

        Optional<UserJobTaskEntity> started = service.tryStart(42L, "boss");

        ArgumentCaptor<UserJobTaskEntity> captor = ArgumentCaptor.forClass(UserJobTaskEntity.class);
        verify(mapper).insert(captor.capture());
        assertTrue(started.isPresent());
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("boss", captor.getValue().getPlatform());
        assertEquals(UserTaskService.STATUS_RUNNING, captor.getValue().getStatus());
    }

    @Test
    void tryStartShouldReturnEmptyWhenSameUserPlatformAlreadyRuns() {
        UserJobTaskMapper mapper = mock(UserJobTaskMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.update(any(UserJobTaskEntity.class), any())).thenReturn(0);
        UserTaskService service = new UserTaskService(mapper);

        Optional<UserJobTaskEntity> started = service.tryStart(42L, "boss");

        assertTrue(started.isEmpty());
    }

    @Test
    void tryStartShouldFailStaleRunningTaskBeforeInsert() {
        UserJobTaskMapper mapper = mock(UserJobTaskMapper.class);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.update(any(UserJobTaskEntity.class), any())).thenReturn(1);
        when(mapper.insert(any(UserJobTaskEntity.class))).thenAnswer(invocation -> {
            UserJobTaskEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        UserTaskService service = new UserTaskService(mapper);

        Optional<UserJobTaskEntity> started = service.tryStart(42L, "liepin");

        assertTrue(started.isPresent());
        ArgumentCaptor<UserJobTaskEntity> updateCaptor = ArgumentCaptor.forClass(UserJobTaskEntity.class);
        verify(mapper).update(updateCaptor.capture(), any());
        assertEquals(UserTaskService.STATUS_FAILED, updateCaptor.getValue().getStatus());
        assertEquals("stale running task cleaned before restart", updateCaptor.getValue().getMessage());
        assertTrue(updateCaptor.getValue().getFinishedAt() != null);
        verify(mapper).insert(any(UserJobTaskEntity.class));
    }

    @Test
    void failRunningTaskShouldMarkCurrentPlatformRunningTaskFailed() {
        UserJobTaskMapper mapper = mock(UserJobTaskMapper.class);
        UserTaskService service = new UserTaskService(mapper);

        service.failRunningTask(42L, "liepin", "manual reset");

        ArgumentCaptor<UserJobTaskEntity> captor = ArgumentCaptor.forClass(UserJobTaskEntity.class);
        verify(mapper).update(captor.capture(), any());
        assertEquals(UserTaskService.STATUS_FAILED, captor.getValue().getStatus());
        assertEquals("manual reset", captor.getValue().getMessage());
        assertTrue(captor.getValue().getFinishedAt() != null);
    }

    @Test
    void terminalStatesShouldSetFinishedAtAndMessage() {
        UserJobTaskMapper mapper = mock(UserJobTaskMapper.class);
        UserTaskService service = new UserTaskService(mapper);

        service.markSuccess(1L, "done");
        service.markFailed(2L, "failed");
        service.markStopped(3L, "stopped");

        ArgumentCaptor<UserJobTaskEntity> captor = ArgumentCaptor.forClass(UserJobTaskEntity.class);
        verify(mapper, org.mockito.Mockito.times(3)).updateById(captor.capture());
        List<UserJobTaskEntity> values = captor.getAllValues();
        assertEquals(UserTaskService.STATUS_SUCCESS, values.get(0).getStatus());
        assertEquals(UserTaskService.STATUS_FAILED, values.get(1).getStatus());
        assertEquals(UserTaskService.STATUS_STOPPED, values.get(2).getStatus());
        assertEquals("done", values.get(0).getMessage());
        assertTrue(values.get(0).getFinishedAt() != null);
    }
}
