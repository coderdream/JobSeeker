package com.wh.jobsbackend.worker.service;

import com.wh.jobsbackend.application.entity.UserJobTaskEntity;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractPlatformJobService implements JobPlatformService {
    private final UserTaskService userTaskService;
    private final Map<Long, Long> runningTaskIds = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> stopRequests = new ConcurrentHashMap<>();

    protected AbstractPlatformJobService(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    @Override
    public void executeDelivery(Long userId, Consumer<JobProgressMessage> progressCallback) {
        Optional<UserJobTaskEntity> task = claimRun(userId, progressCallback);
        if (task.isEmpty()) {
            return;
        }
        executeClaimedDelivery(userId, task.get(), progressCallback);
    }

    @Override
    public boolean startDeliveryAsync(Long userId, Executor executor, Consumer<JobProgressMessage> progressCallback) {
        Optional<UserJobTaskEntity> task = claimRun(userId, progressCallback);
        if (task.isEmpty()) {
            return false;
        }
        CompletableFuture.runAsync(() -> executeClaimedDelivery(userId, task.get(), progressCallback), executor);
        return true;
    }

    @Override
    public void stopDelivery(Long userId) {
        Long taskId = runningTaskIds.get(userId);
        if (taskId != null) {
            log.info("Received stop request for {} delivery user {}", platform(), userId);
            stopRequests.put(userId, true);
            userTaskService.markStopped(taskId, "stop requested");
        }
    }

    @Override
    public Map<String, Object> getStatus(Long userId) {
        Map<String, Object> status = new HashMap<>();
        status.put("platform", platform());
        status.put("isRunning", isRunning(userId));
        status.put("isLoggedIn", isLoggedIn(userId));
        return status;
    }

    @Override
    public String getPlatformName() {
        return platform();
    }

    @Override
    public boolean isRunning(Long userId) {
        return runningTaskIds.containsKey(userId);
    }

    public boolean shouldStop(Long userId) {
        return stopRequests.getOrDefault(userId, false);
    }

    protected abstract String platform();

    protected abstract boolean isLoggedIn(Long userId);

    protected abstract void doExecute(Long userId, Consumer<JobProgressMessage> progressCallback);

    private Optional<UserJobTaskEntity> claimRun(Long userId, Consumer<JobProgressMessage> progressCallback) {
        Optional<UserJobTaskEntity> task = userTaskService.tryStart(userId, platform());
        if (task.isPresent()) {
            runningTaskIds.put(userId, task.get().getId());
            stopRequests.remove(userId);
            return task;
        }
        if (!runningTaskIds.containsKey(userId)) {
            userTaskService.failRunningTask(userId, platform(), "orphan running task cleaned before restart");
            task = userTaskService.tryStart(userId, platform());
            if (task.isPresent()) {
                runningTaskIds.put(userId, task.get().getId());
                stopRequests.remove(userId);
                return task;
            }
        }
        progressCallback.accept(JobProgressMessage.warning(platform(), "\u4efb\u52a1\u5df2\u5728\u8fd0\u884c\u4e2d"));
        return Optional.empty();
    }

    private void executeClaimedDelivery(Long userId, UserJobTaskEntity task, Consumer<JobProgressMessage> progressCallback) {
        try {
            doExecute(userId, progressCallback);
            if (shouldStop(userId)) {
                userTaskService.markStopped(task.getId(), "stopped");
            } else {
                userTaskService.markSuccess(task.getId(), "completed");
            }
        } catch (Exception e) {
            log.error("{} delivery failed", platform(), e);
            progressCallback.accept(JobProgressMessage.error(platform(), "\u6295\u9012\u5931\u8d25: " + e.getMessage()));
            userTaskService.markFailed(task.getId(), e.getMessage());
        } finally {
            runningTaskIds.remove(userId);
            stopRequests.remove(userId);
        }
    }
}
