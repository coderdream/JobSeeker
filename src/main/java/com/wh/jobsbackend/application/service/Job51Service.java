package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wh.jobsbackend.application.entity.Job51ConfigEntity;
import com.wh.jobsbackend.application.entity.Job51Entity;
import com.wh.jobsbackend.application.entity.Job51OptionEntity;
import com.wh.jobsbackend.application.mapper.Job51ConfigMapper;
import com.wh.jobsbackend.application.mapper.Job51Mapper;
import com.wh.jobsbackend.application.mapper.Job51OptionMapper;
import com.wh.jobsbackend.worker.job51.Job51Config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class Job51Service {
    private final Job51ConfigMapper job51ConfigMapper;
    private final Job51OptionMapper job51OptionMapper;
    private final Job51Mapper job51Mapper;
    private final DataSource dataSource;
    private final ReferenceDataService referenceDataService;

    /** 获取第一条配置记录（通常只有一条） */
    public Job51ConfigEntity getFirstConfig() {
        QueryWrapper<Job51ConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.last("LIMIT 1");
        return job51ConfigMapper.selectOne(wrapper);
    }

    /** Build Job51Config from the platform config table. */
    public Job51Config loadJob51Config() {
        Job51ConfigEntity entity = getFirstConfig();
        Job51Config config = new Job51Config();
        if (entity == null) {
            log.warn("hub_job51_config is empty, using defaults");
            config.setKeywords(new ArrayList<>());
            config.setJobArea(new ArrayList<>());
            config.setSalary(new ArrayList<>());
            return config;
        }

        config.setKeywords(parseListString(entity.getKeywords()));

        // City area names or codes are normalized to platform codes.
        List<String> areaInputs = parseListString(entity.getJobArea());
        List<String> areaCodes = new ArrayList<>();
        for (String input : areaInputs) {
            if (input == null || input.isEmpty()) continue;
            String code = normalizeOptionCode("jobArea", input);
            areaCodes.add(code);
        }
        config.setJobArea(areaCodes);

        // Salary names or codes are normalized to platform codes.
        List<String> salaryInputs = parseListString(entity.getSalary());
        List<String> salaryCodes = new ArrayList<>();
        for (String input : salaryInputs) {
            if (input == null || input.isEmpty()) continue;
            String code = normalizeOptionCode("salary", input);
            salaryCodes.add(code);
        }
        config.setSalary(salaryCodes);

        return config;
    }

    public List<String> parseListString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        String s = raw.trim().replace('\uFF0C', ',');

        // 先尝试按 JSON 数组格式解析
        if (s.startsWith("[") && s.endsWith("]")) {
            try {
                String content = s.substring(1, s.length() - 1).trim();
                if (content.isEmpty()) return new ArrayList<>();

                List<String> result = new ArrayList<>();
                // 按逗号拆分，但忽略引号内的逗号
                boolean inQuotes = false;
                StringBuilder current = new StringBuilder();
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        String token = current.toString().trim();
                        if (!token.isEmpty()) {
                            // 去除包裹的双引号
                            if (token.startsWith("\"") && token.endsWith("\"")) {
                                token = token.substring(1, token.length() - 1);
                            }
                            token = token.replace("\\\"", "\"");
                            result.add(token);
                        }
                        current = new StringBuilder();
                    } else {
                        current.append(c);
                    }
                }
                String token = current.toString().trim();
                if (!token.isEmpty()) {
                    if (token.startsWith("\"") && token.endsWith("\"")) {
                        token = token.substring(1, token.length() - 1);
                    }
                    token = token.replace("\\\"", "\"");
                    result.add(token);
                }
                return result;
            } catch (Exception e) {
                log.warn("Failed to parse JSON array, falling back to comma split: {}", e.getMessage());
            }
        }

        return java.util.Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== Option 相关方法 ====================

    /** 获取指定类型的选项列表 */
    public List<Job51OptionEntity> getOptionsByType(String type) {
        List<ReferenceDataService.OptionItem> items = "jobArea".equals(type)
                ? referenceDataService.listCityOptionsForPlatform("51job")
                : referenceDataService.listPlatformOptionItems("51job", type);
        if (!items.isEmpty()) {
            return items.stream().map(this::toJob51Option).collect(Collectors.toList());
        }
        QueryWrapper<Job51OptionEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("type", type);
        wrapper.orderByAsc("sort_order", "id");
        return job51OptionMapper.selectList(wrapper);
    }

    /** 标准化选项输入：优先按中文名映射平台代码，其次兼容旧表中的 code/name。 */
    public String normalizeOptionCode(String type, String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String v = input.trim();
        String referenceType = "jobArea".equals(type) ? "city" : type;
        String platformCode = referenceDataService.codeByName("51job", referenceType, v);
        if (platformCode != null && !platformCode.isBlank()) return platformCode;
        String platformName = referenceDataService.nameByCode("51job", referenceType, v);
        if (platformName != null && !platformName.equals(v)) return v;
        // 兼容旧表：按 code 查询
        QueryWrapper<Job51OptionEntity> byCode = new QueryWrapper<>();
        byCode.eq("type", type).eq("code", v);
        Job51OptionEntity c = job51OptionMapper.selectOne(byCode);
        if (c != null) return c.getCode();
        // 兼容旧表：按 name 查询
        QueryWrapper<Job51OptionEntity> byName = new QueryWrapper<>();
        byName.eq("type", type).eq("name", v);
        Job51OptionEntity n = job51OptionMapper.selectOne(byName);
        if (n != null) return n.getCode();
        return v;
    }

    // ==================== 表结构初始化与数据迁移 ====================

    private Job51OptionEntity toJob51Option(ReferenceDataService.OptionItem item) {
        Job51OptionEntity entity = new Job51OptionEntity();
        entity.setId(item.id());
        entity.setType("city".equals(item.type()) ? "jobArea" : item.type());
        entity.setName(item.name());
        entity.setCode(item.code());
        entity.setSortOrder(item.sortOrder());
        return entity;
    }

    @PostConstruct
    public void ensureJob51OptionTableAndData() {
        if (!Boolean.getBoolean("jobs.allowRuntimeDdl")) {
            log.debug("job51 runtime schema is managed by Flyway");
            return;
        }
        // 仅在显式允许运行时 DDL 时创建旧表结构，正常由 Flyway 管理
        ensureJob51OptionTable();
        ensureJob51DataTable();
    }

    private void ensureJob51OptionTable() {
        String createSql = "CREATE TABLE IF NOT EXISTS hub_job51_option (" +
                " id BIGSERIAL PRIMARY KEY," +
                " type VARCHAR(50)," +
                " name VARCHAR(100)," +
                " code VARCHAR(100)," +
                " sort_order INTEGER," +
                " created_at DATETIME," +
                " updated_at DATETIME" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        } catch (Exception e) {
            log.warn("创建 hub_job51_option 表失败: {}", e.getMessage());
        }
    }

    // 以下为预置选项数据的插入代码，已注释保留以备将来需要从代码初始化数据时使用

    private void insertOption(String type, String name, String code, int sortOrder, LocalDateTime now) {
        try {
            Job51OptionEntity e = new Job51OptionEntity();
            e.setType(type);
            e.setName(name);
            e.setCode(code);
            e.setSortOrder(sortOrder);
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            job51OptionMapper.insert(e);
        } catch (Exception ex) {
            log.warn("插入 51job 选项失败 type={} name={} code={}: {}", type, name, code, ex.getMessage());
        }
    }

    /**
     * 更新配置：有 ID 时按 ID 更新，否则更新首条记录。
     */
    public Job51ConfigEntity updateConfig(Job51ConfigEntity config) {
        if (config == null) return null;
        if (config.getId() != null) {
            // 更新时刷新修改时间
            config.setUpdatedAt(LocalDateTime.now());
            job51ConfigMapper.updateById(config);
            return job51ConfigMapper.selectById(config.getId());
        }
        return saveOrUpdateFirstSelective(config);
    }

    /**
     * 选择性保存或更新首条配置，避免空字段覆盖已有值。
     */
    public Job51ConfigEntity saveOrUpdateFirstSelective(Job51ConfigEntity incoming) {
        Job51ConfigEntity first = getFirstConfig();
        LocalDateTime now = LocalDateTime.now();
        if (first == null) {
            Job51ConfigEntity toInsert = new Job51ConfigEntity();
            toInsert.setKeywords(incoming.getKeywords());
            toInsert.setJobArea(incoming.getJobArea());
            toInsert.setSalary(incoming.getSalary());
            toInsert.setCreatedAt(now);
            toInsert.setUpdatedAt(now);
            job51ConfigMapper.insert(toInsert);
            return getFirstConfig();
        } else {
            Job51ConfigEntity toUpdate = new Job51ConfigEntity();
            toUpdate.setId(first.getId());
            // 选择性更新非空字段
            if (incoming.getKeywords() != null) toUpdate.setKeywords(incoming.getKeywords());
            if (incoming.getJobArea() != null) toUpdate.setJobArea(incoming.getJobArea());
            if (incoming.getSalary() != null) toUpdate.setSalary(incoming.getSalary());
            toUpdate.setCreatedAt(first.getCreatedAt());
            toUpdate.setUpdatedAt(now);
            job51ConfigMapper.updateById(toUpdate);
            return job51ConfigMapper.selectById(first.getId());
        }
    }

    // ==================== 51job 表结构初始化与数据迁移 ====================

    /** 确保 hub_job51_data 表存在（含字段兼容性处理） */
    private void ensureJob51DataTable() {
        String createSql = "CREATE TABLE IF NOT EXISTS hub_job51_data (" +
                " job_id            BIGINT PRIMARY KEY," +
                " job_title         VARCHAR(200)," +
                " job_link          VARCHAR(300)," +
                " job_salary_text   VARCHAR(100)," +
                " job_area          VARCHAR(100)," +
                " job_edu_req       VARCHAR(50)," +
                " job_exp_req       VARCHAR(50)," +
                " job_publish_time  VARCHAR(50)," +
                " comp_id           BIGINT," +
                " comp_name         VARCHAR(200)," +
                " comp_industry     VARCHAR(100)," +
                " comp_scale        VARCHAR(50)," +
                " hr_id             VARCHAR(64)," +
                " hr_name           VARCHAR(50)," +
                " hr_title          VARCHAR(100)," +
                " delivered         INTEGER DEFAULT 0," +
                " create_time       TEXT," +
                " update_time       TEXT" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            try { stmt.execute("ALTER TABLE hub_job51_data ADD COLUMN delivered INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE hub_job51_data DROP COLUMN account_id"); } catch (Exception ignored) {}
            log.info("hub_job51_data table is ready");
        } catch (Exception e) {
            log.warn("Failed to create hub_job51_data table: {}", e.getMessage());
        }
    }

    /** 批量插入不存在的岗位数据，默认 delivered=0 */
    public void batchInsertIfNotExists(List<Job51Entity> entities) {
        if (entities == null || entities.isEmpty()) return;

        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (Job51Entity e : entities) {
            if (e != null && e.getJobId() != null) ids.add(e.getJobId());
        }
        if (ids.isEmpty()) return;

        List<Long> idList = new ArrayList<>(ids);
        List<Job51Entity> existing = job51Mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Job51Entity>().in("job_id", idList)
        );
        java.util.Set<Long> existingIds = new java.util.HashSet<>();
        if (existing != null) {
            for (Job51Entity e : existing) {
                if (e != null && e.getJobId() != null) existingIds.add(e.getJobId());
            }
        }

        List<Job51Entity> toInsert = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String nowIso = now.toString();
        for (Job51Entity e : entities) {
            if (e == null || e.getJobId() == null) continue;
            if (existingIds.contains(e.getJobId())) continue;
            if (e.getCreateTime() == null) e.setCreateTime(nowIso);
            e.setUpdateTime(nowIso);
            if (e.getDelivered() == null) e.setDelivered(0);
            toInsert.add(e);
        }
        if (toInsert.isEmpty()) return;

        String sql = "INSERT INTO hub_job51_data (" +
                "job_id, job_title, job_link, job_salary_text, job_area, job_edu_req, job_exp_req, job_publish_time, " +
                "comp_id, comp_name, comp_industry, comp_scale, " +
                "hr_id, hr_name, hr_title, delivered, create_time, update_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Job51Entity e : toInsert) {
                if (e.getJobId() == null) ps.setNull(1, java.sql.Types.BIGINT); else ps.setLong(1, e.getJobId());
                if (e.getJobTitle() == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, e.getJobTitle());
                if (e.getJobLink() == null) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, e.getJobLink());
                if (e.getJobSalaryText() == null) ps.setNull(4, java.sql.Types.VARCHAR); else ps.setString(4, e.getJobSalaryText());
                if (e.getJobArea() == null) ps.setNull(5, java.sql.Types.VARCHAR); else ps.setString(5, e.getJobArea());
                if (e.getJobEduReq() == null) ps.setNull(6, java.sql.Types.VARCHAR); else ps.setString(6, e.getJobEduReq());
                if (e.getJobExpReq() == null) ps.setNull(7, java.sql.Types.VARCHAR); else ps.setString(7, e.getJobExpReq());
                if (e.getJobPublishTime() == null) ps.setNull(8, java.sql.Types.VARCHAR); else ps.setString(8, e.getJobPublishTime());
                if (e.getCompId() == null) ps.setNull(9, java.sql.Types.BIGINT); else ps.setLong(9, e.getCompId());
                if (e.getCompName() == null) ps.setNull(10, java.sql.Types.VARCHAR); else ps.setString(10, e.getCompName());
                if (e.getCompIndustry() == null) ps.setNull(11, java.sql.Types.VARCHAR); else ps.setString(11, e.getCompIndustry());
                if (e.getCompScale() == null) ps.setNull(12, java.sql.Types.VARCHAR); else ps.setString(12, e.getCompScale());
                if (e.getHrId() == null) ps.setNull(13, java.sql.Types.VARCHAR); else ps.setString(13, e.getHrId());
                if (e.getHrName() == null) ps.setNull(14, java.sql.Types.VARCHAR); else ps.setString(14, e.getHrName());
                if (e.getHrTitle() == null) ps.setNull(15, java.sql.Types.VARCHAR); else ps.setString(15, e.getHrTitle());
                if (e.getDelivered() == null) ps.setNull(16, java.sql.Types.INTEGER); else ps.setInt(16, e.getDelivered());
                if (e.getCreateTime() == null) ps.setNull(17, java.sql.Types.VARCHAR); else ps.setString(17, e.getCreateTime());
                if (e.getUpdateTime() == null) ps.setNull(18, java.sql.Types.VARCHAR); else ps.setString(18, e.getUpdateTime());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            log.warn("批量保存 51job 岗位数据失败: {}", e.getMessage());
        }
    }

    /** 解析并持久化 51job 搜索接口返回的 JSON 数据。 */
    public void parseAndPersistJob51SearchJson(String json) {
        if (json == null || json.isEmpty()) return;
        String trimmed = json.trim();
        // 跳过非 JSON 内容
        if (trimmed.startsWith("<")) {
            // 跳过 HTML 等非 JSON 内容
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

            // 兼容多种 51job 搜索接口返回结构
            com.fasterxml.jackson.databind.JsonNode list = root.path("data").path("items");
            if (!list.isArray()) list = root.path("data").path("jobList");
            if (!list.isArray()) list = root.path("data").path("list");
            if (!list.isArray()) list = root.path("data").path("jobs");
            if (!list.isArray()) list = root.path("resultbody").path("job").path("items");
            if (!list.isArray()) list = root.path("job").path("items");
            if (!list.isArray()) list = root.path("resultbody").path("items");
            if (!list.isArray()) {
                // 未识别到岗位列表，跳过
                return;
            }

            List<Job51Entity> entities = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode item : list) {
                Long jobId = readLong(item.path("jobId"));
                if (jobId == null) continue;

                Job51Entity e = new Job51Entity();
                e.setJobId(jobId);
                // 岗位字段
                e.setJobTitle(readText(item, "jobName", "jobTitle", "title"));
                e.setJobSalaryText(readText(item, "provideSalaryString", "salaryDesc", "salary", "salaryText"));
                e.setJobArea(readText(item, "jobAreaString", "jobArea", "cityName"));
                e.setJobEduReq(readText(item, "degreeString", "degree", "requireEduLevel"));
                e.setJobExpReq(readText(item, "workYearString", "workYear", "requireWorkYears"));
                e.setJobPublishTime(readText(item, "issueDateString", "issueDate", "updateDate", "refreshTime"));

                // 公司字段
                e.setCompId(readLong(item.path("ctmId"), item.path("companyId")));
                e.setCompName(readText(item, "fullCompanyName", "companyName", "ctmName"));
                e.setCompIndustry(readText(item, "industryType1Str", "industry", "compIndustry"));
                e.setCompScale(readText(item, "companySizeString", "companySize", "compScale"));

                // HR 信息字段
                e.setHrId(readText(item, "hrUid", "recruiterId"));
                e.setHrName(readText(item, "hrName", "recruiterName"));
                e.setHrTitle(readText(item, "hrPosition", "recruiterTitle"));

                // 构造职位链接。优先使用 API 返回的 jobHref，缺失则使用 PC 端默认链接
                String jobHref = readText(item, "jobHref");
                if (jobHref == null || jobHref.isEmpty()) {
                    jobHref = "https://we.51job.com/pc/jobdetail?jobId=" + jobId;
                }
                e.setJobLink(jobHref);

                entities.add(e);
            }
            batchInsertIfNotExists(entities);
            // 保存完成
        } catch (Exception e) {
            // 解析失败时静默跳过
        }
    }

    // 从多个候选字段中读取文本
    private static String readText(com.fasterxml.jackson.databind.JsonNode item, String... keys) {
        for (String k : keys) {
            String v = safeText(item.path(k));
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static String safeText(com.fasterxml.jackson.databind.JsonNode node) {
        try { String v = node.asText(null); return (v == null || v.equals("null")) ? null : v; } catch (Exception ignored) { return null; }
    }

    private static Long readLong(com.fasterxml.jackson.databind.JsonNode... nodes) {
        for (com.fasterxml.jackson.databind.JsonNode n : nodes) {
            try {
                if (n == null) continue;
                String v = n.asText(null);
                if (v != null && !v.isEmpty() && !"null".equalsIgnoreCase(v)) {
                    return Long.parseLong(v);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ==================== 投递状态管理 ====================

    /** 按 jobId 标记为已投递 */
    public void markDelivered(Long jobId) {
        if (jobId == null) return;
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "UPDATE hub_job51_data SET delivered=1, update_time=? WHERE job_id=?")) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, now.toString());
            ps.setLong(2, jobId);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("标记 51job 投递状态失败 job_id={}: {}", jobId, e.getMessage());
        }
    }

    /** 批量标记为已投递 */
    public void markDeliveredBatch(java.util.Collection<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) return;
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "UPDATE hub_job51_data SET delivered=1, update_time=? WHERE job_id=?")) {
            conn.setAutoCommit(false);
            LocalDateTime now = LocalDateTime.now();
            for (Long id : jobIds) {
                if (id == null) continue;
                ps.setString(1, now.toString());
                ps.setLong(2, id);
                ps.addBatch();
            }
            int[] counts = ps.executeBatch();
            conn.commit();
            try {
                int updated = 0;
                if (counts != null) {
                    for (int c : counts) {
                        if (c > 0) updated += c;
                    }
                }
                String sample = jobIds.stream().filter(java.util.Objects::nonNull).limit(5).map(String::valueOf).collect(Collectors.joining(", "));
                log.info("[51job] 批量标记投递状态，入参 {} 个，实际更新 {} 个，样例 ID: {}", jobIds.size(), updated, sample);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.warn("批量标记 51job 投递状态失败: {}", e.getMessage());
        }
    }

    // ==================== 投递统计与列表查询 ====================

    public static class NameValue { public String name; public long value; public NameValue() {} public NameValue(String name, long value) { this.name = name; this.value = value; } }
    public static class BucketValue { public String bucket; public long value; public BucketValue() {} public BucketValue(String bucket, long value) { this.bucket = bucket; this.value = value; } }
    public static class Charts {
        public List<NameValue> byStatus;
        public List<NameValue> byCity;
        public List<NameValue> byIndustry;
        public List<NameValue> byCompany;
        public List<NameValue> byExperience;
        public List<NameValue> byDegree;
        public List<BucketValue> salaryBuckets;
        public List<NameValue> dailyTrend; // date as name
    }
    public static class Kpi { public long total; public long delivered; public long pending; public long filtered; public long failed; public Double avgMonthlyK; }
    public static class StatsResponse { public Kpi kpi; public Charts charts; }

    public static class Job51Row {
        public Long jobId;
        public String companyName;
        public String jobName;
        public String salary;
        public String location;
        public String experience;
        public String degree;
        public String hrName;
        public String deliveryStatus;
        public String jobUrl;
        public String publishTime;
        public String createdAt;
        public String industry;
        public String companyScale;
    }
    public static class PagedResult51 {
        public List<Job51Row> items;
        public long total;
        public int page;
        public int size;
    }

    /** 获取 51job 投递分析统计与图表数据（按筛选条件） */
    public StatsResponse getJob51Stats(
            List<String> statuses,
            String location,
            String experience,
            String degree,
            Double minK,
            Double maxK,
            String keyword
    ) {
        StatsResponse resp = new StatsResponse();
        resp.kpi = new Kpi();
        Charts charts = new Charts();
        charts.byStatus = new ArrayList<>();
        charts.byCity = new ArrayList<>();
        charts.byIndustry = new ArrayList<>();
        charts.byCompany = new ArrayList<>();
        charts.byExperience = new ArrayList<>();
        charts.byDegree = new ArrayList<>();
        charts.salaryBuckets = new ArrayList<>();
        charts.dailyTrend = new ArrayList<>();

        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Job51Entity> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            if (statuses != null && !statuses.isEmpty()) {
                List<Integer> deliveredVals = new ArrayList<>();
                if (statuses.contains("\u5df2\u6295\u9012")) deliveredVals.add(1);
                if (statuses.contains("\u672a\u6295\u9012")) deliveredVals.add(0);
                if (!deliveredVals.isEmpty()) wrapper.in("delivered", deliveredVals);
            }
            if (location != null && !location.trim().isEmpty()) wrapper.eq("job_area", location.trim());
            if (experience != null && !experience.trim().isEmpty()) wrapper.eq("job_exp_req", experience.trim());
            if (degree != null && !degree.trim().isEmpty()) wrapper.eq("job_edu_req", degree.trim());
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                wrapper.and(w -> w.like("comp_name", kw).or().like("job_title", kw).or().like("hr_name", kw));
            }
            wrapper.orderByDesc("update_time");

            List<Job51Entity> all = job51Mapper.selectList(wrapper);

            // 内存中进行薪资区间过滤（按中位数K）
            List<Job51Entity> filtered = new ArrayList<>();
            double sumMedian = 0.0; long countMedian = 0;
            List<Double> medians = new ArrayList<>();
            for (Job51Entity e : all) {
                SalaryInfo info = parse51Salary(e.getJobSalaryText());
                boolean passSalary;
                if (minK == null && maxK == null) passSalary = true;
                else {
                    if (info == null || info.medianK == null) passSalary = false;
                    else {
                        boolean ok = true;
                        if (minK != null) ok &= (info.medianK >= minK);
                        if (maxK != null) ok &= (info.medianK <= maxK);
                        passSalary = ok;
                    }
                }
                if (passSalary) {
                    filtered.add(e);
                    if (info != null && info.medianK != null) { sumMedian += info.medianK; countMedian++; medians.add(info.medianK); }
                }
            }

            // KPI
            resp.kpi.total = filtered.size();
            resp.kpi.delivered = filtered.stream().filter(e -> e.getDelivered() != null && e.getDelivered() == 1).count();
            resp.kpi.pending = filtered.stream().filter(e -> e.getDelivered() == null || e.getDelivered() == 0).count();
            resp.kpi.filtered = 0;
            resp.kpi.failed = 0;

            // Charts
            java.util.Map<String, Long> byStatus = filtered.stream()
                    .collect(Collectors.groupingBy(e -> (e.getDelivered()!=null && e.getDelivered()==1) ? "\u5df2\u6295\u9012" : "\u672a\u6295\u9012", Collectors.counting()));
            byStatus.forEach((k,v) -> charts.byStatus.add(new NameValue(nullSafe(k), v)));

            java.util.Map<String, Long> byCity = filtered.stream()
                    .collect(Collectors.groupingBy(e -> nullSafe(e.getJobArea()), Collectors.counting()));
            byCity.entrySet().stream().sorted((a,b)->Long.compare(b.getValue(), a.getValue())).limit(10).forEach(en -> charts.byCity.add(new NameValue(en.getKey(), en.getValue())));

            java.util.Map<String, Long> byIndustry = filtered.stream()
                    .collect(Collectors.groupingBy(e -> nullSafe(e.getCompIndustry()), Collectors.counting()));
            byIndustry.entrySet().stream().sorted((a,b)->Long.compare(b.getValue(), a.getValue())).limit(10).forEach(en -> charts.byIndustry.add(new NameValue(en.getKey(), en.getValue())));

            java.util.Map<String, Long> byCompany = filtered.stream()
                    .collect(Collectors.groupingBy(e -> nullSafe(e.getCompName()), Collectors.counting()));
            byCompany.entrySet().stream().sorted((a,b)->Long.compare(b.getValue(), a.getValue())).limit(10).forEach(en -> charts.byCompany.add(new NameValue(en.getKey(), en.getValue())));

            java.util.Map<String, Long> byExp = filtered.stream()
                    .collect(Collectors.groupingBy(e -> nullSafe(e.getJobExpReq()), Collectors.counting()));
            byExp.forEach((k,v) -> charts.byExperience.add(new NameValue(k,v)));

            java.util.Map<String, Long> byDeg = filtered.stream()
                    .collect(Collectors.groupingBy(e -> nullSafe(e.getJobEduReq()), Collectors.counting()));
            byDeg.forEach((k,v) -> charts.byDegree.add(new NameValue(k,v)));

            java.util.Map<String, Long> byDay = filtered.stream()
                    .collect(Collectors.groupingBy(e -> {
                        String t = e.getCreateTime();
                        if (t == null || t.length() < 10) return "unknown";
                        return t.substring(0,10);
                    }, Collectors.counting()));
            byDay.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(en -> charts.dailyTrend.add(new NameValue(en.getKey(), en.getValue())));

            long b0_10=0,b10_15=0,b15_20=0,b20_top=0,b_ge_top=0;
            double maxMedian = 0.0;
            for (double m : medians) { if (m > maxMedian) maxMedian = m; }
            int topEdge = (int) Math.ceil(maxMedian / 5.0) * 5; if (topEdge <= 20) topEdge = 25;
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

            resp.charts = charts;
            return resp;
        } catch (Exception e) {
            resp.charts = charts;
            return resp;
        }
    }

    /** 列表查询（分页 + 筛选 + 关键词 + 薪资区间基于中位数K） */
    public PagedResult51 listJob51(
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

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Job51Entity> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (statuses != null && !statuses.isEmpty()) {
            List<Integer> deliveredVals = new ArrayList<>();
            if (statuses.contains("\u5df2\u6295\u9012")) deliveredVals.add(1);
            if (statuses.contains("\u672a\u6295\u9012")) deliveredVals.add(0);
            if (!deliveredVals.isEmpty()) wrapper.in("delivered", deliveredVals);
        }
        if (location != null && !location.trim().isEmpty()) wrapper.eq("job_area", location.trim());
        if (experience != null && !experience.trim().isEmpty()) wrapper.eq("job_exp_req", experience.trim());
        if (degree != null && !degree.trim().isEmpty()) wrapper.eq("job_edu_req", degree.trim());
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("comp_name", kw).or().like("job_title", kw).or().like("hr_name", kw));
        }
        wrapper.orderByDesc("update_time");

        List<Job51Entity> all = job51Mapper.selectList(wrapper);

        List<Job51Entity> filtered = new ArrayList<>();
        for (Job51Entity e : all) {
            if (minK == null && maxK == null) { filtered.add(e); }
            else {
                SalaryInfo info = parse51Salary(e.getJobSalaryText());
                if (info == null || info.medianK == null) continue;
                boolean ok = true;
                if (minK != null) ok &= (info.medianK >= minK);
                if (maxK != null) ok &= (info.medianK <= maxK);
                if (ok) filtered.add(e);
            }
        }

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<Job51Entity> pageItems = from >= to ? java.util.Collections.emptyList() : filtered.subList(from, to);

        List<Job51Row> rows = new ArrayList<>();
        for (Job51Entity e : pageItems) {
            Job51Row r = new Job51Row();
            r.jobId = e.getJobId();
            r.companyName = e.getCompName();
            r.jobName = e.getJobTitle();
            r.salary = e.getJobSalaryText();
            r.location = e.getJobArea();
            r.experience = e.getJobExpReq();
            r.degree = e.getJobEduReq();
            r.hrName = e.getHrName();
            r.deliveryStatus = (e.getDelivered()!=null && e.getDelivered()==1) ? "\u5df2\u6295\u9012" : "\u672a\u6295\u9012";
            r.jobUrl = e.getJobLink();
            r.publishTime = e.getJobPublishTime();
            r.createdAt = e.getCreateTime();
            r.industry = e.getCompIndustry();
            r.companyScale = e.getCompScale();
            rows.add(r);
        }

        PagedResult51 result = new PagedResult51();
        result.items = rows;
        result.total = total;
        result.page = page;
        result.size = size;
        return result;
    }

    // ==================== 薪资解析 ====================
    private static class SalaryInfo { Double medianK; }
    private SalaryInfo parse51Salary(String salaryText) {
        if (salaryText == null) return null;
        String s = salaryText.trim().toLowerCase();
        if (s.isEmpty() || s.contains("\u9762\u8bae")) return null;
        // 识别常见区间或单值薪资格式
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\s*[-~]\s*(\\d+(?:\\.\\d+)?)").matcher(s);
        Double a = null, b = null;
        if (m.find()) {
            a = Double.valueOf(m.group(1));
            b = Double.valueOf(m.group(2));
        } else {
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(s);
            if (m2.find()) {
                a = Double.valueOf(m2.group(1)); b = a;
            }
        }
        if (a == null || b == null) return null;
        double min = Math.min(a, b), max = Math.max(a, b);
        double factorK = 1.0; // 统一换算为月薪K
        // 识别单位并计算倍率
        if (s.contains("k")) factorK = 1.0;
        else if (s.contains("\u5343") && s.contains("/\u6708")) factorK = 1.0;
        else if (s.contains("\u4e07") && s.contains("/\u6708")) factorK = 10.0;
        else if (s.contains("\u4e07") && (s.contains("/\u5e74") || s.contains("\u5e74"))) factorK = 10.0 / 12.0;
        else if (s.contains("\u5143/\u5929")) {
            // 元/天按 22 个工作日估算月薪：日薪 * 22 / 1000 -> K
            factorK = (1.0 / 1000.0) * 22.0;
        }
        double medianK = ((min + max) / 2.0) * factorK;
        SalaryInfo info = new SalaryInfo(); info.medianK = medianK; return info;
    }

    private String nullSafe(String s) { return (s == null || s.isEmpty()) ? "unknown" : s; }

    /** 重新加载 51job 数据：执行 checkpoint / VACUUM 并返回总数。 */
    public java.util.Map<String, Object> reloadJob51Data() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            try (Statement st = conn.createStatement()) {
                try { st.execute("PRAGMA wal_checkpoint(TRUNCATE)"); } catch (Exception ignore) {}
                try { st.execute("VACUUM"); } catch (Exception ignore) {}
            }
            long total = scalarCount(conn, "SELECT COUNT(*) FROM hub_job51_data");
            resp.put("success", true);
            resp.put("message", "reloaded");
            resp.put("total", total);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "重载失败: " + e.getMessage());
        } finally { try { if (conn != null) conn.close(); } catch (Exception ignore) {} }
        return resp;
    }

    private long scalarCount(Connection conn, String sql) throws Exception {
        try (Statement st = conn.createStatement(); java.sql.ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
