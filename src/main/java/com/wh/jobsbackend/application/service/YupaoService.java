package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wh.jobsbackend.application.entity.PlatformOptionEntity;
import com.wh.jobsbackend.application.entity.YupaoConfigEntity;
import com.wh.jobsbackend.application.entity.YupaoJobDataEntity;
import com.wh.jobsbackend.application.mapper.YupaoConfigMapper;
import com.wh.jobsbackend.application.mapper.YupaoJobDataMapper;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.worker.yupao.YupaoConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class YupaoService {
    private static final String PLATFORM = "yupao";
    private static final String STATUS_PENDING = "未投递";
    private static final String STATUS_DELIVERED = "已投递";
    private static final String STATUS_FILTERED = "已过滤";
    private static final String STATUS_FAILED = "投递失败";

    private final YupaoConfigMapper yupaoConfigMapper;
    private final YupaoJobDataMapper yupaoJobDataMapper;
    private final DataSource dataSource;
    private final ReferenceDataService referenceDataService;
    private final CurrentUserService currentUserService;

    @PostConstruct
    public void ensureYupaoDataTableExists() {
        if (!Boolean.getBoolean("jobs.allowRuntimeDdl")) {
            log.debug("hub_yupao_* schema is managed by Flyway");
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS hub_yupao_data (" +
                " id BIGSERIAL PRIMARY KEY," +
                " user_id BIGINT," +
                " job_id VARCHAR(128)," +
                " job_title VARCHAR(200)," +
                " job_link VARCHAR(500)," +
                " salary VARCHAR(100)," +
                " location VARCHAR(100)," +
                " experience VARCHAR(100)," +
                " degree VARCHAR(100)," +
                " company_name VARCHAR(200)," +
                " hr_name VARCHAR(100)," +
                " delivery_status VARCHAR(50)," +
                " publish_time VARCHAR(100)," +
                " create_time TIMESTAMP," +
                " update_time TIMESTAMP" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        } catch (Exception e) {
            log.warn("Failed to create hub_yupao_data table: {}", e.getMessage());
        }
    }

    public YupaoConfigEntity getFirstConfig() {
        Long userId = currentUserService.requireUserId();
        QueryWrapper<YupaoConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).last("LIMIT 1");
        return yupaoConfigMapper.selectOne(wrapper);
    }

    public YupaoConfigEntity updateConfig(YupaoConfigEntity config) {
        if (config == null) {
            return null;
        }
        Long userId = currentUserService.requireUserId();
        config.setUserId(userId);
        config.setUpdatedAt(LocalDateTime.now());
        if (config.getId() != null) {
            yupaoConfigMapper.update(config, new UpdateWrapper<YupaoConfigEntity>()
                    .eq("id", config.getId())
                    .eq("user_id", userId));
            return yupaoConfigMapper.selectById(config.getId());
        }
        return saveOrUpdateFirstSelective(config);
    }

    public YupaoConfigEntity saveOrUpdateFirstSelective(YupaoConfigEntity incoming) {
        Long userId = currentUserService.requireUserId();
        YupaoConfigEntity first = getFirstConfig();
        LocalDateTime now = LocalDateTime.now();
        if (first == null) {
            YupaoConfigEntity toInsert = new YupaoConfigEntity();
            toInsert.setUserId(userId);
            toInsert.setKeywords(incoming.getKeywords());
            toInsert.setCityCode(incoming.getCityCode());
            toInsert.setSalary(incoming.getSalary());
            toInsert.setJobType(incoming.getJobType());
            toInsert.setCreatedAt(now);
            toInsert.setUpdatedAt(now);
            yupaoConfigMapper.insert(toInsert);
            return toInsert;
        }

        YupaoConfigEntity toUpdate = new YupaoConfigEntity();
        toUpdate.setId(first.getId());
        toUpdate.setUserId(userId);
        if (incoming.getKeywords() != null) toUpdate.setKeywords(incoming.getKeywords());
        if (incoming.getCityCode() != null) toUpdate.setCityCode(incoming.getCityCode());
        if (incoming.getSalary() != null) toUpdate.setSalary(incoming.getSalary());
        if (incoming.getJobType() != null) toUpdate.setJobType(incoming.getJobType());
        toUpdate.setUpdatedAt(now);
        yupaoConfigMapper.update(toUpdate, new UpdateWrapper<YupaoConfigEntity>()
                .eq("id", first.getId())
                .eq("user_id", userId));
        return yupaoConfigMapper.selectById(first.getId());
    }

    public YupaoConfig loadYupaoConfig() {
        YupaoConfigEntity entity = getFirstConfig();
        YupaoConfig config = new YupaoConfig();
        if (entity == null) {
            return config;
        }
        config.setKeywords(parseListString(entity.getKeywords()));
        config.setCityCode(mapCode("city", entity.getCityCode(), "all"));
        config.setSalary(blankToDefault(entity.getSalary(), ""));
        config.setJobType(mapCode("jobType", entity.getJobType(), ""));
        return config;
    }

    public List<PlatformOptionEntity> getOptionsByType(String type) {
        List<ReferenceDataService.OptionItem> items = "city".equals(type)
                ? referenceDataService.listCityOptionsForPlatform(PLATFORM)
                : referenceDataService.listPlatformOptionItems(PLATFORM, type);
        return items.stream().map(this::toPlatformOption).collect(Collectors.toList());
    }

    public String getCodeByTypeAndName(String type, String name) {
        return referenceDataService.codeByName(PLATFORM, type, name);
    }

    public void insertJob(YupaoJobDataEntity entity) {
        if (entity == null) return;
        LocalDateTime now = LocalDateTime.now();
        entity.setUserId(currentUserService.requireUserId());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        if (entity.getDeliveryStatus() == null) entity.setDeliveryStatus(STATUS_PENDING);
        yupaoJobDataMapper.insert(entity);
    }

    public boolean existsByJobId(String jobId) {
        if (jobId == null || jobId.isBlank()) return false;
        QueryWrapper<YupaoJobDataEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUserService.requireUserId()).eq("job_id", jobId).last("LIMIT 1");
        Long count = yupaoJobDataMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    public void markDeliveredByJobId(String jobId) {
        updateStatusByJobId(jobId, STATUS_DELIVERED);
    }

    public void markFailedByJobId(String jobId) {
        updateStatusByJobId(jobId, STATUS_FAILED);
    }

    public StatsResponse getYupaoStats(List<String> statuses, String location, String experience,
                                       String degree, Double minK, Double maxK, String keyword) {
        List<YupaoJobDataEntity> filtered = filterJobs(statuses, location, experience, degree, minK, maxK, keyword);
        StatsResponse response = new StatsResponse();
        response.kpi = new Kpi();
        response.kpi.total = filtered.size();
        response.kpi.delivered = filtered.stream().filter(e -> STATUS_DELIVERED.equals(nullSafe(e.getDeliveryStatus()))).count();
        response.kpi.pending = filtered.stream().filter(e -> STATUS_PENDING.equals(nullSafe(e.getDeliveryStatus()))).count();
        response.kpi.filtered = filtered.stream().filter(e -> STATUS_FILTERED.equals(nullSafe(e.getDeliveryStatus()))).count();
        response.kpi.failed = filtered.stream().filter(e -> STATUS_FAILED.equals(nullSafe(e.getDeliveryStatus()))).count();
        response.kpi.avgMonthlyK = averageSalary(filtered);

        Charts charts = new Charts();
        charts.byStatus = group(filtered, YupaoJobDataEntity::getDeliveryStatus);
        charts.byCity = group(filtered, YupaoJobDataEntity::getLocation);
        charts.byCompany = group(filtered, YupaoJobDataEntity::getCompanyName);
        charts.byExperience = group(filtered, YupaoJobDataEntity::getExperience);
        charts.byDegree = group(filtered, YupaoJobDataEntity::getDegree);
        charts.salaryBuckets = salaryBuckets(filtered);
        charts.dailyTrend = filtered.stream()
                .filter(e -> e.getCreateTime() != null)
                .collect(Collectors.groupingBy(e -> e.getCreateTime().toLocalDate().toString(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new NameValue(entry.getKey(), entry.getValue()))
                .toList();
        response.charts = charts;
        return response;
    }

    public PagedResult listYupaoJobs(List<String> statuses, String location, String experience,
                                     String degree, Double minK, Double maxK, String keyword,
                                     int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        List<YupaoJobDataEntity> filtered = filterJobs(statuses, location, experience, degree, minK, maxK, keyword);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        PagedResult result = new PagedResult();
        result.items = from >= total ? List.of() : filtered.subList(from, to);
        result.total = total;
        result.page = page;
        result.size = size;
        return result;
    }

    public List<String> parseListString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        String s = raw.trim().replace('，', ',');
        if (s.startsWith("[") && s.endsWith("]")) s = s.substring(1, s.length() - 1);
        if (s.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(this::stripWrapperQuotes)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }

    private void updateStatusByJobId(String jobId, String status) {
        if (jobId == null || jobId.isBlank()) return;
        YupaoJobDataEntity update = new YupaoJobDataEntity();
        update.setDeliveryStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        yupaoJobDataMapper.update(update, new UpdateWrapper<YupaoJobDataEntity>()
                .eq("user_id", currentUserService.requireUserId())
                .eq("job_id", jobId));
    }

    private List<YupaoJobDataEntity> filterJobs(List<String> statuses, String location, String experience,
                                                String degree, Double minK, Double maxK, String keyword) {
        QueryWrapper<YupaoJobDataEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUserService.requireUserId());
        if (statuses != null && !statuses.isEmpty()) {
            Set<String> values = statuses.stream().filter(Objects::nonNull).map(String::trim)
                    .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            if (!values.isEmpty()) wrapper.in("delivery_status", values);
        }
        if (location != null && !location.trim().isEmpty()) wrapper.eq("location", location.trim());
        if (experience != null && !experience.trim().isEmpty()) wrapper.eq("experience", experience.trim());
        if (degree != null && !degree.trim().isEmpty()) wrapper.eq("degree", degree.trim());
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("company_name", kw).or().like("job_title", kw).or().like("hr_name", kw));
        }
        wrapper.orderByDesc("create_time");
        List<YupaoJobDataEntity> all = yupaoJobDataMapper.selectList(wrapper);
        if (minK == null && maxK == null) return all;
        return all.stream().filter(entity -> salaryPass(entity.getSalary(), minK, maxK)).toList();
    }

    private boolean salaryPass(String salary, Double minK, Double maxK) {
        SalaryInfo info = parseSalary(salary);
        if (info == null || info.medianK == null) return false;
        return (minK == null || info.medianK >= minK) && (maxK == null || info.medianK <= maxK);
    }

    private Double averageSalary(List<YupaoJobDataEntity> jobs) {
        List<Double> medians = jobs.stream()
                .map(job -> parseSalary(job.getSalary()))
                .filter(Objects::nonNull)
                .map(info -> info.medianK)
                .filter(Objects::nonNull)
                .toList();
        return medians.isEmpty() ? null : medians.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private List<NameValue> group(List<YupaoJobDataEntity> jobs, java.util.function.Function<YupaoJobDataEntity, String> getter) {
        return jobs.stream()
                .collect(Collectors.groupingBy(entity -> defaultUnknown(getter.apply(entity)), Collectors.counting()))
                .entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(10)
                .map(entry -> new NameValue(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<BucketValue> salaryBuckets(List<YupaoJobDataEntity> jobs) {
        long b0_10 = 0, b10_15 = 0, b15_20 = 0, b20_30 = 0, b30Plus = 0;
        for (YupaoJobDataEntity job : jobs) {
            SalaryInfo info = parseSalary(job.getSalary());
            if (info == null || info.medianK == null) continue;
            double median = info.medianK;
            if (median < 10) b0_10++;
            else if (median < 15) b10_15++;
            else if (median < 20) b15_20++;
            else if (median < 30) b20_30++;
            else b30Plus++;
        }
        return List.of(
                new BucketValue("0-10K", b0_10),
                new BucketValue("10-15K", b10_15),
                new BucketValue("15-20K", b15_20),
                new BucketValue("20-30K", b20_30),
                new BucketValue(">=30K", b30Plus)
        );
    }

    public static SalaryInfo parseSalary(String salary) {
        if (salary == null || salary.isBlank() || salary.contains("面议")) return null;
        String s = salary.trim().replace(" ", "").replace("薪", "");
        java.util.regex.Matcher range = java.util.regex.Pattern.compile("(\\d+)(?:k|K|千)?[-~至](\\d+)(?:k|K|千)").matcher(s);
        java.util.regex.Matcher single = java.util.regex.Pattern.compile("^(\\d+)(?:k|K|千)$").matcher(s);
        Integer minK = null;
        Integer maxK = null;
        if (range.find()) {
            minK = Integer.parseInt(range.group(1));
            maxK = Integer.parseInt(range.group(2));
        } else if (single.find()) {
            minK = Integer.parseInt(single.group(1));
            maxK = minK;
        }
        if (minK == null || maxK == null) return null;
        SalaryInfo info = new SalaryInfo();
        info.minK = minK;
        info.maxK = maxK;
        info.medianK = (minK + maxK) / 2.0;
        return info;
    }

    private String mapCode(String type, String value, String defaultValue) {
        String trimmed = blankToDefault(value, defaultValue);
        if (trimmed.equals(defaultValue) || "不限".equals(trimmed)) return defaultValue;
        String code = referenceDataService.codeByName(PLATFORM, type, trimmed);
        return code == null || code.isBlank() ? trimmed : code;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String stripWrapperQuotes(String value) {
        if (value == null || value.length() < 2) return value;
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private PlatformOptionEntity toPlatformOption(ReferenceDataService.OptionItem item) {
        PlatformOptionEntity entity = new PlatformOptionEntity();
        entity.setId(item.id());
        entity.setPlatform(PLATFORM);
        entity.setType(item.type());
        entity.setName(item.name());
        entity.setCode(item.code());
        entity.setSortOrder(item.sortOrder());
        return entity;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value.trim();
    }

    public static class SalaryInfo {
        public Integer minK;
        public Integer maxK;
        public Double medianK;
    }

    public static class Kpi {
        public long total;
        public long delivered;
        public long pending;
        public long filtered;
        public long failed;
        public Double avgMonthlyK;
    }

    public static class NameValue {
        public String name;
        public long value;
        public NameValue() {}
        public NameValue(String name, long value) { this.name = name; this.value = value; }
    }

    public static class BucketValue {
        public String bucket;
        public long value;
        public BucketValue() {}
        public BucketValue(String bucket, long value) { this.bucket = bucket; this.value = value; }
    }

    public static class Charts {
        public List<NameValue> byStatus;
        public List<NameValue> byCity;
        public List<NameValue> byCompany;
        public List<NameValue> byExperience;
        public List<NameValue> byDegree;
        public List<BucketValue> salaryBuckets;
        public List<NameValue> dailyTrend;
    }

    public static class StatsResponse {
        public Kpi kpi;
        public Charts charts;
    }

    public static class PagedResult {
        public List<YupaoJobDataEntity> items;
        public long total;
        public int page;
        public int size;
    }
}
