package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.service.BossService;
import com.wh.jobsbackend.application.entity.BossFilterConditionEntity;
import com.wh.jobsbackend.application.security.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/boss")
public class BossAnalyticsController {

    private final BossService bossService;
    private final CurrentUserService currentUserService;

    public BossAnalyticsController(BossService bossService, CurrentUserService currentUserService) {
        this.bossService = bossService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/filter-conditions")
    public List<BossFilterConditionEntity> listFilterConditions() {
        return bossService.listFilterConditions(currentUserService.requireUserId());
    }

    @PostMapping("/filter-conditions")
    public BossFilterConditionEntity saveFilterCondition(@RequestBody Map<String, String> body) {
        String name = body.get("filterName");
        String conditions = body.get("filterConditions");
        if (name == null || name.trim().isEmpty() || conditions == null || conditions.trim().isEmpty()) {
            throw new IllegalArgumentException("筛选名称和筛选条件不能为空");
        }
        return bossService.saveFilterCondition(currentUserService.requireUserId(), name.trim(), conditions);
    }

    /**
     * 投递分析统计与图表（支持与列表相同的筛选条件）
     */
    @GetMapping("/stats")
    public BossService.StatsResponse getStats(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filterHeadhunter", required = false) Boolean filterHeadhunter
    ) {
        List<String> statusList = null;
        if (statuses != null && !statuses.trim().isEmpty()) {
            statusList = Arrays.stream(statuses.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return bossService.getBossStats(
                statusList,
                location,
                experience,
                degree,
                minK,
                maxK,
                keyword,
                filterHeadhunter != null && filterHeadhunter
        );
    }

    /**
     * 岗位列表（分页 + 筛选）
     */
    @GetMapping("/list")
    public BossService.PagedResult list(
            @RequestParam(value = "statuses", required = false) String statuses,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "minK", required = false) Double minK,
            @RequestParam(value = "maxK", required = false) Double maxK,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filterHeadhunter", required = false) Boolean filterHeadhunter,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        List<String> statusList = null;
        if (statuses != null && !statuses.trim().isEmpty()) {
            statusList = Arrays.stream(statuses.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return bossService.listBossJobs(
                statusList,
                location,
                experience,
                degree,
                minK,
                maxK,
                keyword,
                page,
                size,
                filterHeadhunter != null && filterHeadhunter
        );
    }

    /**
     * 刷新 boss_data（列顺序检查 + VACUUM）
     */
    @GetMapping("/reload")
    public Map<String, Object> reload() {
        return bossService.reloadBossData();
    }
}
