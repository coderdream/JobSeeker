package com.wh.jobsbackend.worker.boss;

import com.wh.jobsbackend.application.entity.AiEntity;
import com.wh.jobsbackend.application.service.AiService;
import com.wh.jobsbackend.application.service.BossService;
import com.wh.jobsbackend.worker.utils.Job;
import com.wh.jobsbackend.worker.utils.JobUtils;
import com.wh.jobsbackend.worker.utils.PlaywrightUtil;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static com.wh.jobsbackend.worker.boss.Locators.*;


/**
 * @author loks666
 * 项目链接: <a href=
 * "https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 * Boss直聘自动投递
 */
import com.wh.jobsbackend.application.service.CookieService;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class Boss {
    private static final String CODEX_BUILD_MARKER = "boss-cdp-security-id-20260719-v2";

    @Setter
    private Page page;
    @Setter
    private BossConfig config;
    @Setter
    private Long userId;

    private final BossService bossService;
    private final AiService aiService;
    private final CookieService cookieService;
    private final RestTemplate restTemplate = new RestTemplate();
    private Set<String> blackCompanies;
    private Set<String> blackRecruiters;
    private Set<String> blackJobs;
    private static final DateTimeFormatter DIAGNOSTIC_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    // 记录 encryptId -> encryptUserId 的映射，用于后续更新投递状态
    private final ConcurrentMap<String, String> encryptIdToUserId = new ConcurrentHashMap<>();
    @Setter
    private ProgressCallback progressCallback;
    @Setter
    private Supplier<Boolean> shouldStopCallback;

    private final List<Job> resultList = new ArrayList<>();

    /**
     * 进度回调接口
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void accept(String message, Integer current, Integer total);
    }

    // 通过 Lombok @RequiredArgsConstructor 使用构造器注入 bossService 与 aiService

    public void prepare() {
        // 调整 boss_data 表结构：将 encrypt_id、encrypt_user_id 前置
        try { bossService.ensureBossDataColumnOrder(); } catch (Throwable ignore) {}
        // 从数据库加载黑名单
        this.blackCompanies = bossService.getBlackCompanies();
        this.blackRecruiters = bossService.getBlackRecruiters();
        this.blackJobs = bossService.getBlackJobs();

        log.info("黑名单加载完成: 公司({}) 招聘者({}) 职位({})",
                blackCompanies != null ? blackCompanies.size() : 0,
                blackRecruiters != null ? blackRecruiters.size() : 0,
                blackJobs != null ? blackJobs.size() : 0);
        // 不在页面初始化阶段入库，仅用于后续点击卡片时按需入库
    }

    private int totalScraped = 0;

    /**
     * 执行投递
     */
    public int execute() {
        for (String cityCode : config.getCityCode()) {
            if (shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get())) {
                progressCallback.accept("用户取消投递", 0, 0);
                break;
            }
            postJobByCity(cityCode);
            if (shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get())) {
                progressCallback.accept("用户取消投递", 0, 0);
                break;
            }
        }
        return totalScraped > 0 ? totalScraped : resultList.size();
    }

    /**
     * 对数据库中已有的岗位（通过 jobUrl）直接执行投递，不做搜索抓取。
     * 供"一键投递"功能使用，jobId 为 hub_boss_data 的主键。
     *
     * @return "已投递" / "投递失败"
     */
    @SneakyThrows
    public String applyJobByUrl(Long jobId, String jobUrl, String companyName, String jobName, String salary) {
        if (jobUrl == null || jobUrl.isBlank()) {
            log.warn("applyJobByUrl: 岗位 URL 为空，跳过 | jobId={}", jobId);
            return "投递失败";
        }
        
        boolean success = false;
        try {
            // 直接使用主页面执行 JS，不再进行任何 URL navigate 或新开标签页
            // 这样能100%避免触发 Boss直聘 的跳出重定向和反爬机制
            
            Job job = new Job();
            job.setJobName(jobName);
            job.setSalary(salary);
            job.setCompanyName(companyName);
            job.setJobInfo("");

            // 注册 encryptId -> encryptUserId 映射
            String encryptId = extractEncryptId(jobUrl);
            if (encryptId != null) {
                com.wh.jobsbackend.application.entity.BossJobDataEntity entity = bossService.findById(jobId);
                if (entity != null && entity.getEncryptUserId() != null) {
                    encryptIdToUserId.put(encryptId, entity.getEncryptUserId());
                }
            }

            // 将 jobUrl 传递给 resumeSubmission 以便 JS fetch
            com.wh.jobsbackend.application.entity.BossJobDataEntity entity = bossService.findById(jobId);
            String securityId = entity != null ? entity.getSecurityId() : null;
            success = resumeSubmission(page, jobUrl, securityId, jobName, job);
            if (success) {
                bossService.updateDeliveryStatusById(jobId, "已投递");
                log.info("【投递成功】公司：{} | 岗位：{}", companyName, jobName);
                return "已投递";
            } else {
                bossService.updateDeliveryStatusById(jobId, "投递失败");
                log.warn("【投递失败】公司：{} | 岗位：{}", companyName, jobName);
                return "投递失败";
            }
        } catch (Exception e) {
            log.error("applyJobByUrl 异常 | jobId={} | jobUrl={} | error={}", jobId, jobUrl, e.getMessage(), e);
            try { bossService.updateDeliveryStatusById(jobId, "投递失败"); } catch (Exception ignored) {}
            return "投递失败";
        }
    }

    public void fetchJobDetails(List<com.wh.jobsbackend.application.entity.BossJobDataEntity> jobs) {
        int total = jobs.size();
        progressCallback.accept("开始通过Python CDP获取选定岗位详情...", 0, total);

        try {
            // 写入待抓取的列表
            java.io.File tempInputFile = java.io.File.createTempFile("boss_details_input_", ".json");
            tempInputFile.deleteOnExit();

            JSONObject inputRoot = new JSONObject();
            JSONArray inputJobsArray = new JSONArray();
            for (com.wh.jobsbackend.application.entity.BossJobDataEntity job : jobs) {
                JSONObject j = new JSONObject();
                j.put("job_id", job.getEncryptId());
                j.put("title", job.getJobName());
                j.put("boss_name", job.getCompanyName());
                j.put("job_link", job.getJobUrl());
                inputJobsArray.put(j);
            }
            inputRoot.put("jobs", inputJobsArray);
            Files.writeString(tempInputFile.toPath(), inputRoot.toString(), StandardCharsets.UTF_8);

            // 输出详情文件
            java.io.File tempOutputFile = java.io.File.createTempFile("boss_details_output_", ".json");
            tempOutputFile.deleteOnExit();

            List<String> cmd = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                cmd.add("D:\\04_GitHub\\boss-zhipin-scraper\\.venv\\Scripts\\python.exe");
                cmd.add("D:\\04_GitHub\\boss-zhipin-scraper\\scripts\\boss_cdp_raw.py");
            } else {
                cmd.add("/Volumes/System/04_GitHub/boss-zhipin-scraper/.venv/bin/python");
                cmd.add("/Volumes/System/04_GitHub/boss-zhipin-scraper/scripts/boss_cdp_raw.py");
            }
            cmd.add("--input");
            cmd.add(tempInputFile.getAbsolutePath());
            cmd.add("--detail");
            cmd.add("--detail-output");
            cmd.add(tempOutputFile.getAbsolutePath());
            cmd.add("--cdp-port");
            cmd.add("9222");

            executePythonCommand(cmd);

            // 解析输出并更新JD
            if (tempOutputFile.exists() && tempOutputFile.length() > 0) {
                String content = Files.readString(tempOutputFile.toPath(), StandardCharsets.UTF_8);
                JSONArray detailsArray = new JSONArray(content);
                int successCount = 0;
                for (int i = 0; i < detailsArray.length(); i++) {
                    JSONObject d = detailsArray.getJSONObject(i);
                    String jobLink = d.optString("job_link", "");
                    String jd = d.optString("jd", "");
                    if (jd != null && !jd.isEmpty()) {
                        // 寻找对应ID
                        for (com.wh.jobsbackend.application.entity.BossJobDataEntity job : jobs) {
                            if (jobLink.equals(job.getJobUrl())) {
                                String mockJson = constructMockDetailJson(new JSONObject().put("encryptJobId", job.getEncryptId()), jd);
                                processJobDetailJsonAndInsert(mockJson);
                                successCount++;
                                progressCallback.accept("  ✓ 获取详情成功: " + job.getJobName(), successCount, total);
                                break;
                            }
                        }
                    }
                }
                progressCallback.accept("详情获取任务完成，成功: " + successCount + "/" + total, total, total);
            } else {
                progressCallback.accept("详情获取未生成数据", 0, total);
            }
        } catch (Exception e) {
            log.error("详情抓取异常: {}", e.getMessage(), e);
            progressCallback.accept("详情抓取异常: " + e.getMessage(), 0, total);
        }
    }

    /**
     * 获取结果列表
     */
    public List<Job> getResultList() {
        return new ArrayList<>(resultList);
    }

    /**
     * 更新黑名单（从聊天记录中）
     */
    public Map<String, Set<String>> updateBlacklistFromChats() {
        page.navigate("https://www.zhipin.com/web/geek/chat");
        PlaywrightUtil.sleep(3);

        int newBlacklistCount = 0;
        boolean shouldBreak = false;
        while (!shouldBreak) {
            try {
                Locator bottomLocator = page.locator(FINISHED_TEXT);
                if (bottomLocator.count() > 0 && "没有更多了".equals(bottomLocator.textContent())) {
                    shouldBreak = true;
                }
            } catch (Exception ignore) {
            }

            Locator items = page.locator(CHAT_LIST_ITEM);
            int itemCount = items.count();

            for (int i = 0; i < itemCount; i++) {
                try {
                    Locator companyElements = page.locator(COMPANY_NAME_IN_CHAT);
                    Locator messageElements = page.locator(LAST_MESSAGE);

                    if (i >= companyElements.count() || i >= messageElements.count()) {
                        break;
                    }

                    String companyName = null;
                    String message = null;
                    int retryCount = 0;

                    while (true) {
                        try {
                            companyName = companyElements.nth(i).textContent();
                            message = messageElements.nth(i).textContent();
                            break;
                        } catch (Exception e) {
                            retryCount++;
                            if (retryCount >= 2) {
                                log.info("尝试获取元素文本2次失败，放弃本次获取");
                                break;
                            }
                            log.info("页面元素已变更，正在重试第{}次获取元素文本...", retryCount);
                            PlaywrightUtil.sleep(1);
                        }
                    }

                    if (companyName != null && message != null) {
                        boolean match = message.contains("不") || message.contains("感谢") || message.contains("但")
                                || message.contains("遗憾") || message.contains("需要本") || message.contains("对不");
                        boolean nomatch = message.contains("不是") || message.contains("不生");
                        if (match && !nomatch) {
                            if (blackCompanies.stream().anyMatch(companyName::contains)) {
                                continue;
                            }
                            companyName = companyName.replaceAll("\\.{3}", "");
                            if (companyName.matches(".*(\\p{IsHan}{2,}|[a-zA-Z]{4,}).*")) {
                                blackCompanies.add(companyName);
                                // 保存到数据库
                                bossService.addBlacklist("company", companyName);
                                newBlacklistCount++;
                                log.info("黑名单公司：【{}】，信息：【{}】", companyName, message);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("寻找黑名单公司异常...", e);
                }
            }

            try {
                Locator scrollElement = page.locator(SCROLL_LOAD_MORE);
                if (scrollElement.count() > 0) {
                    scrollElement.scrollIntoViewIfNeeded();
                } else {
                    page.evaluate("window.scrollTo(0, document.body.scrollHeight);");
                }
            } catch (Exception e) {
                log.error("滚动元素出错", e);
                break;
            }
        }
        log.info("黑名单公司数量：{}，本次新增：{}", (blackCompanies != null ? blackCompanies.size() : 0), newBlacklistCount);

        Map<String, Set<String>> result = new HashMap<>();
        result.put("blackCompanies", new HashSet<>(blackCompanies != null ? blackCompanies : Collections.emptySet()));
        result.put("blackRecruiters", new HashSet<>(blackRecruiters != null ? blackRecruiters : Collections.emptySet()));
        result.put("blackJobs", new HashSet<>(blackJobs != null ? blackJobs : Collections.emptySet()));
        return result;
    }

    private void postJobByCity(String cityCode) {
        log.info("[BOSS-BREADCRUMB] build={}, strategy=list-api-securityId, script={}",
                CODEX_BUILD_MARKER,
                "D:\\04_GitHub\\boss-zhipin-scraper\\scripts\\boss_cdp_raw.py");
        for (String keyword : config.getKeywords()) {
            if (shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get())) {
                progressCallback.accept("用户取消抓取", 0, 0);
                return;
            }

            log.info("开始使用Python CDP抓取职位列表: keyword={}, city={}", keyword, cityCode);
            progressCallback.accept("开始抓取关键词: " + keyword, 0, 0);
            try {
                // 创建临时输出文件
                java.io.File tempFile = java.io.File.createTempFile("boss_jobs_" + keyword + "_", ".json");
                tempFile.deleteOnExit();

                List<String> cmd = new ArrayList<>();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    cmd.add("D:\\04_GitHub\\boss-zhipin-scraper\\.venv\\Scripts\\python.exe");
                    cmd.add("D:\\04_GitHub\\boss-zhipin-scraper\\scripts\\boss_cdp_raw.py");
                } else {
                    cmd.add("/Volumes/System/04_GitHub/boss-zhipin-scraper/.venv/bin/python");
                    cmd.add("/Volumes/System/04_GitHub/boss-zhipin-scraper/scripts/boss_cdp_raw.py");
                }
                cmd.add("--keyword");
                cmd.add(keyword);
                cmd.add("--city");
                cmd.add(cityCode);
                cmd.add("--pages");
                cmd.add("3");
                cmd.add("--no-detail");
                cmd.add("--output");
                cmd.add(tempFile.getAbsolutePath());
                cmd.add("--cdp-port");
                cmd.add("9222");

                log.info("[BOSS-BREADCRUMB] launch args: keyword={}, city={}, pages=3, noDetail=true, allowDomFallback=false",
                        keyword, cityCode);

                // 添加其他过滤器
                if (config.getExperience() != null && !config.getExperience().isEmpty()) {
                    cmd.add("--experience");
                    cmd.add(config.getExperience().get(0));
                }
                if (config.getDegree() != null && !config.getDegree().isEmpty()) {
                    cmd.add("--degree");
                    cmd.add(config.getDegree().get(0));
                }
                if (config.getSalary() != null && !config.getSalary().isEmpty()) {
                    cmd.add("--salary");
                    cmd.add(config.getSalary().get(0));
                }
                if (config.getScale() != null && !config.getScale().isEmpty()) {
                    cmd.add("--scale");
                    cmd.add(config.getScale().get(0));
                }
                if (config.getStage() != null && !config.getStage().isEmpty()) {
                    cmd.add("--stage");
                    cmd.add(config.getStage().get(0));
                }
                if (config.getIndustry() != null && !config.getIndustry().isEmpty()) {
                    cmd.add("--industry");
                    cmd.add(config.getIndustry().get(0));
                }

                executePythonCommand(cmd);

                // 解析临时文件并入库
                if (tempFile.exists() && tempFile.length() > 0) {
                    String content = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
                    JSONObject root = new JSONObject(content);
                    if (root.has("jobs")) {
                        JSONArray jobsArray = root.getJSONArray("jobs");
                        int count = jobsArray.length();
                        log.info("Python抓取成功，共 {} 条数据，开始过滤并入库...", count);
                        int postCount = 0;
                        for (int i = 0; i < count; i++) {
                            if (shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get())) {
                                progressCallback.accept("用户取消抓取", i, count);
                                return;
                            }
                            JSONObject j = jobsArray.getJSONObject(i);
                            String jobName = j.optString("title", "");
                            String jobSalary = j.optString("salary", "");
                            String bossCompany = j.optString("boss_name", ""); // brandName
                            String companyScale = j.optString("company_scale", "");
                            String location = j.optString("location", "");
                            String tags = j.optString("tags", "");

                            // 列表级基础过滤
                            if (!jobName.isEmpty() && blackJobs != null && blackJobs.stream().anyMatch(jobName::contains)) {
                                continue;
                            }
                            if (!bossCompany.isEmpty() && blackCompanies != null && blackCompanies.stream().anyMatch(bossCompany::contains)) {
                                continue;
                            }
                            if (isSalaryBelowMinimum(jobSalary, 12)) {
                                continue;
                            }

                            // 构造 mock detail 入库
                            String encryptJobId = j.optString("encrypt_job_id", "");
                            String securityId = j.optString("security_id", j.optString("securityId", ""));
                            String encryptBossId = j.optString("encrypt_boss_id", "");
                            String encryptBrandId = j.optString("encrypt_brand_id", "");
                            String jobLink = j.optString("job_link", "");

                            // securityId 是 Boss 直投接口的必要参数。没有它的岗位无法投递，
                            // 不应保存为可投递岗位，避免后续批量投递必然失败。
                            if (securityId.isBlank()) {
                                log.warn("列表岗位缺少 securityId，跳过入库: {} | {}", bossCompany, jobName);
                                progressCallback.accept("跳过无 securityId 岗位: " + jobName, i + 1, count);
                                continue;
                            }

                            String experience = "";
                            String degree = "";
                            if (!tags.isEmpty()) {
                                String[] parts = tags.split(" \\| ");
                                if (parts.length > 0) experience = parts[0];
                                if (parts.length > 1) degree = parts[1];
                            }

                            JSONObject mockRoot = new JSONObject();
                            JSONObject mockZpData = new JSONObject();
                            JSONObject mockJobInfo = new JSONObject();
                            JSONObject mockBrand = new JSONObject();
                            JSONObject mockBoss = new JSONObject();

                            mockJobInfo.put("encryptId", encryptJobId);
                            mockJobInfo.put("encryptUserId", encryptBossId);
                            mockJobInfo.put("securityId", securityId);
                            mockJobInfo.put("jobName", jobName);
                            mockJobInfo.put("salaryDesc", jobSalary);
                            mockJobInfo.put("locationName", location);
                            mockJobInfo.put("experienceName", experience);
                            mockJobInfo.put("degreeName", degree);
                            mockJobInfo.put("jobStatusDesc", "招聘中");

                            mockBrand.put("brandName", bossCompany);
                            mockBrand.put("scaleName", companyScale);
                            mockBrand.put("industryName", j.optString("company_industry", ""));
                            mockBrand.put("stageName", j.optString("company_stage", ""));

                            mockBoss.put("encryptBossId", encryptBossId);

                            mockZpData.put("jobInfo", mockJobInfo);
                            mockZpData.put("brandComInfo", mockBrand);
                            mockZpData.put("bossInfo", mockBoss);
                            mockRoot.put("zpData", mockZpData);

                            processJobDetailJsonAndInsert(mockRoot.toString());
                            postCount++;

                            // SSE
                            String progressMsg = String.format("  ✓ %s | %s | %s | %s | %s", 
                                    jobName, jobSalary, location, bossCompany, companyScale);
                            progressCallback.accept(progressMsg, i + 1, count);
                        }
                        totalScraped += postCount;
                        progressCallback.accept("【" + keyword + "】岗位已抓取完毕！实际抓取数量:" + postCount, count, count);
                    }
                } else {
                    progressCallback.accept("Python抓取未生成数据", 0, 0);
                }
            } catch (Exception e) {
                log.error("CDP抓取异常: {}", e.getMessage(), e);
                progressCallback.accept("抓取异常: " + e.getMessage(), 0, 0);
            }
        }
    }

    private void executePythonCommand(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[Python Scraper] {}", line);
                progressCallback.accept(line, null, null);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python scraper exited with code " + exitCode);
        }
    }

    /**
     * 解析岗位详情 JSON 并进行入库与黑名单处理（只在点击卡片时调用）。
     */
    private void processJobDetailJsonAndInsert(String body) {
        if (body == null || body.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(body);
            JSONObject zpData = root.optJSONObject("zpData");
            if (zpData == null) return;

            JSONObject jobInfo = zpData.optJSONObject("jobInfo");
            JSONObject brand = zpData.optJSONObject("brandComInfo");
            JSONObject bossInfo = zpData.optJSONObject("bossInfo");
            if (jobInfo == null) return;

            String encryptId = jobInfo.optString("encryptId", null);
            String encryptUserId = jobInfo.optString("encryptUserId", null);
            if (encryptUserId == null && bossInfo != null) {
                // 兼容部分页面字段落在 bossInfo 内
                encryptUserId = bossInfo.optString("encryptUserId", null);
                if (encryptUserId == null) {
                    // 进一步兼容可能的字段命名
                    encryptUserId = bossInfo.optString("encryptBossId", null);
                }
            }
            if (encryptId != null && encryptUserId != null) {
                encryptIdToUserId.put(encryptId, encryptUserId);
            }

            com.wh.jobsbackend.application.entity.BossJobDataEntity entity = new com.wh.jobsbackend.application.entity.BossJobDataEntity();
            entity.setJobName(jobInfo.optString("jobName", null));
            entity.setSalary(jobInfo.optString("salaryDesc", null));
            entity.setLocation(jobInfo.optString("locationName", null));
            entity.setExperience(jobInfo.optString("experienceName", null));
            entity.setDegree(jobInfo.optString("degreeName", null));
            entity.setJobDescription(jobInfo.optString("postDescription", null));
            entity.setRecruitmentStatus(jobInfo.optString("jobStatusDesc", null));
            entity.setCompanyAddress(jobInfo.optString("address", null));
            entity.setEncryptId(encryptId);
            entity.setEncryptUserId(encryptUserId);
            entity.setSecurityId(jobInfo.optString("securityId", null));

            entity.setCompanyName(brand != null ? brand.optString("brandName", null) : null);
            entity.setIndustry(brand != null ? brand.optString("industryName", null) : null);
            entity.setIntroduce(brand != null ? brand.optString("introduce", null) : null);
            entity.setFinancingStage(brand != null ? brand.optString("stageName", null) : null);
            entity.setCompanyScale(brand != null ? brand.optString("scaleName", null) : null);

            entity.setHrName(bossInfo != null ? bossInfo.optString("name", null) : null);
            entity.setHrPosition(bossInfo != null ? bossInfo.optString("title", null) : null);
            entity.setHrActiveStatus(bossInfo != null ? bossInfo.optString("activeTimeDesc", null) : null);

            if (encryptId != null && !encryptId.isEmpty()) {
                entity.setJobUrl("https://www.zhipin.com/job_detail/" + encryptId + ".html");
            }

            // 黑名单处理
            boolean filtered = false;
            String companyName = entity.getCompanyName() != null ? entity.getCompanyName() : "";
            String positionName = entity.getJobName() != null ? entity.getJobName() : "";
            String hrPosition = entity.getHrPosition() != null ? entity.getHrPosition() : "";
            try {
                if (blackCompanies != null && blackCompanies.stream().anyMatch(companyName::contains)) filtered = true;
                if (!filtered && blackJobs != null && blackJobs.stream().anyMatch(positionName::contains)) filtered = true;
                if (!filtered && blackRecruiters != null && blackRecruiters.stream().anyMatch(hrPosition::contains)) filtered = true;
            } catch (Throwable ignore) {}

            // HR活跃状态过滤：开启过滤且活跃描述包含“年”，则标记为已过滤，但仍入库
            if (!filtered && Boolean.TRUE.equals(config.getFilterDeadHR())) {
                String hrActive = entity.getHrActiveStatus();
                if (hrActive != null && hrActive.contains("年")) {
                    filtered = true;
                }
            }

            entity.setDeliveryStatus(filtered ? "已过滤" : "未投递");

            // 入库（若不存在），优先以 encrypt_id + encrypt_user_id 去重；若 userId 缺失，则以 encrypt_id 去重
            if (encryptId != null) {
                try {
                    boolean exists = false;
                    if (encryptUserId != null) {
        exists = bossService.existsBossJob(encryptId, encryptUserId);
                    } else {
        exists = bossService.existsBossJobByEncryptId(encryptId);
                    }
                    if (!exists) {
                        bossService.insertBossJob(entity);
                        log.debug("岗位入库：{} | 公司：{} | HR：{} | 状态：{}", entity.getJobName(), entity.getCompanyName(), entity.getHrName(), entity.getDeliveryStatus());
                    } else if (entity.getSecurityId() != null && !entity.getSecurityId().isBlank()) {
                        // 详情抓取可能晚于列表入库，历史记录也必须回填 securityId。
                        com.wh.jobsbackend.application.entity.BossJobDataEntity existing = bossService.findByEncryptId(encryptId, encryptUserId);
                        if (existing != null && (existing.getSecurityId() == null || existing.getSecurityId().isBlank())) {
                            bossService.updateSecurityIdById(existing.getId(), entity.getSecurityId());
                            log.info("已回填岗位 securityId：{}", entity.getJobName());
                        }
                    }
                } catch (Exception e) {
                    log.warn("岗位入库失败：{}", e.getMessage());
                }
            }
        } catch (Throwable e) {
            log.debug("解析岗位详情 JSON 失败：{}", e.getMessage());
        }
    }

    public String decodeSalary(String text) {
        Map<Character, Character> fontMap = new HashMap<>();
        fontMap.put('\uE8F0', '0');
        fontMap.put('\uE8F1', '1');
        fontMap.put('\uE8F2', '2');
        fontMap.put('\uE8F3', '3');
        fontMap.put('\uE8F4', '4');
        fontMap.put('\uE8F5', '5');
        fontMap.put('\uE8F6', '6');
        fontMap.put('\uE8F7', '7');
        fontMap.put('\uE8F8', '8');
        fontMap.put('\uE8F9', '9');
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(fontMap.getOrDefault(c, c));
        }
        return result.toString();
    }

    // 安全获取单个文本内容
    public String safeText(Locator root, String selector) {
        Locator node = root.locator(selector);
        try {
            if (node.count() > 0 && node.innerText() != null) {
                return node.innerText().trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    // 安全获取多个文本内容
    public List<String> safeAllText(Locator root, String selector) {
        try {
            return root.locator(selector).allInnerTexts();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Boss姓名+活跃状态拆分
    public String[] splitBossName(String raw) {
        String[] bossParts = raw.trim().split("\\s+");
        String bossName = bossParts[0];
        String bossActive = bossParts.length > 1 ? String.join(" ", Arrays.copyOfRange(bossParts, 1, bossParts.length)) : "";
        return new String[]{bossName, bossActive};
    }

    // Boss公司+职位拆分
    public String[] splitBossTitle(String raw) {
        String[] parts = raw.trim().split(" · ");
        String company = parts[0];
        String job = parts.length > 1 ? parts[1] : "";
        return new String[]{company, job};
    }

    // 匹配命中词条（用于日志输出过滤原因）
    private String findMatchedTerm(Collection<String> patterns, String text) {
        if (patterns == null || text == null) return null;
        try {
            for (String p : patterns) {
                if (p != null && !p.isEmpty() && text.contains(p)) {
                    return p;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private boolean isSalaryBelowMinimum(String salary, int minimumK) {
        BossService.SalaryInfo info = BossService.parseSalary(salary);
        return info != null && info.minK != null && info.minK < minimumK;
    }

    private boolean hasAgeLimit(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        return normalized.matches(".*(年龄|年纪|岁数).{0,8}(35|40|45|三十五|四十|四十五).{0,8}(以内|以下|以下|以下优先|以下者|不超过|以下).*")
                || normalized.matches(".*(35|40|45|三十五|四十|四十五).{0,4}(岁|周岁).{0,8}(以内|以下|不超过|以下优先).*")
                || normalized.matches(".*(90后|95后|00后|年轻化|年龄要求).*");
    }

    public static String buildSearchUrl(BossConfig config, String cityCode) {
        String baseUrl = "https://www.zhipin.com/web/geek/jobs";
        if (config == null) {
            return baseUrl;
        }
        List<String> params = new ArrayList<>();
        addParam(params, JobUtils.appendParam("city", cityCode));
        addParam(params, JobUtils.appendParam("jobType", config.getJobType()));
        addParam(params, JobUtils.appendListParam("salary", config.getSalary()));
        addParam(params, JobUtils.appendListParam("experience", config.getExperience()));
        addParam(params, JobUtils.appendListParam("degree", config.getDegree()));
        addParam(params, JobUtils.appendListParam("scale", config.getScale()));
        addParam(params, JobUtils.appendListParam("industry", config.getIndustry()));
        addParam(params, JobUtils.appendListParam("stage", config.getStage()));
        if (params.isEmpty()) {
            return baseUrl;
        }
        return baseUrl + "?" + String.join("&", params);
    }

    private static void addParam(List<String> params, String param) {
        if (param == null || param.isEmpty()) {
            return;
        }
        params.add(param.startsWith("&") ? param.substring(1) : param);
    }

    private String getSearchUrl(String cityCode) {
        return buildSearchUrl(config, cityCode);
    }

    private void waitForBossJobList(String url) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                page.waitForSelector(BossPageModel.JOB_LIST_CONTAINER_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(60_000));
                return;
            } catch (Exception e) {
                lastError = e;
                if (!"about:blank".equalsIgnoreCase(page.url()) || attempt >= 2) {
                    break;
                }
                log.warn("Boss search page became blank; retrying navigation once: target={}", url);
                page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(60_000));
            }
        }
        if (lastError instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(lastError);
    }

    private void logBossPageDiagnostic(String reason) {
        String screenshotPath = "";
        try {
            Path diagnosticDir = Paths.get("target", "diagnostics", "boss");
            Files.createDirectories(diagnosticDir);
            Path path = diagnosticDir.resolve(reason + "-" + DIAGNOSTIC_TIME_FORMAT.format(LocalDateTime.now()) + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(path).setFullPage(true));
            screenshotPath = path.toAbsolutePath().toString();
        } catch (Exception e) {
            screenshotPath = "screenshot failed: " + e.getMessage();
        }
        log.warn("Boss page diagnostic: reason={}, url={}, title={}, screenshot={}, body={}",
                reason, page.url(), safeTitle(page), screenshotPath, compactText(safeBodyText(page)));
    }

    @SneakyThrows
    private boolean resumeSubmission(Page p, String detailUrl, String securityId, String keyword, Job job) {
        // 若收到停止指令，直接短路返回
        if (shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get())) {
            log.info("停止指令已触发，跳过投递 | 公司：{} | 岗位：{}", job.getCompanyName(), job.getJobName());
            return false;
        }
        // 调试模式：仅遍历不投递
        if (Boolean.TRUE.equals(config.getDebugger())) {
            log.info("调试模式：仅遍历岗位，不投递 | 公司：{} | 岗位：{}", job.getCompanyName(), job.getJobName());
            return false;
        }

        String encryptId = extractEncryptId(detailUrl);
        String encryptUserId = encryptId != null ? encryptIdToUserId.get(encryptId) : null;

        log.info("准备通过纯 JS fetch 投递岗位 | 公司：{} | 岗位：{}", job.getCompanyName(), job.getJobName());

        boolean sendSuccess = false;
        try {
            // 确保当前页面在 zhipin.com 域名下，否则 fetch 会报 CORS 或 Failed to fetch
            if (!p.url().contains("zhipin.com")) {
                log.info("当前页面不在 zhipin.com ({})，导航至主页以绕过跨域限制...", p.url());
                p.navigate("https://www.zhipin.com/web/geek/job", new Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(30_000));
                PlaywrightUtil.sleep(3);
            }

            // 通过注入隐藏 iframe 来加载详情页，绕过简单的 fetch 拦截，直接提取 __INITIAL_STATE__
            if (securityId == null || securityId.isBlank()) {
                log.warn("岗位缺少 securityId，跳过投递: {}", job.getJobName());
                return false;
            }
            String directJs = "(args) => fetch('/wapi/zpgeek/friend/add.json?securityId=' + encodeURIComponent(args.securityId) + '&jobId=' + encodeURIComponent(args.jobId) + '&lid=', {" +
                    "method:'POST',headers:{'Accept':'application/json, text/plain, */*','Content-Type':'application/x-www-form-urlencoded','x-requested-with':'XMLHttpRequest'},body:'sessionId='" +
                    "}).then(r => r.json())";
            Object directResult = p.evaluate(directJs, java.util.Map.of("jobId", encryptId != null ? encryptId : "", "securityId", securityId));
            if (directResult instanceof java.util.Map) {
                Object code = ((java.util.Map<?, ?>) directResult).get("code");
                if (code instanceof Number && ((Number) code).intValue() == 0) {
                    sendSuccess = true;
                } else {
                    log.warn("列表 securityId 投递失败，接口返回: {}", directResult);
                }
            }
            if (sendSuccess) {
                log.info("纯 JS fetch 投递成功！");
                return true;
            }

            String js = "(args) => {\n" +
                "  return new Promise((resolve, reject) => {\n" +
                "      const iframe = document.createElement('iframe');\n" +
                "      iframe.style.display = 'none';\n" +
                "      iframe.src = args.detailUrl;\n" +
                "      document.body.appendChild(iframe);\n" +
                "      \n" +
                "      let attempts = 0;\n" +
                "      const timer = setInterval(() => {\n" +
                "          attempts++;\n" +
                "          try {\n" +
                "              const win = iframe.contentWindow;\n" +
                "              if (win && win.__INITIAL_STATE__ && win.__INITIAL_STATE__.jobInfo && win.__INITIAL_STATE__.jobInfo.securityId) {\n" +
                "                  clearInterval(timer);\n" +
                "                  const securityId = win.__INITIAL_STATE__.jobInfo.securityId;\n" +
                "                  fetch('https://www.zhipin.com/wapi/zpgeek/friend/add.json?securityId=' + securityId + '&jobId=' + args.jobId + '&lid=', {\n" +
                "                      method: 'POST',\n" +
                "                      headers: {\n" +
                "                          'Accept': 'application/json, text/plain, */*',\n" +
                "                          'Content-Type': 'application/x-www-form-urlencoded',\n" +
                "                          'x-requested-with': 'XMLHttpRequest'\n" +
                "                      },\n" +
                "                      body: 'sessionId='\n" +
                "                  }).then(r => r.json()).then(data => {\n" +
                "                      document.body.removeChild(iframe);\n" +
                "                      resolve(data);\n" +
                "                  }).catch(e => {\n" +
                "                      document.body.removeChild(iframe);\n" +
                "                      resolve({ok: false, error: '投递接口失败: ' + e.toString()});\n" +
                "                  });\n" +
                "              }\n" +
                "          } catch (e) {\n" +
                "              // 忽略跨域等报错，继续轮询\n" +
                "          }\n" +
                "          \n" +
                "          if (attempts > 150) {\n" +
                "              clearInterval(timer);\n" +
                "              document.body.removeChild(iframe);\n" +
                "              resolve({ok: false, error: '获取 securityId 超时 (15秒)，可能是遇到验证码拦截'});\n" +
                "          }\n" +
                "      }, 100);\n" +
                "  });\n" +
                "}";

            Object result = p.evaluate(js, java.util.Map.of("jobId", encryptId != null ? encryptId : "", "detailUrl", detailUrl));
            
            if (result instanceof java.util.Map) {
                java.util.Map<?, ?> response = (java.util.Map<?, ?>) result;
                if (response.containsKey("code")) {
                    Object code = response.get("code");
                    if (code instanceof Integer && ((Integer) code) == 0) {
                        sendSuccess = true;
                        log.info("纯 JS fetch 投递成功 | code=0");
                    } else {
                        log.warn("纯 JS fetch 投递失败 | response: {}", response);
                    }
                } else {
                    log.warn("纯 JS fetch 返回未知格式 | response: {}", response);
                }
            }
        } catch (Exception e) {
            log.error("执行原生 fetch 投递时发生异常: {}", e.getMessage(), e);
        }

        log.info("投递完成 | 公司：{} | 岗位：{} | 薪资：{} | 投递结果：{}", job.getCompanyName(), job.getJobName(), job.getSalary(), sendSuccess ? "成功" : "失败");

        PlaywrightUtil.sleep(1);

        // 10. 更新数据库投递状态 & 成功投递加入结果
        if (sendSuccess) {
            if (encryptId != null && encryptUserId != null) {
                try {
                    bossService.updateDeliveryStatus(encryptId, encryptUserId, "已投递");
                    log.info("更新投递状态成功 | 公司：{} | 岗位：{} | encryptId：{} | encryptUserId：{}", job.getCompanyName(), job.getJobName(), encryptId, encryptUserId);
                } catch (Exception e) {
                    log.warn("更新投递状态为已投递失败：{}", e.getMessage());
                }
            } else {
                log.debug("未能找到 encryptId/encryptUserId 用于更新投递状态，detailUrl: {}", detailUrl);
            }
            resultList.add(job);
        } else {
            if (encryptId != null && encryptUserId != null) {
                try {
                    bossService.updateDeliveryStatus(encryptId, encryptUserId, "投递失败");
                    log.warn("更新投递状态为失败 | 公司：{} | 岗位：{} | encryptId：{} | encryptUserId：{}", job.getCompanyName(), job.getJobName(), encryptId, encryptUserId);
                } catch (Exception e) {
                    log.warn("更新投递状态为投递失败异常：{}", e.getMessage());
                }
            }
        }
        return sendSuccess;
    }

    private boolean waitForBossDeliveryConfirmation(Page targetPage) {
        for (int i = 0; i < 5; i++) {
            String text = safeBodyText(targetPage);
            if (BossPageModel.isConfirmedDeliveryText(text)) {
                return true;
            }
            if (BossPageModel.isBlockingText(text)) {
                failPageModel("Boss投递后出现阻塞提示: " + compactText(text), null);
            }
            try {
                Locator confirm = targetPage.locator(BossPageModel.SUCCESS_CONFIRM_SELECTOR);
                if (confirm.count() > 0) {
                    List<String> texts = confirm.allInnerTexts();
                    for (String item : texts) {
                        if (BossPageModel.isConfirmedDeliveryText(item)) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            PlaywrightUtil.sleep(1);
        }
        failPageModel("Boss点击发送后未检测到平台成功确认", null);
        return false;
    }

    private void closeAnyBlockingOverlays(Page targetPage) {
        try {
            Locator closeButtons = targetPage.locator(BossPageModel.CLOSE_BUTTON_SELECTOR);
            for (int i = 0; i < Math.min(closeButtons.count(), 3); i++) {
                try {
                    if (closeButtons.nth(i).isVisible()) {
                        closeButtons.nth(i).click(new Locator.ClickOptions().setForce(true).setTimeout(1000));
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void failPageModel(String message, Throwable cause) {
        log.error(message, cause);
        if (progressCallback != null) {
            progressCallback.accept(message, null, null);
        }
        if (cause == null) {
            throw new PlatformPageModelException(message);
        }
        throw new PlatformPageModelException(message, cause);
    }

    private String safeBodyText(Page targetPage) {
        try {
            Object text = targetPage.evaluate("() => document.body ? document.body.innerText : ''");
            return text == null ? "" : String.valueOf(text);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeTitle(Page targetPage) {
        try {
            return targetPage.title();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String compactText(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120);
    }

    

    /**
     * 注册页面响应监听：拦截 /wapi/zpgeek/job/detail.json 请求并解析写库
     */
    private void attachJobDetailResponseListener() {
        if (page == null) return;
        page.onResponse(resp -> {
            try {
                String url = resp.url();
                if (url == null) return;
                // 仅处理 Boss 岗位详情接口（GET）
                if (url.contains("/wapi/zpgeek/job/detail.json") &&
                        "GET".equalsIgnoreCase(resp.request().method())) {
                    String body = null;
                    try {
                        body = resp.text();
                    } catch (Throwable ignore) {
                        // 某些情况下可能拿不到文本，忽略
                    }
                    if (body == null || body.isEmpty()) return;

                    // 保存原始 JSON 到 target/job.txt
                    appendRawJson(body);

                    // 仅记录映射与原始 JSON；入库逻辑已移动到点击卡片时
                    JSONObject root = new JSONObject(body);
                    JSONObject zpData = root.optJSONObject("zpData");
                    if (zpData == null) return;
                    JSONObject jobInfo = zpData.optJSONObject("jobInfo");
                    if (jobInfo == null) return;
                    String encryptId = jobInfo.optString("encryptId", null);
                    String encryptUserId = jobInfo.optString("encryptUserId", null);
                    if (encryptId != null && encryptUserId != null) {
                        encryptIdToUserId.put(encryptId, encryptUserId);
                    }
                }
            } catch (Throwable e) {
                log.debug("监听岗位详情响应处理异常：{}", e.getMessage());
            }
        });
    }


    /**
     * 追加保存原始 JSON 到 target/job.txt
     */
    private void appendRawJson(String body) {
        try {
            java.io.File dir = new java.io.File("target");
            if (!dir.exists()) dir.mkdirs();
            java.io.File file = new java.io.File(dir, "job.txt");
            try (java.io.FileWriter fw = new java.io.FileWriter(file, true)) {
                fw.write(body);
                fw.write(System.lineSeparator());
                fw.write("\n");
            }
        } catch (Exception e) {
            log.debug("写入 target/job.txt 失败：{}", e.getMessage());
        }
    }

    /**
     * 从详情页 URL 中提取 encrypt_id
     */
    private String extractEncryptId(String detailUrl) {
        try {
            if (detailUrl == null) return null;
            String key = "/job_detail/";
            int idx = detailUrl.indexOf(key);
            if (idx < 0) return null;
            int start = idx + key.length();
            int end = detailUrl.indexOf(".html", start);
            if (end < 0) end = detailUrl.length();
            return detailUrl.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValidString(String str) {
        return str != null && !str.isEmpty();
    }

    private boolean sendImageResume(Page page) {
        try {
            // 0) 资源存在性校验，避免后续无效操作
            URL resourceUrlCheck = Boss.class.getResource("/resume.jpg");
            if (resourceUrlCheck == null) {
                log.error("资源文件 resume.jpg 不存在，跳过发送图片简历");
                return false;
            }

            // 进入聊天页
            if (!page.url().contains("/web/geek/chat")) {
                Locator chatBtn = page.locator(BossPageModel.CHAT_BUTTON_SELECTOR);
                if (chatBtn.count() == 0) {
                    log.warn("未找到【继续沟通/立即沟通】按钮，跳过发送图片简历");
                    return false;
                }
                chatBtn.first().click();
                page.waitForURL("**/web/geek/chat**", new Page.WaitForURLOptions().setTimeout(15_000));
            }

            // 1) 解析图片路径（在可能触发文件选择器前就准备好）
            java.nio.file.Path imagePath = resolveResumeImage();

            // 精准定位聊天工具栏内的图片输入，避免匹配到页面其他上传控件
            Locator imgContainer = page.locator("div.btn-sendimg[aria-label='发送图片'], div[aria-label='发送图片'].btn-sendimg");
            Locator imageInput = imgContainer.locator("input[type='file'][accept*='image']").first();
            if (imageInput.count() == 0) {
                // 若未渲染，尝试拦截系统文件选择器；若未出现则普通点击促使 input 出现
                if (imgContainer.count() > 0) {
                    boolean chooserHandled = false;
                    try {
                        com.microsoft.playwright.FileChooser chooser = page.waitForFileChooser(() -> {
                            imgContainer.first().click();
                        });
                        chooser.setFiles(imagePath);
                        chooserHandled = true;
                        log.info("通过 FileChooser 直接提交图片文件，避免系统窗口阻塞");
                    } catch (com.microsoft.playwright.PlaywrightException ignore) {
                        // 未弹出系统文件选择器，继续常规流程
                    }
                    if (!chooserHandled) {
                        PlaywrightUtil.sleep(1);
                        imageInput = imgContainer.locator("input[type='file'][accept*='image']").first();
                    }
                }
            }
            imageInput.waitFor(new Locator.WaitForOptions().setTimeout(10_000));

            // 上传图片
            imageInput.setInputFiles(imagePath);
            PlaywrightUtil.sleep(1);
            return true;
        } catch (Throwable e) {
            log.error("发送图片简历失败：{}", e.getMessage(), e);
            return false;
        }
    }

    private java.nio.file.Path resolveResumeImage() throws Exception {
        URL resourceUrl = Boss.class.getResource("/resume.jpg");
        if (resourceUrl == null) {
            throw new IllegalStateException("资源文件 /resume.jpg 未找到，请将图片放置到 src/main/resources 目录下");
        }
        if ("file".equalsIgnoreCase(resourceUrl.getProtocol())) {
            return java.nio.file.Paths.get(resourceUrl.toURI());
        }
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("resume-", ".jpg");
        try (java.io.InputStream in = Boss.class.getResourceAsStream("/resume.jpg")) {
            if (in == null) {
                throw new IllegalStateException("无法从类路径读取 /resume.jpg 资源");
            }
            java.nio.file.Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    /**
     * 检查岗位薪资是否符合预期
     *
     * @return boolean
     * true 不符合预期
     * false 符合预期
     * 期望的最低薪资如果比岗位最高薪资还小，则不符合（薪资给的太少）
     * 期望的最高薪资如果比岗位最低薪资还小，则不符合(要求太高满足不了)
     */
    private boolean isSalaryNotExpected(String salary) {
        try {
            // 1. 如果没有期望薪资范围，直接返回 false，表示"薪资并非不符合预期"
            List<Integer> expectedSalary = config.getExpectedSalary();
            if (!hasExpectedSalary(expectedSalary)) {
                return false;
            }

            // 2. 清理薪资文本（比如去掉 "·15薪"）
            salary = removeYearBonusText(salary);

            // 3. 如果薪资格式不符合预期（如缺少 "K" / "k"），直接返回 true，表示"薪资不符合预期"
            if (!isSalaryInExpectedFormat(salary)) {
                return true;
            }

            // 4. 进一步清理薪资文本，比如去除 "K"、"k"、"·" 等
            salary = cleanSalaryText(salary);

            // 5. 判断是 "月薪" 还是 "日薪"
            String jobType = detectJobType(salary);
            salary = removeDayUnitIfNeeded(salary); // 如果是按天，则去除 "元/天"

            // 6. 解析薪资范围并检查是否超出预期
            Integer[] jobSalaryRange = parseSalaryRange(salary);
            return isSalaryOutOfRange(jobSalaryRange,
                    getMinimumSalary(expectedSalary),
                    getMaximumSalary(expectedSalary),
                    jobType);

        } catch (Exception e) {
            log.error("岗位薪资获取异常！薪资文本【{}】,异常信息【{}】", salary, e.getMessage(), e);
            // 出错时，您可根据业务需求决定返回 true 或 false
            // 这里假设出错时无法判断，视为不满足预期 => 返回 true
            return true;
        }
    }

    /**
     * 是否存在有效的期望薪资范围
     */
    private boolean hasExpectedSalary(List<Integer> expectedSalary) {
        return expectedSalary != null && !expectedSalary.isEmpty();
    }

    /**
     * 去掉年终奖信息，如 "·15薪"、"·13薪"。
     */
    private String removeYearBonusText(String salary) {
        if (salary.contains("薪")) {
            // 使用正则去除 "·任意数字薪"
            return salary.replaceAll("·\\d+薪", "");
        }
        return salary;
    }

    /**
     * 判断是否是按天计薪，如发现 "元/天" 则认为是日薪
     */
    private String detectJobType(String salary) {
        if (salary.contains("元/天")) {
            return "day";
        }
        return "mouth";
    }

    /**
     * 如果是日薪，则去除 "元/天"
     */
    private String removeDayUnitIfNeeded(String salary) {
        if (salary.contains("元/天")) {
            return salary.replaceAll("元/天", "");
        }
        return salary;
    }

    private Integer getMinimumSalary(List<Integer> expectedSalary) {
        return expectedSalary != null && !expectedSalary.isEmpty() ? expectedSalary.get(0) : null;
    }

    private Integer getMaximumSalary(List<Integer> expectedSalary) {
        return expectedSalary != null && expectedSalary.size() > 1 ? expectedSalary.get(1) : null;
    }

    private boolean isSalaryInExpectedFormat(String salaryText) {
        return salaryText.contains("K") || salaryText.contains("k") || salaryText.contains("元/天");
    }

    private String cleanSalaryText(String salaryText) {
        salaryText = salaryText.replace("K", "").replace("k", "");
        int dotIndex = salaryText.indexOf('·');
        if (dotIndex != -1) {
            salaryText = salaryText.substring(0, dotIndex);
        }
        return salaryText;
    }

    private boolean isSalaryOutOfRange(Integer[] jobSalary, Integer miniSalary, Integer maxSalary,
                                       String jobType) {
        if (jobSalary == null) {
            return true;
        }
        if (miniSalary == null) {
            return false;
        }
        if (Objects.equals("day", jobType)) {
            // 期望薪资转为平均每日的工资
            maxSalary = BigDecimal.valueOf(maxSalary).multiply(BigDecimal.valueOf(1000))
                    .divide(BigDecimal.valueOf(21.75), 0, RoundingMode.HALF_UP).intValue();
            miniSalary = BigDecimal.valueOf(miniSalary).multiply(BigDecimal.valueOf(1000))
                    .divide(BigDecimal.valueOf(21.75), 0, RoundingMode.HALF_UP).intValue();
        }
        // 如果职位薪资下限低于期望的最低薪资，返回不符合
        if (jobSalary[1] < miniSalary) {
            return true;
        }
        // 如果职位薪资上限高于期望的最高薪资，返回不符合
        return maxSalary != null && jobSalary[0] > maxSalary;
    }

    public boolean containsDeadStatus(String activeTimeText, List<String> deadStatus) {
        for (String status : deadStatus) {
            if (activeTimeText.contains(status)) {
                return true;// 一旦找到包含的值，立即返回 true
            }
        }
        return false;// 如果没有找到，返回 false
    }

    private String generateAiMessage(String keyword, String jobName, String jd) {
        AiEntity aiConfig = aiService.getAiConfig();
        String introduce = (aiConfig != null && aiConfig.getIntroduce() != null) ? aiConfig.getIntroduce() : "";
        String prompt = (aiConfig != null) ? aiConfig.getPrompt() : null;

        String requestMessage = (prompt != null)
                ? String.format(prompt, introduce, keyword, jobName, jd, config.getSayHi())
                : buildDefaultPrompt(introduce, keyword, jobName, jd);

        try {
            String result = aiService.sendRequest(requestMessage);
            if (result == null) {
                return config.getSayHi();
            }
            return result.toLowerCase().contains("false") ? config.getSayHi() : result;
        } catch (Exception e) {
            log.warn("AI请求失败，使用原有打招呼语: {}", e.getMessage());
            return config.getSayHi();
        }
    }

    private String buildDefaultPrompt(String introduce, String keyword, String jobName, String jd) {
        return "请基于以下信息生成简洁友好的中文打招呼语，不超过60字：\n" +
                "个人介绍：" + introduce + "\n" +
                "关键词：" + keyword + "\n" +
                "职位名称：" + jobName + "\n" +
                "职位描述：" + jd + "\n" +
                "参考语：" + config.getSayHi();
    }

    private Integer[] parseSalaryRange(String salaryText) {
        try {
            return Arrays.stream(salaryText.split("-")).map(s -> s.replaceAll("[^0-9]", "")) // 去除非数字字符
                    .map(Integer::parseInt) // 转换为Integer
                    .toArray(Integer[]::new); // 转换为Integer数组
        } catch (Exception e) {
            log.error("薪资解析异常！{}", e.getMessage(), e);
        }
        return null;
    }

    private void waitForSliderVerify(Page page) {
        String SLIDER_URL = "https://www.zhipin.com/web/user/safe/verify-slider";
        // 最多等待5分钟（防呆，防止死循环）
        long start = System.currentTimeMillis();
        while (true) {
            String url = page.url();
            if (url != null && url.startsWith(SLIDER_URL)) {
                progressCallback.accept("请手动完成Boss直聘滑块验证，通过后在控制台回车继续...", 0, 0);
                System.out.println("\n【滑块验证】请手动完成Boss直聘滑块验证，通过后在控制台回车继续…");
                try {
                    System.in.read();
                } catch (Exception e) {
                    log.error("等待滑块验证输入异常: {}", e.getMessage());
                }
                PlaywrightUtil.sleep(1);
                // 验证通过后页面url会变，循环再检测一次
                continue;
            }
            if ((System.currentTimeMillis() - start) > 5 * 60 * 1000) {
                throw new RuntimeException("滑块验证超时！");
            }
            break;
        }
    }


    private boolean isLoginRequired() {
        try {
            Locator buttonLocator = page.locator(LOGIN_BTNS);
            if (buttonLocator.count() > 0 && buttonLocator.textContent().contains("登录")) {
                return true;
            }
        } catch (Exception e) {
            try {
                page.locator(PAGE_HEADER).waitFor();
                Locator errorLoginLocator = page.locator(ERROR_PAGE_LOGIN);
                if (errorLoginLocator.count() > 0) {
                    errorLoginLocator.click();
                }
                return true;
            } catch (Exception ex) {
                log.info("没有出现403访问异常");
            }
            log.info("cookie有效，已登录...");
            return false;
        }
        return false;
    }

    private void nativeClick(Page targetPage, Locator locator) {
        try {
            locator.scrollIntoViewIfNeeded();
            locator.click(new Locator.ClickOptions().setTimeout(2000));
        } catch (Exception e) {
            try {
                locator.evaluate("el => el.click()");
            } catch (Exception ex) {
                log.warn("nativeClick failed: {}", ex.getMessage());
            }
        }
    }

    private String buildApiUrl(String searchUrl, String keyword, int pageNum) {
        String queryPart = searchUrl.contains("?") ? searchUrl.substring(searchUrl.indexOf("?") + 1) : "";
        List<String> params = new ArrayList<>();
        if (!queryPart.isEmpty()) {
            params.add(queryPart);
        }
        params.add("scene=1");
        try {
            params.add("query=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        } catch (Exception e) {
            params.add("query=" + keyword);
        }
        params.add("page=" + pageNum);
        params.add("pageSize=30");
        return "https://www.zhipin.com/wapi/zpgeek/search/joblist.json?" + String.join("&", params);
    }

    private String doHttpGet(String url) {
        com.wh.jobsbackend.application.entity.CookieEntity cookie = cookieService.getCookieByPlatform(userId, "boss");
        if (cookie == null || cookie.getCookieValue() == null) {
            throw new RuntimeException("未能获取到Boss直聘的Cookie，请重新登录");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", cookie.getCookieValue());
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private String fetchJobListJson(String apiUrl) {
        try {
            return doHttpGet(apiUrl);
        } catch (Exception e) {
            log.error("Http get fetchJobListJson failed", e);
            return null;
        }
    }

    private String fetchJobDetailJsonOrHtml(Page detailPage, String detailUrl) {
        final String[] detailJsonHolder = new String[1];
        detailPage.onResponse(resp -> {
            try {
                String url = resp.url();
                if (url != null && url.contains("/wapi/zpgeek/job/detail.json") && "GET".equalsIgnoreCase(resp.request().method())) {
                    detailJsonHolder[0] = resp.text();
                }
            } catch (Exception ignore) {}
        });

        try {
            detailPage.navigate(detailUrl, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(30000));
        } catch (Exception e) {
            log.warn("Navigate to detail page failed: {}", e.getMessage());
        }

        // 检测/处理详情页的滑块验证
        waitForSliderVerify(detailPage);

        // 等待最多 5 秒让 JSON 数据返回
        for (int i = 0; i < 50; i++) {
            if (detailJsonHolder[0] != null) {
                break;
            }
            PlaywrightUtil.sleepMillis(100);
        }

        // 如果未拦截到接口，回退到 DOM 提取
        if (detailJsonHolder[0] == null) {
            log.info("未拦截到详情接口，尝试从 DOM 提取 JD...");
            try {
                Object jdObj = detailPage.evaluate("() => {\n" +
                    "  var el = document.querySelector('.job-sec .text, .job-detail-section .text, .job-sec-text');\n" +
                    "  return el ? el.innerText : '';\n" +
                    "}");
                if (jdObj != null && !jdObj.toString().isBlank()) {
                    log.info("DOM 提取 JD 成功，长度: {}", jdObj.toString().length());
                    return constructMockDetailJson(new JSONObject(), jdObj.toString());
                }
            } catch (Exception e) {
                log.warn("DOM 提取 JD 失败: {}", e.getMessage());
            }
        }

        return detailJsonHolder[0];
    }

    private String constructMockDetailJson(JSONObject listItem, String postDescription) {
        JSONObject root = new JSONObject();
        JSONObject zpData = new JSONObject();
        JSONObject jobInfo = new JSONObject();
        JSONObject brandComInfo = new JSONObject();
        JSONObject bossInfo = new JSONObject();

        jobInfo.put("encryptId", listItem.optString("encryptJobId", ""));
        jobInfo.put("encryptUserId", listItem.optString("encryptBossId", listItem.optString("bossId", "")));
        jobInfo.put("jobName", listItem.optString("jobName", ""));
        jobInfo.put("salaryDesc", listItem.optString("salaryDesc", ""));
        jobInfo.put("locationName", listItem.optString("cityName", ""));
        jobInfo.put("experienceName", listItem.optString("jobExperience", ""));
        jobInfo.put("degreeName", listItem.optString("jobDegree", ""));
        jobInfo.put("postDescription", postDescription != null ? postDescription : "");

        brandComInfo.put("brandName", listItem.optString("brandName", ""));
        brandComInfo.put("scaleName", listItem.optString("brandScaleName", ""));
        brandComInfo.put("stageName", listItem.optString("brandStageName", ""));
        brandComInfo.put("industryName", listItem.optString("brandIndustry", ""));

        bossInfo.put("name", listItem.optString("bossName", ""));
        bossInfo.put("title", listItem.optString("bossTitle", ""));
        if (listItem.optBoolean("bossOnline", false)) {
            bossInfo.put("activeTimeDesc", "刚刚在线");
        } else {
            bossInfo.put("activeTimeDesc", "");
        }

        zpData.put("jobInfo", jobInfo);
        zpData.put("brandComInfo", brandComInfo);
        zpData.put("bossInfo", bossInfo);
        root.put("zpData", zpData);

        return root.toString();
    }

}
