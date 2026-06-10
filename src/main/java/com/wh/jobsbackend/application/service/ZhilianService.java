package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wh.jobsbackend.application.entity.ZhilianConfigEntity;
import com.wh.jobsbackend.application.entity.ZhilianJobDataEntity;
import com.wh.jobsbackend.application.entity.ZhilianOptionEntity;
import com.wh.jobsbackend.application.mapper.ZhilianConfigMapper;
import com.wh.jobsbackend.application.mapper.ZhilianJobDataMapper;
import com.wh.jobsbackend.application.mapper.ZhilianOptionMapper;
import com.wh.jobsbackend.worker.zhilian.ZhilianConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZhilianService {
    private final ZhilianConfigMapper zhilianConfigMapper;
    private final ZhilianOptionMapper zhilianOptionMapper;
    private final ZhilianJobDataMapper zhilianJobDataMapper;
    private final DataSource dataSource;
    private final ReferenceDataService referenceDataService;

    /** 获取第一条配置记录（通常只有一条） */
    public ZhilianConfigEntity getFirstConfig() {
        QueryWrapper<ZhilianConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.last("LIMIT 1");
        return zhilianConfigMapper.selectOne(wrapper);
    }

    /** Build ZhilianConfig from the platform config table. */
    public ZhilianConfig loadZhilianConfig() {
        ZhilianConfigEntity entity = getFirstConfig();
        ZhilianConfig config = new ZhilianConfig();
        if (entity == null) {
            log.warn("hub_zhilian_config is empty, using defaults");
            config.setKeywords(new ArrayList<>());
            config.setCityCode("0");
            config.setSalary("0");
            return config;
        }

        config.setKeywords(parseListString(entity.getKeywords()));

        // City names map to platform codes; blank or unlimited maps to 0.
        String city = safeTrim(entity.getCityCode());
        if (city == null || city.isEmpty() || "\u4e0d\u9650".equals(city)) {
            config.setCityCode("0");
        } else if (city.chars().allMatch(Character::isDigit)) {
            config.setCityCode(city);
        } else {
            String code = getCodeByTypeAndName("city", city);
            if (code == null) {
                log.warn("Zhilian city {} was not found in hub_zhilian_option, using 0", city);
                config.setCityCode("0");
            } else {
                config.setCityCode(code);
            }
        }

        String salary = safeTrim(entity.getSalary());
        if (salary == null || salary.isEmpty() || "\u4e0d\u9650".equals(salary)) {
            config.setSalary("0");
        } else {
            config.setSalary(salary);
        }
        return config;
    }

    public List<String> parseListString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        String s = raw.trim().replace('\uFF0C', ',');
        if (s.startsWith("[") && s.endsWith("]")) s = s.substring(1, s.length() - 1);
        if (s.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(this::stripWrapperQuotes)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }

    private String safeTrim(String s) { return s == null ? null : s.trim(); }

    private String stripWrapperQuotes(String value) {
        if (value == null || value.length() < 2) return value;
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    /** 更新配置：有 ID 时按 ID 更新，否则更新首条记录。 */
    public ZhilianConfigEntity updateConfig(ZhilianConfigEntity config) {
        if (config == null) return null;
        if (config.getId() != null) {
            zhilianConfigMapper.updateById(config);
            return zhilianConfigMapper.selectById(config.getId());
        }
        return saveOrUpdateFirstSelective(config);
    }

    /**
     * 选择性保存或更新首条配置，避免空字段覆盖已有值。
     */
    public ZhilianConfigEntity saveOrUpdateFirstSelective(ZhilianConfigEntity incoming) {
        ZhilianConfigEntity first = getFirstConfig();
        LocalDateTime now = LocalDateTime.now();
        if (first == null) {
            ZhilianConfigEntity toInsert = new ZhilianConfigEntity();
            toInsert.setKeywords(incoming.getKeywords());
            toInsert.setCityCode(incoming.getCityCode());
            toInsert.setSalary(incoming.getSalary());
            toInsert.setCreatedAt(now);
            toInsert.setUpdatedAt(now);
            zhilianConfigMapper.insert(toInsert);
            return getFirstConfig();
        } else {
            ZhilianConfigEntity toUpdate = new ZhilianConfigEntity();
            toUpdate.setId(first.getId());
            if (incoming.getKeywords() != null) toUpdate.setKeywords(incoming.getKeywords());
            if (incoming.getCityCode() != null) toUpdate.setCityCode(incoming.getCityCode());
            if (incoming.getSalary() != null) toUpdate.setSalary(incoming.getSalary());
            toUpdate.setCreatedAt(first.getCreatedAt());
            toUpdate.setUpdatedAt(now);
            zhilianConfigMapper.updateById(toUpdate);
            return zhilianConfigMapper.selectById(first.getId());
        }
    }

    // ========== Option 管理 ==========

    public List<ZhilianOptionEntity> getOptionsByType(String type) {
        List<ReferenceDataService.OptionItem> items = "city".equals(type)
                ? referenceDataService.listCityOptionsForPlatform("zhilian")
                : referenceDataService.listPlatformOptionItems("zhilian", type);
        if (!items.isEmpty()) {
            return items.stream().map(this::toZhilianOption).collect(Collectors.toList());
        }
        return zhilianOptionMapper.selectList(
                new QueryWrapper<ZhilianOptionEntity>()
                        .eq("type", type)
                        .orderByAsc("sort_order")
        );
    }

    public ZhilianOptionEntity getOptionByTypeAndCode(String type, String code) {
        String name = referenceDataService.nameByCode("zhilian", type, code);
        if (name != null && !name.equals(code)) {
            ZhilianOptionEntity entity = new ZhilianOptionEntity();
            entity.setType(type);
            entity.setName(name);
            entity.setCode(code);
            return entity;
        }
        return zhilianOptionMapper.selectOne(
                new QueryWrapper<ZhilianOptionEntity>()
                        .eq("type", type)
                        .eq("code", code)
                        .last("LIMIT 1")
        );
    }

    public String getCodeByTypeAndName(String type, String name) {
        String code = referenceDataService.codeByName("zhilian", type, name);
        if (code != null && !code.isBlank()) {
            return code;
        }
        ZhilianOptionEntity e = zhilianOptionMapper.selectOne(
                new QueryWrapper<ZhilianOptionEntity>()
                        .eq("type", type)
                        .eq("name", name)
                        .last("LIMIT 1")
        );
        return e == null ? null : e.getCode();
    }

    public String getNameByTypeAndCode(String type, String code) {
        ZhilianOptionEntity e = getOptionByTypeAndCode(type, code);
        return e == null ? null : e.getName();
    }

    // ==================== 参考数据转换 ====================

    private ZhilianOptionEntity toZhilianOption(ReferenceDataService.OptionItem item) {
        ZhilianOptionEntity entity = new ZhilianOptionEntity();
        entity.setId(item.id());
        entity.setType(item.type());
        entity.setName(item.name());
        entity.setCode(item.code());
        entity.setSortOrder(item.sortOrder());
        return entity;
    }

    @PostConstruct
    public void ensureZhilianDataTableExists() {
        if (!Boolean.getBoolean("jobs.allowRuntimeDdl")) {
            log.debug("hub_zhilian_data schema is managed by Flyway");
            return;
        }
        String createSql = "CREATE TABLE IF NOT EXISTS hub_zhilian_data (" +
                " id BIGSERIAL PRIMARY KEY," +
                " job_id VARCHAR(64)," +
                " job_title VARCHAR(200)," +
                " job_link VARCHAR(300)," +
                " salary VARCHAR(100)," +
                " location VARCHAR(100)," +
                " experience VARCHAR(100)," +
                " degree VARCHAR(100)," +
                " company_name VARCHAR(200)," +
                " delivery_status VARCHAR(20) DEFAULT '\u672a\u6295\u9012'," +
                " create_time DATETIME," +
                " update_time DATETIME" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            log.info("hub_zhilian_data table is ready");
        } catch (Exception e) {
            log.warn("Failed to create hub_zhilian_data table: {}", e.getMessage());
        }
    }

    public boolean existsByJobId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) return false;
        QueryWrapper<ZhilianJobDataEntity> w = new QueryWrapper<>();
        w.eq("job_id", jobId).last("LIMIT 1");
        Long c = zhilianJobDataMapper.selectCount(w);
        return c != null && c > 0;
    }

    public boolean existsByTitleAndCompany(String jobTitle, String companyName) {
        if (jobTitle == null || companyName == null) return false;
        QueryWrapper<ZhilianJobDataEntity> w = new QueryWrapper<>();
        w.eq("job_title", jobTitle).eq("company_name", companyName).last("LIMIT 1");
        Long c = zhilianJobDataMapper.selectCount(w);
        return c != null && c > 0;
    }

    public void insertJob(ZhilianJobDataEntity entity) {
        if (entity == null) return;
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        if (entity.getDeliveryStatus() == null) entity.setDeliveryStatus("\u672a\u6295\u9012");
        zhilianJobDataMapper.insert(entity);
    }

    public void markDeliveredByJobId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) return;
        ZhilianJobDataEntity upd = new ZhilianJobDataEntity();
        upd.setDeliveryStatus("\u5df2\u6295\u9012");
        upd.setUpdateTime(LocalDateTime.now());
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ZhilianJobDataEntity> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("job_id", jobId);
        zhilianJobDataMapper.update(upd, uw);
    }

    public void markDeliveredByTitleAndCompany(String jobTitle, String companyName) {
        if (jobTitle == null || companyName == null) return;
        ZhilianJobDataEntity upd = new ZhilianJobDataEntity();
        upd.setDeliveryStatus("\u5df2\u6295\u9012");
        upd.setUpdateTime(LocalDateTime.now());
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ZhilianJobDataEntity> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("job_title", jobTitle).eq("company_name", companyName);
        zhilianJobDataMapper.update(upd, uw);
    }

    // ==================== 投递统计与 Dashboard 数据 ====================

    /** 薪资解析结果 */
    public static class SalaryInfo {
        public Integer minK;      // 月薪下限（K）
        public Integer maxK;      // 月薪上限（K）
        public Integer months;
        public Double medianK;
        public Long annualTotal;  // 估算年薪总额
    }

    /** 解析薪资文本，如 0-40K、45-65K·16薪、20K；无法解析时返回 null。 */
    public static SalaryInfo parseSalary(String salary) {
        if (salary == null) return null;
        String s = salary.trim();
        if (s.isEmpty()) return null;
        if (s.contains("\u9762\u8bae")) return null;
        s = s.replace(" ", "");

        Integer months = 12;
        java.util.regex.Matcher mMonths = java.util.regex.Pattern.compile("[\\u00B7\\.\\-]?([0-9]+)\\u85AA").matcher(s);
        if (mMonths.find()) {
            try { months = Integer.parseInt(mMonths.group(1)); } catch (Exception ignore) {}
            s = s.substring(0, mMonths.start());
        }

        Integer minK = null, maxK = null;
        java.util.regex.Matcher mRange = java.util.regex.Pattern.compile("^(\\d+)-(\\d+)[Kk]$").matcher(s);
        java.util.regex.Matcher mSingle = java.util.regex.Pattern.compile("^(\\d+)[Kk]$").matcher(s);
        if (mRange.find()) {
            try { minK = Integer.parseInt(mRange.group(1)); maxK = Integer.parseInt(mRange.group(2)); } catch (Exception ignore) {}
        } else if (mSingle.find()) {
            try { minK = Integer.parseInt(mSingle.group(1)); maxK = minK; } catch (Exception ignore) {}
        } else {
            String cleaned = s.replaceAll("[^0-9Kk\\-]", "");
            mRange = java.util.regex.Pattern.compile("^(\\d+)-(\\d+)[Kk]$").matcher(cleaned);
            mSingle = java.util.regex.Pattern.compile("^(\\d+)[Kk]$").matcher(cleaned);
            if (mRange.find()) {
                try { minK = Integer.parseInt(mRange.group(1)); maxK = Integer.parseInt(mRange.group(2)); } catch (Exception ignore) {}
            } else if (mSingle.find()) {
                try { minK = Integer.parseInt(mSingle.group(1)); maxK = minK; } catch (Exception ignore) {}
            }
        }

        if (minK == null || maxK == null) return null;

        SalaryInfo info = new SalaryInfo();
        info.minK = minK;
        info.maxK = maxK;
        info.months = months != null ? months : 12;
        info.medianK = (minK + maxK) / 2.0;
        info.annualTotal = Math.round(info.medianK * 1000 * info.months);
        return info;
    }

    /** KPI 指标 */
    public static class Kpi {
        public long total;
        public long delivered;
        public long pending;
        public long filtered; // 已过滤数量（delivery_status）
        public long failed;   // 投递失败数量（delivery_status）
        public Double avgMonthlyK; // 平均月薪（K）
    }

    /** name-value 结构 */
    public static class NameValue { public String name; public long value; public NameValue(){} public NameValue(String n,long v){name=n;value=v;} }
    /** 分桶统计结构 */
    public static class BucketValue { public String bucket; public long value; public BucketValue(){} public BucketValue(String b,long v){bucket=b;value=v;} }

    /** 图表数据 */
    public static class Charts {
        public List<NameValue> byStatus;
        public List<NameValue> byCity;
        public List<NameValue> byCompany;
        public List<NameValue> byExperience;
        public List<NameValue> byDegree;
        public List<BucketValue> salaryBuckets;
        public List<NameValue> dailyTrend; // date as name
    }

    /** 统计响应 */
    public static class StatsResponse { public Kpi kpi; public Charts charts; }

    /** 获取智联投递统计与图表数据，支持筛选条件。 */
    public StatsResponse getZhilianStats(
            List<String> statuses,
            String location,
            String experience,
            String degree,
            Double minK,
            Double maxK,
            String keyword
    ) {
        QueryWrapper<ZhilianJobDataEntity> wrapper = new QueryWrapper<>();
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in("delivery_status", statuses.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet()));
        }
        if (location != null && !location.trim().isEmpty()) wrapper.eq("location", location.trim());
        if (experience != null && !experience.trim().isEmpty()) wrapper.eq("experience", experience.trim());
        if (degree != null && !degree.trim().isEmpty()) wrapper.eq("degree", degree.trim());
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("company_name", kw)
                    .or().like("job_title", kw));
        }

        wrapper.orderByDesc("create_time");
        List<ZhilianJobDataEntity> all = zhilianJobDataMapper.selectList(wrapper);

        List<ZhilianJobDataEntity> filtered = new ArrayList<>();
        for (ZhilianJobDataEntity e : all) {
            if (minK == null && maxK == null) {
                filtered.add(e);
            } else {
                SalaryInfo info = parseSalary(e.getSalary());
                if (info == null || info.medianK == null) continue;
                boolean ok = true;
                if (minK != null) ok = ok && (info.medianK >= minK);
                if (maxK != null) ok = ok && (info.medianK <= maxK);
                if (ok) filtered.add(e);
            }
        }

        Kpi kpi = new Kpi();
        kpi.total = filtered.size();
        kpi.delivered = filtered.stream().filter(e -> "\u5df2\u6295\u9012".equals(nullSafe(e.getDeliveryStatus()))).count();
        kpi.pending = filtered.stream().filter(e -> "\u672a\u6295\u9012".equals(nullSafe(e.getDeliveryStatus()))).count();
        kpi.filtered = filtered.stream().filter(e -> "\u5df2\u8fc7\u6ee4".equals(nullSafe(e.getDeliveryStatus()))).count();
        kpi.failed = filtered.stream().filter(e -> "\u6295\u9012\u5931\u8d25".equals(nullSafe(e.getDeliveryStatus()))).count();
        {
            List<Double> medians = new ArrayList<>();
            for (ZhilianJobDataEntity e : filtered) {
                SalaryInfo info = parseSalary(e.getSalary());
                if (info == null || info.medianK == null) continue;
                medians.add(info.medianK);
            }
            kpi.avgMonthlyK = medians.isEmpty() ? null : medians.stream().mapToDouble(d -> d).average().orElse(0.0);
        }

        Charts charts = new Charts();
        charts.byStatus = new ArrayList<>();
        charts.byCity = new ArrayList<>();
        charts.byCompany = new ArrayList<>();
        charts.byExperience = new ArrayList<>();
        charts.byDegree = new ArrayList<>();
        charts.salaryBuckets = new ArrayList<>();
        charts.dailyTrend = new ArrayList<>();

        Map<String, Long> byStatus = filtered.stream()
                .collect(Collectors.groupingBy(e -> nullSafe(e.getDeliveryStatus()), Collectors.counting()));
        byStatus.forEach((k, v) -> charts.byStatus.add(new NameValue(k, v)));

        Map<String, Long> byCity = filtered.stream()
                .filter(e -> e.getLocation() != null && !e.getLocation().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> nullSafe(e.getLocation()), Collectors.counting()));
        byCity.forEach((k, v) -> charts.byCity.add(new NameValue(k, v)));

        Map<String, Long> byCompany = filtered.stream()
                .filter(e -> e.getCompanyName() != null && !e.getCompanyName().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> nullSafe(e.getCompanyName()), Collectors.counting()));
        byCompany.forEach((k, v) -> charts.byCompany.add(new NameValue(k, v)));

        Map<String, Long> byExp = filtered.stream()
                .filter(e -> e.getExperience() != null && !e.getExperience().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> nullSafe(e.getExperience()), Collectors.counting()));
        byExp.forEach((k, v) -> charts.byExperience.add(new NameValue(k, v)));

        Map<String, Long> byDegree = filtered.stream()
                .filter(e -> e.getDegree() != null && !e.getDegree().trim().isEmpty())
                .collect(Collectors.groupingBy(e -> nullSafe(e.getDegree()), Collectors.counting()));
        byDegree.forEach((k, v) -> charts.byDegree.add(new NameValue(k, v)));

        Map<String, Long> byDay = filtered.stream()
                .filter(e -> e.getCreateTime() != null)
                .collect(Collectors.groupingBy(e -> e.getCreateTime().toLocalDate().toString(), Collectors.counting()));
        byDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(en -> charts.dailyTrend.add(new NameValue(en.getKey(), en.getValue())));

        long b0_10=0,b10_15=0,b15_20=0,b20_top=0,b_ge_top=0;
        double maxMedian = 0.0;
        List<Double> medians = new ArrayList<>();
        for (ZhilianJobDataEntity e : filtered) {
            SalaryInfo info = parseSalary(e.getSalary());
            if (info == null || info.medianK == null) continue;
            double m = info.medianK;
            medians.add(m);
            if (m > maxMedian) maxMedian = m;
        }
        int topEdge = (int) Math.ceil(maxMedian / 5.0) * 5;
        if (topEdge <= 20) topEdge = 25;
        for (double m : medians) {
            if (m < 10) b0_10++;
            else if (m < 15) b10_15++;
            else if (m < 20) b15_20++;
            else if (m < topEdge) b20_top++;
            else b_ge_top++;
        }
        charts.salaryBuckets.add(new BucketValue("0-10K", b0_10));
        charts.salaryBuckets.add(new BucketValue("10-15K", b10_15));
        charts.salaryBuckets.add(new BucketValue("15-20K", b15_20));
        charts.salaryBuckets.add(new BucketValue("20-" + topEdge + "K", b20_top));
        charts.salaryBuckets.add(new BucketValue(">=" + topEdge + "K", b_ge_top));

        StatsResponse resp = new StatsResponse();
        resp.kpi = kpi;
        resp.charts = charts;
        return resp;
    }

    /** 列表查询（分页 + 筛选 + 关键词 + 薪资区间基于中位数K） */
    public PagedResult listZhilianJobs(
            List<String> statuses,
            String location,
            String experience,
            String degree,
            Double minK,
            Double maxK,
            String keyword,
            int page,
            int size
    ) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        QueryWrapper<ZhilianJobDataEntity> wrapper = new QueryWrapper<>();
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in("delivery_status", statuses.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet()));
        }
        if (location != null && !location.trim().isEmpty()) wrapper.eq("location", location.trim());
        if (experience != null && !experience.trim().isEmpty()) wrapper.eq("experience", experience.trim());
        if (degree != null && !degree.trim().isEmpty()) wrapper.eq("degree", degree.trim());

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("company_name", kw)
                    .or().like("job_title", kw));
        }

        wrapper.orderByDesc("create_time");
        List<ZhilianJobDataEntity> all = zhilianJobDataMapper.selectList(wrapper);

        List<ZhilianJobDataEntity> filtered = new ArrayList<>();
        for (ZhilianJobDataEntity e : all) {
            if (minK == null && maxK == null) {
                filtered.add(e);
            } else {
                SalaryInfo info = parseSalary(e.getSalary());
                if (info == null || info.medianK == null) continue;
                boolean ok = true;
                if (minK != null) ok = ok && (info.medianK >= minK);
                if (maxK != null) ok = ok && (info.medianK <= maxK);
                if (ok) filtered.add(e);
            }
        }

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);

        PagedResult pr = new PagedResult();
        pr.items = filtered.subList(from, to);
        pr.total = total;
        pr.page = page;
        pr.size = size;
        return pr;
    }

    public static class PagedResult {
        public List<ZhilianJobDataEntity> items;
        public long total;
        public int page;
        public int size;
    }

    private static String nullSafe(String s) { return s == null ? "" : s.trim(); }
}
