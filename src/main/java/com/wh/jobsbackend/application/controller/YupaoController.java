package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionEntity;
import com.wh.jobsbackend.application.entity.YupaoConfigEntity;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.YupaoService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.YupaoJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/yupao")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class YupaoController {
    private static final String PLATFORM = "yupao";

    private final YupaoJobService yupaoJobService;
    private final PlaywrightManager playwrightManager;
    private final CookieService cookieService;
    private final YupaoService yupaoService;
    private final CurrentUserService currentUserService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> triggerYupaoLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.triggerYupaoLogin(userId);
            response.put("success", true);
            response.put("message", "已打开鱼泡直聘登录页，请完成登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发鱼泡登录失败", e);
            response.put("success", false);
            response.put("message", "触发鱼泡登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/login-status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            boolean loggedIn = playwrightManager.refreshLoginStatus(userId, PLATFORM);
            response.put("success", true);
            response.put("isLoggedIn", loggedIn);
            response.put("message", loggedIn ? "已登录" : "未登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查鱼泡登录状态失败", e);
            response.put("success", false);
            response.put("message", "检查登录状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/config")
    public Map<String, Object> getAllYupaoConfig() {
        Map<String, Object> result = new HashMap<>();
        YupaoConfigEntity config = yupaoService.getFirstConfig();
        if (config == null) {
            config = new YupaoConfigEntity();
        }
        Map<String, List<PlatformOptionEntity>> options = new HashMap<>();
        options.put("city", yupaoService.getOptionsByType("city"));
        options.put("salary", yupaoService.getOptionsByType("salary"));
        options.put("jobType", yupaoService.getOptionsByType("jobType"));
        result.put("config", config);
        result.put("options", options);
        return result;
    }

    @PutMapping("/config")
    public YupaoConfigEntity updateConfig(@RequestBody YupaoConfigEntity config) {
        if (config.getId() != null) {
            return yupaoService.updateConfig(config);
        }
        return yupaoService.saveOrUpdateFirstSelective(config);
    }

    @GetMapping("/config/options/{type}")
    public List<PlatformOptionEntity> getOptionsByType(@PathVariable String type) {
        return yupaoService.getOptionsByType(type);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startYupaoJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
                response.put("success", false);
                response.put("message", "请先登录鱼泡直聘");
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }
            boolean started = yupaoJobService.startDeliveryAsync(userId, taskExecutor, progressMessage ->
                    log.info("[{}] {}", progressMessage.getPlatform(), progressMessage.getMessage()));
            if (!started) {
                response.put("success", false);
                response.put("message", "当前已有鱼泡任务在运行中");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "鱼泡任务启动成功");
            response.put("status", "started");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动鱼泡任务失败", e);
            response.put("success", false);
            response.put("message", "启动鱼泡任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopYupaoJob() {
        Map<String, Object> response = new HashMap<>();
        Long userId = currentUserService.requireUserId();
        if (!yupaoJobService.isRunning(userId)) {
            response.put("success", false);
            response.put("message", "当前没有正在运行的鱼泡任务");
            return ResponseEntity.badRequest().body(response);
        }
        yupaoJobService.stopDelivery(userId);
        response.put("success", true);
        response.put("message", "鱼泡任务停止请求已发送");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCurrentStatus(
            @RequestParam(value = "refreshLogin", required = false, defaultValue = "false") boolean refreshLogin) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            response.put("success", true);
            response.putAll(yupaoJobService.getStatus(userId));
            boolean loggedIn = refreshLogin
                    ? playwrightManager.refreshLoginStatus(userId, PLATFORM)
                    : playwrightManager.getCachedLoginStatus(userId, PLATFORM);
            response.put("isLoggedIn", loggedIn);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取鱼泡状态失败", e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutYupao() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.setLoginStatus(userId, PLATFORM, false);
            cookieService.clearCookieByPlatform(PLATFORM, "manual logout");
            playwrightManager.clearYupaoCookies();
            response.put("success", true);
            response.put("message", "鱼泡已退出登录，Cookie 已清理");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/cookie")
    public ResponseEntity<Map<String, Object>> getYupaoCookieRecord() {
        Map<String, Object> response = new HashMap<>();
        CookieEntity cookie = cookieService.getCookieByPlatform(PLATFORM);
        Map<String, Object> data = new HashMap<>();
        if (cookie != null) {
            data.put("id", cookie.getId());
            data.put("platform", cookie.getPlatform());
            data.put("cookie_value", cookie.getCookieValue());
            data.put("remark", cookie.getRemark());
            data.put("created_at", cookie.getCreatedAt());
            data.put("updated_at", cookie.getUpdatedAt());
        } else {
            data.put("platform", PLATFORM);
            data.put("cookie_value", null);
        }
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-cookie")
    public ResponseEntity<Map<String, Object>> saveYupaoCookie() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.saveCookiesToDb(userId, PLATFORM, "manual save");
            response.put("success", true);
            response.put("message", "已保存鱼泡 Cookie");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "保存 Cookie 失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/stats")
    public YupaoService.StatsResponse getStats(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return yupaoService.getYupaoStats(parseStatuses(statuses), location, experience, degree, minK, maxK, keyword);
    }

    @GetMapping("/list")
    public YupaoService.PagedResult list(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return yupaoService.listYupaoJobs(parseStatuses(statuses), location, experience, degree, minK, maxK, keyword, page, size);
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
