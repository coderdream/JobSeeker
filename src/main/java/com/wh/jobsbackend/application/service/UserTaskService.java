package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.jobsbackend.application.entity.UserJobTaskEntity;
import com.wh.jobsbackend.application.mapper.UserJobTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTaskService {
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_STOPPED = "STOPPED";

    private final UserJobTaskMapper userJobTaskMapper;

    public Optional<UserJobTaskEntity> tryStart(Long userId, String platform) {
        failStaleRunningTasks(userId, platform);

        LambdaQueryWrapper<UserJobTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserJobTaskEntity::getUserId, userId)
                .eq(UserJobTaskEntity::getPlatform, platform)
                .eq(UserJobTaskEntity::getStatus, STATUS_RUNNING);
        if (userJobTaskMapper.selectCount(wrapper) > 0) {
            return Optional.empty();
        }

        UserJobTaskEntity task = new UserJobTaskEntity();
        task.setUserId(userId);
        task.setPlatform(platform);
        task.setStatus(STATUS_RUNNING);
        task.setStartedAt(LocalDateTime.now());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        userJobTaskMapper.insert(task);
        return Optional.of(task);
    }

    public void markSuccess(Long taskId, String message) {
        markTerminal(taskId, STATUS_SUCCESS, message);
    }

    public void markFailed(Long taskId, String message) {
        markTerminal(taskId, STATUS_FAILED, message);
    }

    public void markStopped(Long taskId, String message) {
        markTerminal(taskId, STATUS_STOPPED, message);
    }

    public void failRunningTask(Long userId, String platform, String message) {
        UserJobTaskEntity task = terminalTask(STATUS_FAILED, message);
        LambdaQueryWrapper<UserJobTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserJobTaskEntity::getUserId, userId)
                .eq(UserJobTaskEntity::getPlatform, platform)
                .eq(UserJobTaskEntity::getStatus, STATUS_RUNNING);
        userJobTaskMapper.update(task, wrapper);
    }

    private void markTerminal(Long taskId, String status, String message) {
        UserJobTaskEntity task = terminalTask(status, message);
        task.setId(taskId);
        userJobTaskMapper.updateById(task);
    }

    private UserJobTaskEntity terminalTask(String status, String message) {
        UserJobTaskEntity task = new UserJobTaskEntity();
        task.setStatus(status);
        task.setMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private void failStaleRunningTasks(Long userId, String platform) {
        UserJobTaskEntity task = terminalTask(STATUS_FAILED, "stale running task cleaned before restart");
        LambdaQueryWrapper<UserJobTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserJobTaskEntity::getUserId, userId)
                .eq(UserJobTaskEntity::getPlatform, platform)
                .eq(UserJobTaskEntity::getStatus, STATUS_RUNNING)
                .lt(UserJobTaskEntity::getUpdatedAt, LocalDateTime.now().minusHours(6));
        userJobTaskMapper.update(task, wrapper);
    }
}
