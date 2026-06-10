package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.entity.ZhilianConfigEntity;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.ZhilianService;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.ZhilianJobService;
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
@RequestMapping("/api/zhilian")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ZhilianController {

    private final ZhilianService zhilianService;
    private final PlaywrightManager playwrightManager;
    private final CookieService cookieService;
    private final ZhilianJobService zhilianJobService;
    private final CurrentUserService currentUserService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @GetMapping("/config")
    public Map<String, Object> getAllZhilianConfig() {
        Map<String, Object> result = new HashMap<>();
        ZhilianConfigEntity config = zhilianService.getFirstConfig();
        if (config == null) {
            config = new ZhilianConfigEntity();
        }

        Map<String, List<Map<String, String>>> options = new HashMap<>();
        options.put("city", zhilianService.getOptionsByType("city").stream().map(e -> {
            Map<String, String> item = new HashMap<>();
            item.put("name", e.getName());
            item.put("code", e.getCode());
            return item;
        }).collect(Collectors.toList()));
        options.put("salary", zhilianService.getOptionsByType("salary").stream().map(e -> {
            Map<String, String> item = new HashMap<>();
            item.put("name", e.getName());
            item.put("code", e.getCode());
            return item;
        }).collect(Collectors.toList()));

        result.put("config", config);
        result.put("options", options);
        return result;
    }

    @PutMapping("/config")
    public ZhilianConfigEntity updateConfig(@RequestBody ZhilianConfigEntity config) {
        return zhilianService.updateConfig(config);
    }

    @GetMapping("/config/options/city")
    public List<Map<String, String>> getCityOptions() {
        return zhilianService.getOptionsByType("city").stream().map(e -> {
            Map<String, String> item = new HashMap<>();
            item.put("name", e.getName());
            item.put("code", e.getCode());
            return item;
        }).collect(Collectors.toList());
    }

    @GetMapping("/config/options/salary")
    public List<Map<String, String>> getSalaryOptions() {
        return zhilianService.getOptionsByType("salary").stream().map(e -> {
            Map<String, String> item = new HashMap<>();
            item.put("name", e.getName());
            item.put("code", e.getCode());
            return item;
        }).collect(Collectors.toList());
    }

    @GetMapping("/login-status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            boolean isLoggedIn = playwrightManager.refreshLoginStatus(userId, "zhilian");
            response.put("success", true);
            response.put("isLoggedIn", isLoggedIn);
            response.put("message", isLoggedIn ? "已登录" : "未登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查智联登录状态失败", e);
            response.put("success", false);
            response.put("message", "检查登录状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> triggerZhilianLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.triggerZhilianLogin(userId);
            response.put("success", true);
            response.put("message", "已尝试打开智联二维码登录入口，请在浏览器扫码登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发智联登录失败", e);
            response.put("success", false);
            response.put("message", "触发登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutZhilian() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.setLoginStatus(userId, "zhilian", false);
            cookieService.clearCookieByPlatform("zhilian", "manual logout");
            try {
                playwrightManager.clearZhilianCookies();
            } catch (Exception e) {
                log.warn("清理智联运行时Cookie失败: {}", e.getMessage());
            }
            response.put("success", true);
            response.put("message", "智联招聘已退出登录，Cookie已清理");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("退出智联登录失败", e);
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/cookie")
    public ResponseEntity<Map<String, Object>> getZhilianCookieRecord() {
        Map<String, Object> response = new HashMap<>();
        try {
            CookieEntity cookie = cookieService.getCookieByPlatform("zhilian");
            Map<String, Object> data = new HashMap<>();
            if (cookie != null) {
                data.put("id", cookie.getId());
                data.put("platform", cookie.getPlatform());
                data.put("cookie_value", cookie.getCookieValue());
                data.put("remark", cookie.getRemark());
                data.put("created_at", cookie.getCreatedAt());
                data.put("updated_at", cookie.getUpdatedAt());
            } else {
                data.put("platform", "zhilian");
                data.put("cookie_value", null);
                data.put("message", "未找到智联招聘Cookie记录");
            }
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("读取智联Cookie记录失败", e);
            response.put("success", false);
            response.put("message", "读取Cookie记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/save-cookie")
    public ResponseEntity<Map<String, Object>> saveZhilianCookie() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.saveCookiesToDb(userId, "zhilian", "manual save");
            response.put("success", true);
            response.put("message", "已主动保存智联Cookie到数据库");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("保存智联Cookie失败", e);
            response.put("success", false);
            response.put("message", "保存智联Cookie失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/stats")
    public ZhilianService.StatsResponse stats(
        @RequestParam(value = "statuses", required = false) String statuses,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "experience", required = false) String experience,
        @RequestParam(value = "degree", required = false) String degree,
        @RequestParam(value = "minK", required = false) Double minK,
        @RequestParam(value = "maxK", required = false) Double maxK,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return zhilianService.getZhilianStats(parseStatuses(statuses), location, experience, degree, minK, maxK, keyword);
    }

    @GetMapping("/list")
    public ZhilianService.PagedResult list(
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
        return zhilianService.listZhilianJobs(
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

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startZhilianJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, "zhilian")) {
                response.put("success", false);
                response.put("message", "请先登录智联招聘");
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }

            boolean started = zhilianJobService.startDeliveryAsync(userId, taskExecutor, progressMessage ->
                log.info("[{}] {}", progressMessage.getPlatform(), progressMessage.getMessage())
            );
            if (!started) {
                response.put("success", false);
                response.put("message", "当前已有智联招聘任务在运行中");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "智联招聘任务启动成功");
            response.put("status", "started");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动智联任务失败", e);
            response.put("success", false);
            response.put("message", "启动智联招聘任务失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/apply-button-check")
    public ResponseEntity<Map<String, Object>> checkApplyButtons() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            ZhilianJobService.ApplyButtonInspection inspection = zhilianJobService.inspectApplyButtons(userId);
            response.put("success", true);
            response.put("platform", "zhilian");
            response.put("checked", true);
            response.put("keyword", inspection.keyword());
            response.put("jobCardCount", inspection.jobCardCount());
            response.put("applyButtonCount", inspection.applyButtonCount());
            response.put("visibleApplyButtonCount", inspection.visibleApplyButtonCount());
            response.put("firstButtonText", inspection.firstButtonText());
            response.put("message", inspection.message());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (PlatformPageModelException | IllegalArgumentException e) {
            log.warn("智联投递按钮干跑检查失败: {}", e.getMessage());
            response.put("success", false);
            response.put("platform", "zhilian");
            response.put("checked", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("智联投递按钮干跑检查异常", e);
            response.put("success", false);
            response.put("platform", "zhilian");
            response.put("checked", false);
            response.put("message", "智联投递按钮干跑检查失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopZhilianJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!zhilianJobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "当前没有正在运行的智联招聘任务");
                return ResponseEntity.badRequest().body(response);
            }

            zhilianJobService.stopDelivery(userId);
            response.put("success", true);
            response.put("message", "智联招聘任务停止请求已发送");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止智联任务失败", e);
            response.put("success", false);
            response.put("message", "停止智联招聘任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCurrentStatus(
            @RequestParam(value = "refreshLogin", defaultValue = "false") boolean refreshLogin
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            response.put("success", true);
            response.putAll(zhilianJobService.getStatus(userId));
            boolean isRunning = Boolean.TRUE.equals(response.get("isRunning"));
            if (!isRunning) {
                boolean cachedLoggedIn = playwrightManager.getCachedLoginStatus(userId, "zhilian");
                boolean isLoggedIn = cachedLoggedIn || (refreshLogin && playwrightManager.refreshLoginStatus(userId, "zhilian"));
                response.put("isLoggedIn", isLoggedIn);
            }
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取智联状态失败", e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "ZhilianController");
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
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
