package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.service.BossService;
import com.wh.jobsbackend.application.stream.ProgressStreamService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.BossJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Boss 平台控制器（单平台合并版）：进度 SSE 与任务接口
 */
@Slf4j
@RestController
@RequestMapping("/api/boss")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BossController {
    private static final String BOSS_PROGRESS_TOPIC = "boss-progress";

    private final BossJobService bossJobService;
    private final BossService bossService;
    private final PlaywrightManager playwrightManager;
    private final CookieService cookieService;
    private final CurrentUserService currentUserService;
    private final ProgressStreamService progressStreamService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    /** SSE - Boss投递任务进度推送 */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBossProgress() {
        Long userId = currentUserService.requireUserId();
        return progressStreamService.open(userId, BOSS_PROGRESS_TOPIC, Map.of("message", "已连接到Boss投递进度推送"));
    }

    /** POST - 启动Boss投递任务 */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeBoss() {
        Long userId = currentUserService.requireUserId();
        boolean started = bossJobService.startDeliveryAsync(userId, taskExecutor, message -> sendBossProgress(userId, message));
        if (!started) {
            return ResponseEntity.ok(Map.of(
                    "status", "already_running",
                    "message", "Boss投递任务已在运行中"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Boss投递任务已启动"
        ));
    }

    /** POST - 启动Boss投递任务（前端使用的接口）*/
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startBoss() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            try {
                boolean isLoggedIn = playwrightManager.refreshLoginStatus(userId, "boss");
                if (!isLoggedIn) {
                    response.put("success", false);
                    response.put("message", "Boss登录状态已失效，请重新登录");
                    response.put("status", "not_logged_in");
                    return ResponseEntity.badRequest().body(response);
                }
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", e.getMessage());
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }
            if (bossJobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "Boss任务已在运行中，请等待当前任务完成");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }
            boolean started = bossJobService.startDeliveryAsync(userId, taskExecutor, pm -> {
                sendBossProgress(userId, pm);
                log.info("[{}] {}", pm.getPlatform(), pm.getMessage());
            });
            if (!started) {
                response.put("success", false);
                response.put("message", "\u5f53\u524d\u5df2\u6709Boss\u4efb\u52a1\u5728\u8fd0\u884c\u4e2d");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "Boss任务启动成功");
            response.put("status", "started");
            log.info("通过API启动Boss任务成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动Boss任务失败", e);
            response.put("success", false);
            response.put("message", "启动Boss任务失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** POST - 停止Boss投递任务 */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopBoss() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!bossJobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "没有正在运行的Boss任务");
                return ResponseEntity.badRequest().body(response);
            }
            bossJobService.stopDelivery(userId);
            response.put("success", true);
            response.put("message", "Boss任务停止请求已发送");
            log.info("通过API停止Boss任务");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止Boss任务失败", e);
            response.put("success", false);
            response.put("message", "停止Boss任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** POST - trigger Boss login */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> triggerBossLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.triggerBossLogin(userId);
            response.put("success", true);
            response.put("message", "已打开 Boss 登录页面，请完成登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发 Boss 登录失败", e);
            response.put("success", false);
            response.put("message", "触发登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** POST - confirm Boss login completed in the system browser */
    @PostMapping("/login-confirmed")
    public ResponseEntity<Map<String, Object>> confirmBossLogin() {
        Long userId = currentUserService.requireUserId();
        try {
            boolean isLoggedIn = playwrightManager.refreshLoginStatus(userId, "boss");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "platform", "boss",
                    "isLoggedIn", isLoggedIn,
                    "message", "Boss 登录状态已刷新",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "platform", "boss",
                    "isLoggedIn", false,
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /** POST - logout Boss */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutBoss() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.setLoginStatus(userId, "boss", false);
            cookieService.clearCookieByPlatform("boss", "manual logout");
            try { 
                playwrightManager.clearBossCookies(); 
            } catch (Exception e) { 
                log.warn("清理Boss上下文Cookie异常: {}", e.getMessage()); 
            }
            response.put("success", true);
            response.put("message", "Boss已退出登录，数据库Cookie和上下文Cookie均已清理");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** GET - 获取Boss任务状态 */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBossStatus(@RequestParam(defaultValue = "false") boolean refreshLogin) {
        Long userId = currentUserService.requireUserId();
        Map<String, Object> status = new HashMap<>(bossJobService.getStatus(userId));
        status.put("success", true);
        boolean loggedIn = refreshLogin
                ? playwrightManager.refreshLoginStatus(userId, "boss")
                : playwrightManager.getCachedLoginStatus(userId, "boss");
        status.put("isLoggedIn", loggedIn);
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    private void sendBossProgress(Long userId, JobProgressMessage message) {
        progressStreamService.publish(userId, BOSS_PROGRESS_TOPIC, "progress", message);
    }

    /** POST - 一键投递：对前端勾选的岗位 id 列表发起投递 */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyJobs(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, "boss")) {
                response.put("success", false);
                response.put("message", "请先登录Boss直聘");
                return ResponseEntity.badRequest().body(response);
            }
            @SuppressWarnings("unchecked")
            java.util.List<Integer> rawIds = (java.util.List<Integer>) body.get("jobIds");
            if (rawIds == null || rawIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "jobIds 不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            java.util.List<Long> jobIds = rawIds.stream().map(i -> i.longValue()).collect(java.util.stream.Collectors.toList());
            // 从数据库加载岗位信息
            java.util.List<com.wh.jobsbackend.application.entity.BossJobDataEntity> jobs = jobIds.stream()
                    .map(bossService::findById)
                    .filter(e -> e != null && e.getJobUrl() != null && !e.getJobUrl().isBlank())
                    .collect(java.util.stream.Collectors.toList());
            if (jobs.isEmpty()) {
                response.put("success", false);
                response.put("message", "没有找到有效的岗位（jobUrl 不能为空）");
                return ResponseEntity.badRequest().body(response);
            }
            // 先把所有岗位状态改为"投递中"
            jobs.forEach(e -> bossService.updateDeliveryStatusById(e.getId(), "投递中"));

            // 异步执行投递
            taskExecutor.execute(() -> bossJobService.applySpecificJobs(userId, jobs, pm -> sendBossProgress(userId, pm)));

            response.put("success", true);
            response.put("message", String.format("已启动投递，共 %d 个岗位", jobs.size()));
            response.put("count", jobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("一键投递失败", e);
            response.put("success", false);
            response.put("message", "一键投递失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /** POST - 一键获取详情：对前端勾选的岗位 id 列表获取详情（JD 等信息） */
    @PostMapping("/fetch-details")
    public ResponseEntity<Map<String, Object>> fetchDetails(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, "boss")) {
                response.put("success", false);
                response.put("message", "请先登录Boss直聘");
                return ResponseEntity.badRequest().body(response);
            }
            @SuppressWarnings("unchecked")
            java.util.List<Integer> rawIds = (java.util.List<Integer>) body.get("jobIds");
            if (rawIds == null || rawIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "jobIds 不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            java.util.List<Long> jobIds = rawIds.stream().map(i -> i.longValue()).collect(java.util.stream.Collectors.toList());
            // 从数据库加载岗位信息
            java.util.List<com.wh.jobsbackend.application.entity.BossJobDataEntity> jobs = jobIds.stream()
                    .map(bossService::findById)
                    .filter(e -> e != null && e.getJobUrl() != null && !e.getJobUrl().isBlank())
                    .collect(java.util.stream.Collectors.toList());
            if (jobs.isEmpty()) {
                response.put("success", false);
                response.put("message", "没有找到有效的岗位（jobUrl 不能为空）");
                return ResponseEntity.badRequest().body(response);
            }

            // 异步执行获取详情
            taskExecutor.execute(() -> bossJobService.fetchSpecificJobDetails(userId, jobs, pm -> sendBossProgress(userId, pm)));

            response.put("success", true);
            response.put("message", String.format("已启动批量获取详情任务，共 %d 个岗位", jobs.size()));
            response.put("count", jobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量获取详情失败", e);
            response.put("success", false);
            response.put("message", "批量获取详情失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** PATCH - 更新单条岗位状态（废弃等） */
    @org.springframework.web.bind.annotation.PatchMapping("/jobs/{id}/status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            currentUserService.requireUserId(); // 仅做认证校验
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                response.put("success", false);
                response.put("message", "status 字段不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            bossService.updateDeliveryStatusById(id, status);
            response.put("success", true);
            response.put("message", "状态已更新为：" + status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新岗位状态失败 id={}", id, e);
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 心跳 - Boss进度 SSE */
    @Scheduled(fixedRate = 30000)
    public void heartbeatBossProgress() {
        progressStreamService.heartbeatAll(BOSS_PROGRESS_TOPIC);
    }
}
