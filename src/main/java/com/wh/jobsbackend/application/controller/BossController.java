package com.wh.jobsbackend.application.controller;

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

import java.awt.Desktop;
import java.net.URI;
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
            if (!playwrightManager.isLoggedIn(userId, "boss")) {
                response.put("success", false);
                /*
                response.put("message", "请先登录Boss直聘");
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }
            if (bossJobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "Boss任务已在运行中，请等待当前任务完成");
                */
                response.put("message", "\u5f53\u524d\u5df2\u6709Boss\u4efb\u52a1\u5728\u8fd0\u884c\u4e2d");
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
            currentUserService.requireUserId();
            openSystemBrowser("https://www.zhipin.com/web/user/?ka=header-login");
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

    private void openSystemBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
            return;
        }
        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
    }

    /** POST - confirm Boss login completed in the system browser */
    @PostMapping("/login-confirmed")
    public ResponseEntity<Map<String, Object>> confirmBossLogin() {
        Long userId = currentUserService.requireUserId();
        playwrightManager.setLoginStatus(userId, "boss", true);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "platform", "boss",
                "isLoggedIn", true,
                "message", "Boss 登录状态已刷新",
                "timestamp", System.currentTimeMillis()
        ));
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

    /** 心跳 - Boss进度 SSE */
    @Scheduled(fixedRate = 30000)
    public void heartbeatBossProgress() {
        progressStreamService.heartbeatAll(BOSS_PROGRESS_TOPIC);
    }
}
