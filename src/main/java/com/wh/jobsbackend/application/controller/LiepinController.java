package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.entity.LiepinConfigEntity;
import com.wh.jobsbackend.application.entity.LiepinOptionEntity;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.LiepinService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.LiepinJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/liepin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LiepinController {

    private final LiepinJobService liepinJobService;
    private final PlaywrightManager playwrightManager;
    private final CookieService cookieService;
    private final LiepinService liepinService;
    private final CurrentUserService currentUserService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @GetMapping("/login-status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            boolean isLoggedIn = playwrightManager.refreshLoginStatus(userId, "liepin");
            response.put("success", true);
            response.put("isLoggedIn", isLoggedIn);
            response.put("message", isLoggedIn ? "已登录" : "未登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查猎聘登录状态失败", e);
            response.put("success", false);
            response.put("message", "检查登录状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> triggerLiepinLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.triggerLiepinLogin(userId);
            response.put("success", true);
            response.put("message", "已打开猎聘登录页面，请完成登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发猎聘登录失败", e);
            response.put("success", false);
            response.put("message", "触发登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startLiepinJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, "liepin")) {
                response.put("success", false);
                response.put("message", "请先登录猎聘");
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }

            boolean started = liepinJobService.startDeliveryAsync(userId, taskExecutor, progressMessage ->
                log.info("[{}] {}", progressMessage.getPlatform(), progressMessage.getMessage())
            );
            if (!started) {
                response.put("success", false);
                response.put("message", "当前已有猎聘任务在运行中");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "猎聘任务启动成功");
            response.put("status", "started");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动猎聘任务失败", e);
            response.put("success", false);
            response.put("message", "启动猎聘任务失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopLiepinJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!liepinJobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "当前没有正在运行的猎聘任务");
                return ResponseEntity.badRequest().body(response);
            }

            liepinJobService.stopDelivery(userId);
            response.put("success", true);
            response.put("message", "猎聘任务停止请求已发送");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止猎聘任务失败", e);
            response.put("success", false);
            response.put("message", "停止猎聘任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCurrentStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            response.put("success", true);
            response.putAll(liepinJobService.getStatus(userId));
            boolean isRunning = Boolean.TRUE.equals(response.get("isRunning"));
            if (!isRunning) {
                response.put("isLoggedIn", playwrightManager.refreshLoginStatus(userId, "liepin"));
            }
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取猎聘状态失败", e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "LiepinController");
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public Map<String, Object> getAllLiepinConfig() {
        Map<String, Object> result = new HashMap<>();
        LiepinConfigEntity config = liepinService.getFirstConfig();
        if (config == null) {
            config = new LiepinConfigEntity();
        }
        Map<String, List<LiepinOptionEntity>> options = new HashMap<>();
        options.put("city", liepinService.getOptionsByType("city"));
        options.put("salary", liepinService.getOptionsByType("salary"));
        options.put("compTag", liepinService.getOptionsByType("compTag"));
        options.put("pubTime", liepinService.getOptionsByType("pubTime"));
        options.put("workYearCode", liepinService.getOptionsByType("workYearCode"));
        options.put("degree", liepinService.getOptionsByType("degree"));
        options.put("industry", liepinService.getOptionsByType("industry"));
        options.put("jobType", liepinService.getOptionsByType("jobType"));
        options.put("scale", liepinService.getOptionsByType("scale"));
        options.put("stage", liepinService.getOptionsByType("stage"));
        options.put("compKind", liepinService.getOptionsByType("compKind"));
        result.put("config", config);
        result.put("options", options);
        return result;
    }

    @PutMapping("/config")
    public LiepinConfigEntity updateConfig(@RequestBody LiepinConfigEntity config) {
        if (config.getCity() != null && !config.getCity().isEmpty()) {
            config.setCity(liepinService.normalizeCityToName(config.getCity()));
        }
        if (config.getId() != null) {
            return liepinService.updateConfig(config);
        }
        return liepinService.saveOrUpdateFirstSelective(config);
    }

    @GetMapping("/config/options/{type}")
    public List<LiepinOptionEntity> getOptionsByType(@PathVariable String type) {
        return liepinService.getOptionsByType(type);
    }

    @GetMapping("/stats")
    public LiepinService.StatsResponse getStats(
        @RequestParam(value = "statuses", required = false) String statuses,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "experience", required = false) String experience,
        @RequestParam(value = "degree", required = false) String degree,
        @RequestParam(value = "minK", required = false) Double minK,
        @RequestParam(value = "maxK", required = false) Double maxK,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return liepinService.getLiepinStats(parseStatuses(statuses), location, experience, degree, minK, maxK, keyword);
    }

    @GetMapping("/list")
    public LiepinService.PagedResult list(
        @RequestParam(value = "statuses", required = false) String statuses,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "experience", required = false) String experience,
        @RequestParam(value = "degree", required = false) String degree,
        @RequestParam(value = "minK", required = false) Double minK,
        @RequestParam(value = "maxK", required = false) Double maxK,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        return liepinService.listLiepinJobs(
            parseStatuses(statuses),
            location,
            experience,
            degree,
            minK,
            maxK,
            keyword,
            page,
            size
        );
    }

    @GetMapping("/cookie")
    public ResponseEntity<Map<String, Object>> getLiepinCookieRecord() {
        Map<String, Object> response = new HashMap<>();
        try {
            CookieEntity cookie = cookieService.getCookieByPlatform("liepin");
            Map<String, Object> data = new HashMap<>();
            if (cookie != null) {
                data.put("id", cookie.getId());
                data.put("platform", cookie.getPlatform());
                data.put("cookie_value", cookie.getCookieValue());
                data.put("remark", cookie.getRemark());
                data.put("created_at", cookie.getCreatedAt());
                data.put("updated_at", cookie.getUpdatedAt());
            } else {
                data.put("platform", "liepin");
                data.put("cookie_value", null);
                data.put("message", "未找到猎聘Cookie记录");
            }
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("读取猎聘Cookie记录失败", e);
            response.put("success", false);
            response.put("message", "读取Cookie记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutLiepin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.setLoginStatus(userId, "liepin", false);
            cookieService.clearCookieByPlatform("liepin", "manual logout");
            try {
                playwrightManager.clearLiepinCookies();
            } catch (Exception e) {
                log.warn("清理猎聘运行时Cookie失败: {}", e.getMessage());
            }
            response.put("success", true);
            response.put("message", "猎聘已退出登录，Cookie已清理");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("退出猎聘登录失败", e);
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/save-cookie")
    public ResponseEntity<Map<String, Object>> saveLiepinCookie() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.saveCookiesToDb(userId, "liepin", "manual save");
            response.put("success", true);
            response.put("message", "已主动保存猎聘Cookie到数据库");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("保存猎聘Cookie失败", e);
            response.put("success", false);
            response.put("message", "保存猎聘Cookie失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private List<String> parseStatuses(String statuses) {
        if (statuses == null || statuses.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(statuses.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
