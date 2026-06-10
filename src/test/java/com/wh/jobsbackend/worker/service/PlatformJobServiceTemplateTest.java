package com.wh.jobsbackend.worker.service;

import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformJobServiceTemplateTest {

    @Test
    void startDeliveryAsyncShouldRejectDuplicateStartForSameUserPlatform() {
        RecordingTaskService taskService = new RecordingTaskService();
        BlockingPlatformJobService service = new BlockingPlatformJobService(taskService);
        CapturingExecutor executor = new CapturingExecutor();

        assertTrue(service.startDeliveryAsync(1L, executor, message -> {}));
        assertFalse(service.startDeliveryAsync(1L, executor, message -> {}));
        assertTrue(service.isRunning(1L));
    }

    @Test
    void startDeliveryAsyncShouldClearOrphanRunningTaskAndRetryStart() {
        OrphanRunningTaskService taskService = new OrphanRunningTaskService();
        BlockingPlatformJobService service = new BlockingPlatformJobService(taskService);
        CapturingExecutor executor = new CapturingExecutor();

        assertTrue(service.startDeliveryAsync(1L, executor, message -> {}));

        assertEquals(1, taskService.failedRunningCount);
        assertTrue(service.isRunning(1L));
    }

    @Test
    void startDeliveryAsyncShouldAllowDifferentUsersOnSamePlatform() {
        RecordingTaskService taskService = new RecordingTaskService();
        BlockingPlatformJobService service = new BlockingPlatformJobService(taskService);
        CapturingExecutor executor = new CapturingExecutor();

        assertTrue(service.startDeliveryAsync(1L, executor, message -> {}));
        assertTrue(service.startDeliveryAsync(2L, executor, message -> {}));
        assertTrue(service.isRunning(1L));
        assertTrue(service.isRunning(2L));
    }

    @Test
    void executeDeliveryShouldMarkFailureAndResetRunningAfterFailure() {
        RecordingTaskService taskService = new RecordingTaskService();
        FailingPlatformJobService service = new FailingPlatformJobService(taskService);
        List<JobProgressMessage> messages = new ArrayList<>();

        service.executeDelivery(1L, messages::add);

        assertFalse(service.isRunning(1L));
        assertEquals(1, messages.size());
        assertEquals("test", messages.get(0).getPlatform());
        assertEquals("error", messages.get(0).getType());
        assertTrue(messages.get(0).getMessage().contains("boom"));
        assertEquals(10L, taskService.failedTaskId);
    }

    @Test
    void stopDeliveryShouldSetStopFlagWhenRunning() {
        RecordingTaskService taskService = new RecordingTaskService();
        BlockingPlatformJobService service = new BlockingPlatformJobService(taskService);
        CapturingExecutor executor = new CapturingExecutor();

        service.startDeliveryAsync(1L, executor, message -> {});
        service.stopDelivery(1L);

        assertTrue(service.shouldStop(1L));
        assertTrue((Boolean) service.getStatus(1L).get("isRunning"));
        assertEquals(10L, taskService.stoppedTaskId);
    }

    private static final class CapturingExecutor implements Executor {
        private Runnable command;

        @Override
        public void execute(Runnable command) {
            this.command = command;
        }
    }

    private static class BlockingPlatformJobService extends AbstractPlatformJobService {
        BlockingPlatformJobService(com.wh.jobsbackend.application.service.UserTaskService taskService) {
            super(taskService);
        }

        @Override
        protected String platform() {
            return "test";
        }

        @Override
        protected boolean isLoggedIn(Long userId) {
            return true;
        }

        @Override
        protected void doExecute(Long userId, Consumer<JobProgressMessage> progressCallback) {
        }
    }

    private static final class FailingPlatformJobService extends AbstractPlatformJobService {
        FailingPlatformJobService(RecordingTaskService taskService) {
            super(taskService);
        }

        @Override
        protected String platform() {
            return "test";
        }

        @Override
        protected boolean isLoggedIn(Long userId) {
            return true;
        }

        @Override
        protected void doExecute(Long userId, Consumer<JobProgressMessage> progressCallback) {
            throw new IllegalStateException("boom");
        }
    }

    private static final class RecordingTaskService extends com.wh.jobsbackend.application.service.UserTaskService {
        private Long failedTaskId;
        private Long stoppedTaskId;
        private final java.util.Set<String> running = new java.util.HashSet<>();

        RecordingTaskService() {
            super(null);
        }

        @Override
        public Optional<com.wh.jobsbackend.application.entity.UserJobTaskEntity> tryStart(Long userId, String platform) {
            String key = userId + ":" + platform;
            if (!running.add(key)) {
                return Optional.empty();
            }
            com.wh.jobsbackend.application.entity.UserJobTaskEntity task = new com.wh.jobsbackend.application.entity.UserJobTaskEntity();
            task.setId(userId * 10);
            task.setUserId(userId);
            task.setPlatform(platform);
            task.setStatus(com.wh.jobsbackend.application.service.UserTaskService.STATUS_RUNNING);
            return Optional.of(task);
        }

        @Override
        public void markSuccess(Long taskId, String message) {
        }

        @Override
        public void markFailed(Long taskId, String message) {
            failedTaskId = taskId;
        }

        @Override
        public void markStopped(Long taskId, String message) {
            stoppedTaskId = taskId;
        }
    }

    private static final class OrphanRunningTaskService extends com.wh.jobsbackend.application.service.UserTaskService {
        private int starts;
        private int failedRunningCount;

        OrphanRunningTaskService() {
            super(null);
        }

        @Override
        public Optional<com.wh.jobsbackend.application.entity.UserJobTaskEntity> tryStart(Long userId, String platform) {
            starts++;
            if (starts == 1) {
                return Optional.empty();
            }
            com.wh.jobsbackend.application.entity.UserJobTaskEntity task = new com.wh.jobsbackend.application.entity.UserJobTaskEntity();
            task.setId(20L);
            task.setUserId(userId);
            task.setPlatform(platform);
            task.setStatus(com.wh.jobsbackend.application.service.UserTaskService.STATUS_RUNNING);
            return Optional.of(task);
        }

        @Override
        public void failRunningTask(Long userId, String platform, String message) {
            failedRunningCount++;
        }
    }
}
