package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.entity.Job51ConfigEntity;
import com.wh.jobsbackend.application.entity.Job51OptionEntity;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.stream.ProgressStreamService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.application.service.Job51Service;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.service.Job51JobService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JobController 负责 51job 平台配置、任务、分析、SSE 和登录状态接口。
 * 路径保留原有 /api/51job/... 和 /api/jobs/login-status/...。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobController {
    private static final String JOB51_PROGRESS_TOPIC = "51job-progress";
    private static final String LOGIN_STATUS_TOPIC = "login-status";

    // Services / Managers
    private final Job51Service job51Service;
    private final Job51JobService job51JobService;
    private final PlaywrightManager playwrightManager;
    private final CookieService cookieService;
    private final CurrentUserService currentUserService;
    private final ProgressStreamService progressStreamService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    private final Consumer<PlaywrightManager.LoginStatusChange> loginStatusListener = this::sendLoginStatusChange;

    @PostConstruct
    void registerLoginStatusListener() {
        playwrightManager.addLoginStatusListener(loginStatusListener);
    }

    @PreDestroy
    void unregisterLoginStatusListener() {
        playwrightManager.removeLoginStatusListener(loginStatusListener);
    }

    // ==================== 51job 投递进度 SSE ====================

    /** SSE - 51job 投递进度流 */
    @GetMapping(value = "/51job/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJob51Progress() {
        Long userId = currentUserService.requireUserId();
        return progressStreamService.open(userId, JOB51_PROGRESS_TOPIC, Map.of("message", "已连接到 51job 投递进度流"));
    }

    /** SSE - 登录状态变化流 */
    @GetMapping(value = "/jobs/login-status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLoginStatus() {
        Long userId = currentUserService.requireUserId();
        boolean bossLoggedIn = playwrightManager.getCachedLoginStatus(userId, "boss");
        boolean liepinLoggedIn = playwrightManager.getCachedLoginStatus(userId, "liepin");
        boolean job51LoggedIn = playwrightManager.getCachedLoginStatus(userId, "51job");
        boolean zhilianLoggedIn = playwrightManager.getCachedLoginStatus(userId, "zhilian");
        boolean yupaoLoggedIn = playwrightManager.getCachedLoginStatus(userId, "yupao");

        return progressStreamService.open(userId, LOGIN_STATUS_TOPIC, Map.of(
                "message", "已连接到登录状态流",
                "bossLoggedIn", bossLoggedIn,
                "liepinLoggedIn", liepinLoggedIn,
                "job51LoggedIn", job51LoggedIn,
                "zhilianLoggedIn", zhilianLoggedIn,
                "yupaoLoggedIn", yupaoLoggedIn
        ));
    }

    private void sendJob51Progress(Long userId, JobProgressMessage message) {
        progressStreamService.publish(userId, JOB51_PROGRESS_TOPIC, "progress", message);
    }

    private void sendLoginStatusChange(PlaywrightManager.LoginStatusChange change) {
        progressStreamService.publish(change.userId(), LOGIN_STATUS_TOPIC, "login-status", Map.of(
                "platform", change.platform(),
                "isLoggedIn", change.isLoggedIn(),
                "timestamp", change.timestamp()
        ));
    }

    /** 心跳 - 登录状态 SSE */
    @Scheduled(fixedRate = 30000)
    public void heartbeatLoginStatus() {
        progressStreamService.heartbeatAll(LOGIN_STATUS_TOPIC);
    }

    /** 心跳 - 51job 任务 SSE */
    @Scheduled(fixedRate = 30000)
    public void heartbeatJob51Progress() {
        progressStreamService.heartbeatAll(JOB51_PROGRESS_TOPIC);
    }

    // ==================== 51job 配置 / 登录 / Cookie / 任务 ====================

    /** 获取 51job 配置和选项 */
    @GetMapping("/51job/config")
    public Map<String, Object> getAllJob51Config() {
        Map<String, Object> result = new HashMap<>();

        Job51ConfigEntity config = job51Service.getFirstConfig();
        if (config == null) config = new Job51ConfigEntity();

        Map<String, List<Map<String, String>>> options = new HashMap<>();
        options.put("jobArea", buildOptionsFromDb("jobArea"));
        options.put("salary", buildOptionsFromDb("salary"));

        result.put("config", config);
        result.put("options", options);
        return result;
    }

    /** 更新 51job 配置 */
    @PutMapping("/51job/config")
    public Job51ConfigEntity updateConfig(@RequestBody Job51ConfigEntity config) {
        if (config == null) return job51Service.getFirstConfig();

        if (config.getKeywords() != null) {
            List<String> list = job51Service.parseListString(config.getKeywords());
            String normalized = toBracketListString(list);
            config.setKeywords(normalized);
        }

        if (config.getJobArea() != null) {
            List<String> raw = job51Service.parseListString(config.getJobArea());
            List<String> names = toNames("jobArea", raw);
            config.setJobArea(toBracketListString(names));
        }
        if (config.getSalary() != null) {
            List<String> raw = job51Service.parseListString(config.getSalary());
            List<String> names = toNames("salary", raw);
            config.setSalary(toBracketListString(names));
        }

        return job51Service.updateConfig(config);
    }

    /** 获取 jobArea 选项列表 */
    @GetMapping("/51job/config/options/jobArea")
    public List<Map<String, String>> getJobAreaOptions() { return buildOptionsFromDb("jobArea"); }

    /** 获取 salary 选项列表 */
    @GetMapping("/51job/config/options/salary")
    public List<Map<String, String>> getSalaryOptions() { return buildOptionsFromDb("salary"); }

    /** 触发 51job 登录流程 */
    @PostMapping("/51job/login")
    public ResponseEntity<Map<String, Object>> triggerLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.trigger51jobLogin(userId);
            response.put("success", true);
            response.put("message", "已打开 51job 登录页面，请扫码完成登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发 51job 登录失败", e);
            response.put("success", false);
            response.put("message", "触发登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 检查 51job 登录状态 */
    @GetMapping("/51job/login-status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus51() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            boolean isLoggedIn = playwrightManager.isLoggedIn(userId, "51job");
            response.put("success", true);
            response.put("isLoggedIn", isLoggedIn);
            response.put("message", isLoggedIn ? "已登录" : "未登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查登录状态失败", e);
            response.put("success", false);
            response.put("message", "检查登录状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 退出 51job 登录 */
    @PostMapping("/51job/logout")
    public ResponseEntity<Map<String, Object>> logout51job() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.setLoginStatus(userId, "51job", false);
            cookieService.clearCookieByPlatform("51job", "manual logout");
            try { playwrightManager.clear51jobCookies(); } catch (Exception e) { log.warn("清理 51job 浏览器 Cookie 异常: {}", e.getMessage()); }
            response.put("success", true);
            response.put("message", "51job 已退出登录，数据库 Cookie 和浏览器 Cookie 已清理");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 获取数据库中的 51job Cookie 记录 */
    @GetMapping("/51job/cookie")
    public ResponseEntity<Map<String, Object>> get51jobCookieRecord() {
        Map<String, Object> response = new HashMap<>();
        try {
            CookieEntity cookie = cookieService.getCookieByPlatform("51job");
            Map<String, Object> data = new HashMap<>();
            if (cookie != null) {
                data.put("id", cookie.getId());
                data.put("platform", cookie.getPlatform());
                data.put("cookie_value", cookie.getCookieValue());
                data.put("remark", cookie.getRemark());
                data.put("created_at", cookie.getCreatedAt());
                data.put("updated_at", cookie.getUpdatedAt());
            } else {
                data.put("platform", "51job");
                data.put("cookie_value", null);
                data.put("message", "未找到 51job Cookie 记录");
            }
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取 Cookie 记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 保存当前 51job Cookie 到数据库 */
    @PostMapping("/51job/save-cookie")
    public ResponseEntity<Map<String, Object>> save51jobCookie() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            playwrightManager.saveCookiesToDb(userId, "51job", "manual save");
            response.put("success", true);
            response.put("message", "已保存当前 51job Cookie 到数据库");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "保存 51job Cookie 失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 启动 51job 自动投递任务 */
    @PostMapping("/51job/start")
    public ResponseEntity<Map<String, Object>> start51jobJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!playwrightManager.isLoggedIn(userId, "51job")) {
                response.put("success", false);
                response.put("message", "请先登录 51job");
                response.put("status", "not_logged_in");
                return ResponseEntity.badRequest().body(response);
            }
            if (job51JobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "当前已有 51job 任务在运行中");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }
            boolean started = job51JobService.startDeliveryAsync(userId, taskExecutor, pm -> {
                sendJob51Progress(userId, pm);
                log.info("[{}] {}", pm.getPlatform(), pm.getMessage());
            });
            if (!started) {
                response.put("success", false);
                response.put("message", "当前已有 51job 任务在运行中");
                response.put("status", "running");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "51job 任务启动成功");
            response.put("status", "started");
            log.info("通过 API 启动 51job 任务成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动 51job 任务失败", e);
            response.put("success", false);
            response.put("message", "51job 任务启动失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 停止 51job 任务 */
    @PostMapping("/51job/stop")
    public ResponseEntity<Map<String, Object>> stop51jobJob() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            if (!job51JobService.isRunning(userId)) {
                response.put("success", false);
                response.put("message", "没有正在运行的 51job 任务");
                return ResponseEntity.badRequest().body(response);
            }
            job51JobService.stopDelivery(userId);
            response.put("success", true);
            response.put("message", "51job 任务停止请求已发送");
            log.info("通过 API 停止 51job 任务");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止 51job 任务失败", e);
            response.put("success", false);
            response.put("message", "停止 51job 任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 获取当前 51job 任务状态 */
    @GetMapping("/51job/status")
    public ResponseEntity<Map<String, Object>> getCurrentStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = currentUserService.requireUserId();
            Map<String, Object> status = job51JobService.getStatus(userId);
            response.put("success", true);
            response.putAll(status);
            response.put("isLoggedIn", playwrightManager.refreshLoginStatus(userId, "51job"));
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取当前状态失败", e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** 51job 健康检查 */
    @GetMapping("/51job/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "Job51Controller");
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // ==================== 51job Analytics ====================

    /** 投递分析统计和图表数据，支持筛选条件 */
    @GetMapping("/51job/stats")
    public Job51Service.StatsResponse getStats(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        List<String> statusList = null;
        if (statuses != null && !statuses.trim().isEmpty()) {
            statusList = List.of(statuses.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return job51Service.getJob51Stats(statusList, location, experience, degree, minK, maxK, keyword);
    }

    /** 岗位列表分页和筛选 */
    @GetMapping("/51job/list")
    public Job51Service.PagedResult51 list(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        List<String> statusList = null;
        if (statuses != null && !statuses.trim().isEmpty()) {
            statusList = List.of(statuses.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return job51Service.listJob51(statusList, location, experience, degree, minK, maxK, keyword, page, size);
    }

    /** 刷新 job51_data 派生数据 */
    @GetMapping("/51job/reload")
    public Map<String, Object> reload() { return job51Service.reloadJob51Data(); }

    // ==================== 辅助方法 ====================

    private String toBracketListString(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        String joined = list.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.replace("\"", "\\\""))
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));
        return "[" + joined + "]";
    }

    private List<String> toNames(String type, List<String> inputs) {
        List<Job51OptionEntity> options = job51Service.getOptionsByType(type);
        Map<String, String> codeToName = new HashMap<>();
        Map<String, String> nameToName = new HashMap<>();
        for (Job51OptionEntity o : options) {
            if (o.getCode() != null) codeToName.put(o.getCode(), o.getName());
            if (o.getName() != null) nameToName.put(o.getName(), o.getName());
        }
        List<String> names = new ArrayList<>();
        for (String s : inputs) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            String name = codeToName.getOrDefault(t, nameToName.getOrDefault(t, t));
            names.add(name);
        }
        return names;
    }

    private List<Map<String, String>> buildOptionsFromDb(String type) {
        List<Job51OptionEntity> rows = job51Service.getOptionsByType(type);
        List<Map<String, String>> list = new ArrayList<>();
        for (Job51OptionEntity row : rows) {
            Map<String, String> item = new HashMap<>();
            item.put("name", row.getName());
            item.put("code", row.getCode());
            list.add(item);
        }
        return list;
    }
}
