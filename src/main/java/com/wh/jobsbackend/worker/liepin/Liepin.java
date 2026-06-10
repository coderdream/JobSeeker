package com.wh.jobsbackend.worker.liepin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.jobsbackend.application.entity.LiepinEntity;
import com.wh.jobsbackend.application.service.LiepinService;
import com.wh.jobsbackend.worker.utils.PlaywrightUtil;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static com.wh.jobsbackend.worker.liepin.Locators.*;


/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Slf4j
@Component
@Scope("prototype")
public class Liepin {
    static {
        // 在类加载时就设置日志文件名，确保Logger初始化时能获取到正确的属性
        System.setProperty("log.name", "liepin");
    }

    private int maxPage = 50;
    private final List<String> resultList = new ArrayList<>();
    private final List<LiepinEntity> lastApiEntities = new ArrayList<>();
    @Setter
    private LiepinConfig config;
    @Getter
    private Date startDate;
    @Setter
    private Page page;
    @Autowired
    private LiepinService liepinService;

    public interface ProgressCallback {
        void onProgress(String message, Integer current, Integer total);
    }

    @Setter
    private ProgressCallback progressCallback;
    @Setter
    private Supplier<Boolean> shouldStopCallback;

    public void prepare() {
        this.startDate = new Date();
        this.resultList.clear();
        this.lastApiEntities.clear();
    }

    public int execute() {
        if (page == null) {
            throw new IllegalStateException("Liepin.page 未设置");
        }
        if (config == null) {
            throw new IllegalStateException("Liepin.config 未设置");
        }

        // 在开始执行前确保已注册接口监听
        prepare();

        List<String> keywords = config.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            log.warn("未配置关键词，执行结束");
            return 0;
        }

        for (String keyword : keywords) {
            if (shouldStop()) {
                info("收到停止指令，提前结束关键词循环");
                break;
            }
            submit(keyword);
        }
        return resultList.size();
    }

    // ========== 解析接口JSON并保存到数据库 ==========
    private void captureSearchResponse(Runnable action) {
        lastApiEntities.clear();
        RuntimeException[] actionFailure = new RuntimeException[1];
        try {
            Response response = page.waitForResponse(
                    this::isLiepinSearchResponse,
                    new Page.WaitForResponseOptions().setTimeout(15000),
                    () -> {
                        try {
                            action.run();
                        } catch (RuntimeException e) {
                            actionFailure[0] = e;
                            throw e;
                        }
                    });
            parseLiepinSearchResponse(response);
        } catch (RuntimeException e) {
            if (actionFailure[0] != null) {
                throw actionFailure[0];
            }
            log.warn("[liepin] search API response not captured: {}", e.getMessage());
        }
    }

    private boolean isLiepinSearchResponse(Response response) {
        try {
            String url = response.url();
            return url != null
                    && response.status() == 200
                    && url.contains("com.liepin.searchfront4c.pc-search-job")
                    && !url.contains("com.liepin.searchfront4c.pc-search-job-cond-init");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void parseLiepinSearchResponse(Response response) {
        try {
            if (response == null) {
                return;
            }
            log.info("[liepin][response] {}", response.url());
            String contentType = null;
            try {
                contentType = response.headers().get("content-type");
            } catch (Exception ignored) {
            }
            if (contentType == null || contentType.contains("json")) {
                String text = response.text();
                if (text != null && !text.isEmpty()) {
                    parseAndPersistLiepinData(text);
                }
            }
        } catch (Exception e) {
            log.warn("[liepin] parse search response failed: {}", e.getMessage());
        }
    }

    private void parseAndPersistLiepinData(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            // 兼容两种结构：data.data.jobCardList 或 data.jobCardList
            JsonNode cardList = root.path("data").path("data").path("jobCardList");
            if (!cardList.isArray()) {
                cardList = root.path("data").path("jobCardList");
            }
            if (!cardList.isArray()) {
                return;
            }
            lastApiEntities.clear();
            for (JsonNode item : cardList) {
                JsonNode job = item.path("job");
                JsonNode comp = item.path("comp");
                JsonNode recruiter = item.path("recruiter");

                Long jobId = readLong(job.path("jobId"));
                if (jobId == null) {
                    continue;
                }

                LiepinEntity entity = new LiepinEntity();
                entity.setJobId(jobId);
                entity.setJobTitle(readText(job.path("title")));
                entity.setJobLink(readText(job.path("link")));
                entity.setJobSalaryText(readText(job.path("salary")));
                entity.setJobArea(readText(job.path("dq")));
                entity.setJobEduReq(readText(job.path("requireEduLevel")));
                entity.setJobExpReq(readText(job.path("requireWorkYears")));
                entity.setJobPublishTime(readText(job.path("refreshTime")));

                entity.setCompId(readLong(comp.path("compId")));
                entity.setCompName(readText(comp.path("compName")));
                entity.setCompIndustry(readText(comp.path("compIndustry")));
                entity.setCompScale(readText(comp.path("compScale")));

                entity.setHrId(readText(recruiter.path("recruiterId")));
                entity.setHrName(readText(recruiter.path("recruiterName")));
                entity.setHrTitle(readText(recruiter.path("recruiterTitle")));
                entity.setHrImId(readText(recruiter.path("imId")));

                // 缓存到内存供页面投递显示使用（避免从页面读取文本）
                lastApiEntities.add(entity);
            }
            // 批量持久化：仅不存在时插入，默认 delivered=0
            try {
                liepinService.insertSnapshotsIfNotExistsBatch(lastApiEntities);
            } catch (Exception e) {
                log.warn("批量保存猎聘岗位数据失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("解析猎聘JSON失败: {}", e.getMessage());
        }
    }

    private String readText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String v = node.asText();
        return (v == null || v.isEmpty()) ? null : v;
    }

    private Long readLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try {
            if (node.isNumber()) {
                long v = node.asLong();
                return v == 0 ? null : v;
            }
            if (node.isTextual()) {
                String t = node.asText();
                if (t == null || t.isEmpty()) return null;
                long v = Long.parseLong(t.trim());
                return v == 0 ? null : v;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String safeText(String s) {
        if (s == null) return null;
        return s.replaceAll("\n", " ").replaceAll("【 ", "[").replaceAll(" 】", "]");
    }

    private boolean shouldStop() {
        return shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get());
    }

    private void info(String msg) {
        if (progressCallback != null) {
            progressCallback.onProgress(msg, null, null);
        } else {
            log.info(msg);
        }
    }

    private void submit(String keyword) {
        // 清洗关键词：去掉前后引号与多余空白
        String cleanKeyword = keyword == null ? "" : keyword.replace("\"", "").trim();
        String searchUrl = buildSearchUrl(cleanKeyword);
        captureSearchResponse(() -> page.navigate(searchUrl));
        
        // 等待分页元素加载
        try {
            page.waitForSelector(LiepinPageModel.PAGINATION_BOX_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(10000));
        } catch (RuntimeException e) {
            throw new PlatformPageModelException("猎聘搜索结果页未加载分页元素，可能未登录、被风控、无结果或页面结构变化。"
                    + " keyword=" + cleanKeyword
                    + ", url=" + safePageUrl()
                    + ", title=" + safePageTitle(), e);
        }
        Locator paginationBox = page.locator(LiepinPageModel.PAGINATION_BOX_SELECTOR);
        Locator lis = paginationBox.locator("li");
        setMaxPage(lis);
        
        for (int i = 0; i < maxPage; i++) {
            if (shouldStop()) {
                info("收到停止指令，结束分页循环");
                return;
            }
            try {
                // 尝试关闭订阅弹窗
                closeAnyBlockingOverlays();
                Locator closeBtn = page.locator(LiepinPageModel.SUBSCRIBE_CLOSE_BUTTON_SELECTOR);
                if (closeBtn.count() > 0) {
                    closeBtn.click();
                }
            } catch (Exception ignored) {
            }
            
        // 等待岗位卡片挂载（不要求可见，避免因遮挡造成超时）
        page.waitForSelector(
            LiepinPageModel.JOB_CARD_SELECTOR,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(15000)
        );
            info(String.format("正在投递【%s】第【%d】页...", cleanKeyword, i + 1));
            submitJob();
            info(String.format("已投递第【%d】页所有的岗位...", i + 1));
            
            // 查找下一页按钮（AntD v5 结构）
            paginationBox = page.locator(LiepinPageModel.PAGINATION_BOX_SELECTOR);
            Locator nextLi = paginationBox.locator(LiepinPageModel.NEXT_PAGE_SELECTOR);
            if (nextLi.count() > 0) {
                String cls = nextLi.first().getAttribute("class");
                boolean disabled = cls != null && cls.contains("ant-pagination-disabled");
                if (!disabled) {
                    Locator btn = nextLi.first().locator("button.ant-pagination-item-link");
                    if (btn.count() > 0) {
                        captureSearchResponse(() -> btn.first().click());
                    } else {
                        captureSearchResponse(() -> nextLi.first().click());
                    }
                } else {
                    break;
                }
            } else {
                failPageModel("猎聘未找到下一页按钮，页面模型可能已失效");
            }
        }
        info(String.format("【%s】关键词投递完成！", cleanKeyword));
    }

    String buildSearchUrl(String keyword) {
        String baseUrl = "https://www.liepin.com/zhaopin/?";
        StringBuilder sb = new StringBuilder(baseUrl);
        // 直接拼接参数，参数为空则忽略
        appendQueryParam(sb, "city", config.getCityCode());
        appendQueryParam(sb, "dq", config.getCityCode());
        appendQueryParam(sb, "salary", config.getSalary());
        appendQueryParam(sb, "compTag", config.getCompTag());
        appendQueryParam(sb, "pubTime", config.getPubTime());
        appendQueryParam(sb, "workYearCode", config.getWorkYearCode());
        appendQueryParam(sb, "eduLevel", config.getEduLevel());
        appendQueryParam(sb, "industry", config.getIndustry());
        appendQueryParam(sb, "jobKind", config.getJobKind());
        appendQueryParam(sb, "compScale", config.getCompScale());
        appendQueryParam(sb, "compStage", config.getCompStage());
        appendQueryParam(sb, "compKind", config.getCompKind());
        sb.append("currentPage=0");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&key=").append(encode(keyword));
        }
        return sb.toString();
    }

    private void appendQueryParam(StringBuilder sb, String name, String value) {
        if (!hasSearchValue(value)) {
            return;
        }
        sb.append(name).append("=").append(encode(value.trim())).append("&");
    }

    private boolean hasSearchValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !"0".equals(trimmed) && !"不限".equals(trimmed);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String safePageUrl() {
        try {
            return page == null ? "" : page.url();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safePageTitle() {
        try {
            return page == null ? "" : page.title();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void setMaxPage(Locator lis) {
        try {
            int count = lis.count();
            if (count >= 2) {
                String pageText = lis.nth(count - 2).textContent();
                int page = Integer.parseInt(pageText);
                if (page > 1) {
                    maxPage = page;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void submitJob() {
        // 获取hr数量
        Locator jobCards = page.locator(LiepinPageModel.JOB_CARD_SELECTOR);
        int count = jobCards.count();
        if (count == 0) {
            failPageModel("猎聘当前页未找到职位卡片，页面模型可能已失效");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (shouldStop()) {
                info("收到停止指令，结束卡片遍历");
                return;
            }
            // 获取当前岗位卡片（用于后续操作与缺省展示）
            Locator currentJobCard = page.locator(LiepinPageModel.JOB_CARD_SELECTOR).nth(i);
            // 从接口数据获取展示所需字段，若接口数据缺失则使用缺省占位，仍尝试打招呼
            String jobName = null;
            String companyName = null;
            String salary = null;
            String recruiterName = null;
            if (i < lastApiEntities.size()) {
                LiepinEntity apiEntity = lastApiEntities.get(i);
                jobName = safeText(apiEntity.getJobTitle());
                companyName = safeText(apiEntity.getCompName());
                salary = safeText(apiEntity.getJobSalaryText());
                recruiterName = safeText(apiEntity.getHrName());
            }
            if (recruiterName == null) recruiterName = "HR";
            if (jobName == null) jobName = "岗位";
            if (companyName == null) companyName = "公司";
            if (salary == null) salary = "";
            
            try {
                // 使用JavaScript滚动到卡片位置，更稳定
                try {
                    // 先滚动到卡片位置
                    page.evaluate("(element) => element.scrollIntoView({behavior: 'instant', block: 'center'})", currentJobCard.elementHandle());
                    // PlaywrightUtil.sleep(1); // 等待滚动完成
                    
                    // 再次确保元素在视窗中
                    page.evaluate("(element) => { const rect = element.getBoundingClientRect(); if (rect.top < 0 || rect.bottom > window.innerHeight) { element.scrollIntoView({behavior: 'instant', block: 'center'}); } }", currentJobCard.elementHandle());
                    // PlaywrightUtil.sleep(1);
                } catch (Exception scrollError) {
                    log.warn("JavaScript滚动失败，尝试页面滚动: {}", scrollError.getMessage());
                    // 备用方案：滚动页面到大概位置
                    page.evaluate("window.scrollBy(0, " + (i * 200) + ")");
                    // PlaywrightUtil.sleep(1);
                }
                
                // 查找HR区域 - 尝试多种可能的HR标签选择器
                Locator hrArea = null;
                String[] hrSelectors = {
                    ".recruiter-info-box",  // 根据页面源码，这是主要的HR区域类名
                    ".recruiter-info, .hr-info, .contact-info",
                    "[class*='recruiter'], [class*='hr-'], [class*='contact']",
                    ".job-card-footer, .card-footer",
                    ".job-bottom, .bottom-info"
                };
                
                for (String selector : hrSelectors) {
                    Locator tempHrArea = currentJobCard.locator(selector);
                    if (tempHrArea.count() > 0) {
                        hrArea = tempHrArea.first();
                        log.debug("找到HR区域，使用选择器: {}", selector);
                        break;
                    }
                }
                
                // 如果找不到特定的HR区域，使用整个卡片
                if (hrArea == null) {
                    log.debug("未找到特定HR区域，使用整个岗位卡片");
                    hrArea = currentJobCard;
                }
                
                // 鼠标悬停到HR区域，触发按钮显示 - 简化悬停逻辑
                boolean hoverSuccess = false;
                int hoverRetries = 3;
                for (int retry = 0; retry < hoverRetries; retry++) {
                    try {
                        // 检查HR区域是否可见，如果不可见则跳过悬停
                        if (!hrArea.isVisible()) {
                            log.debug("HR区域不可见，跳过悬停操作");
                            hoverSuccess = true; // 设为成功，继续后续流程
                            break;
                        }
                        
                        // 直接悬停，不再进行复杂的微调
                        hrArea.hover(new Locator.HoverOptions().setTimeout(5000));
                        hoverSuccess = true;
                        break;
                    } catch (Exception hoverError) {
                        log.warn("第{}次悬停失败: {}", retry + 1, hoverError.getMessage());
                        if (retry < hoverRetries - 1) {
                            // 重试前重新滚动确保元素可见
                            try {
                                page.evaluate("(element) => element.scrollIntoView({behavior: 'instant', block: 'center'})", currentJobCard.elementHandle());
                                Thread.sleep(500); // 等待滚动完成
                            } catch (Exception e) {
                                log.warn("重试前滚动失败: {}", e.getMessage());
                            }
                        }
                    }
                }
                
                if (!hoverSuccess) {
                    log.warn("悬停操作失败，但继续查找按钮");
                    // 不再跳过，而是继续查找按钮，因为有些按钮可能不需要悬停就能显示
                }
                
                // PlaywrightUtil.sleep(1); // 等待按钮显示
                
                // 获取hr名字
                // 已从接口获取 HR 名字，无需再从页面读
                
            } catch (Exception e) {
                log.error("处理岗位卡片失败: {}", e.getMessage());
                continue;
            }
            
            // 查找聊一聊按钮
            Locator button = null;
            String buttonText = "";
            try {
                // 在当前岗位卡片中查找按钮，尝试多种选择器
                
                String[] buttonSelectors = {
                    "button.ant-btn.ant-btn-primary.ant-btn-round",
                    "button.ant-btn.ant-btn-round.ant-btn-primary", 
                    "button[class*='ant-btn'][class*='primary']",
                    "button[class*='ant-btn'][class*='round']",
                    "button[class*='chat'], button[class*='talk']",
                    ".chat-btn, .talk-btn, .contact-btn",
                    "button:has-text('聊一聊')",
                    "button" // 最后尝试所有按钮
                };
                
                for (String selector : buttonSelectors) {
                    try {
                        Locator tempButtons = currentJobCard.locator(selector);
                        int buttonCount = tempButtons.count();
                        log.debug("选择器 '{}' 找到 {} 个按钮", selector, buttonCount);
                        
                        for (int j = 0; j < buttonCount; j++) {
                            Locator tempButton = tempButtons.nth(j);
                            try {
                                if (tempButton.isVisible()) {
                                    String text = tempButton.textContent();
                                    log.debug("按钮文本: '{}'", text);
                                    if (text != null && !text.trim().isEmpty()) {
                                        button = tempButton;
                                        buttonText = text.trim();
                                        // 只关注"聊一聊"按钮
                                        if (text.contains("聊一聊")) {
                                            log.debug("找到目标按钮: '{}'", text);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                                log.debug("获取按钮文本失败: {}", ignore.getMessage());
                            }
                        }
                        
                        if (button != null && buttonText.contains("聊一聊")) {
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("选择器 '{}' 查找失败: {}", selector, e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                log.error("查找按钮失败: {}", e.getMessage());
                // 不再保存页面源码
                continue;
            }
            
            // 提取 jobId（用于更新投递状态）
            Long jobIdForUpdate = null;
            if (i < lastApiEntities.size()) {
                jobIdForUpdate = lastApiEntities.get(i).getJobId();
            }
            if (jobIdForUpdate == null) {
                jobIdForUpdate = extractJobIdFromCard(currentJobCard);
            }

            // 检查按钮文本并点击
            if (button != null && buttonText.contains("聊一聊")) {
                try {
                    closeAnyBlockingOverlays();
                    // 在点击按钮前进行鼠标微调，先向右移动2像素，再向左移动2像素
                    try {
                        var boundingBox = button.boundingBox();
                        if (boundingBox != null) {
                            double centerX = boundingBox.x + boundingBox.width / 2;
                            double centerY = boundingBox.y + boundingBox.height / 2;
                            
                            // 先移动到按钮中心
                            page.mouse().move(centerX, centerY);
                            Thread.sleep(50);
                            
                            // 向右移动2像素
                            page.mouse().move(centerX + 2, centerY);
                            Thread.sleep(50);
                            
                            // 向左移动2像素（回到中心再向左2像素）
                            page.mouse().move(centerX - 2, centerY);
                            Thread.sleep(50);
                            
                            // 回到中心位置
                            page.mouse().move(centerX, centerY);
                            Thread.sleep(50);
                            
                            log.debug("完成鼠标微调，准备点击按钮");
                        }
                    } catch (Exception moveError) {
                        log.warn("鼠标微调失败，直接点击按钮: {}", moveError.getMessage());
                    }
                    
                    button.click();
                    // PlaywrightUtil.sleep(1); // 等待点击响应
                    
                    // 只有检测到平台简历投递确认后才计入成功；聊一聊/沟通成功不等于投递简历。
                    try {
                        clickSendResumeButton();
                        confirmResumeDeliveryIfPrompted();
                        if (!waitForLiepinDeliveryConfirmation()) {
                            failPageModel("猎聘点击发简历后未检测到简历投递确认");
                        }
                        closeLiepinChatPanel();
                        
                        resultList.add(sb.append("【").append(companyName).append(" ").append(jobName).append(" ").append(salary).append(" ").append(recruiterName).append(" ").append("】").toString());
                        sb.setLength(0);
                        // 点击成功后标记为已投递
                        if (jobIdForUpdate != null) {
                            liepinService.markDelivered(jobIdForUpdate);
                        }
                        
                    } catch (Exception e) {
                        log.warn("猎聘岗位投递确认失败，跳过当前岗位: {}", e.getMessage());
                        info("猎聘岗位投递确认失败，已跳过: " + jobName + " - " + e.getMessage());
                        closeChatAndBlockingOverlays();
                        continue;
                    }

                } catch (Exception e) {
                    log.warn("猎聘点击聊一聊按钮失败，跳过当前岗位: {}", e.getMessage());
                    info("猎聘点击聊一聊按钮失败，已跳过: " + jobName + " - " + e.getMessage());
                    closeChatAndBlockingOverlays();
                    continue;
                }
            } else {
                if (button != null) {
                    log.debug("跳过岗位（按钮文本不匹配）: 【{}】的【{}·{}】岗位，按钮文本: '{}'", companyName, jobName, salary, buttonText);
                } else {
                    log.warn("猎聘未找到聊一聊按钮，跳过岗位: {}", jobName);
                    info("猎聘未找到聊一聊按钮，已跳过: " + jobName);
                }
            }
        }
    }

    private void clickSendResumeButton() {
        for (int attempt = 0; attempt < 30; attempt++) {
            String text = safeBodyText();
            if (LiepinPageModel.isBlockingText(text)) {
                failPageModel("猎聘出现阻塞提示: " + compactText(text));
            }
            try {
                page.waitForSelector(LiepinPageModel.CHAT_PANEL_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(1000));
            } catch (Exception ignored) {
            }

            Locator chatPanel = page.locator(LiepinPageModel.CHAT_PANEL_SELECTOR).first();
            if (clickSendResumeButtonInScope(chatPanel)) {
                return;
            }
            if (clickSendResumeButtonInScope(page.locator("body").first())) {
                return;
            }
            PlaywrightUtil.sleep(1);
        }
        failPageModel("猎聘聊天窗口已打开，但未找到发简历按钮");
    }

    private void confirmResumeDeliveryIfPrompted() {
        boolean promptSeen = false;
        for (int attempt = 0; attempt < LiepinPageModel.CONFIRM_RESUME_DELIVERY_WAIT_SECONDS; attempt++) {
            String text = safeBodyText();
            if (LiepinPageModel.isBlockingText(text)) {
                failPageModel("猎聘出现阻塞提示: " + compactText(text));
            }
            if (LiepinPageModel.isConfirmedDeliveryText(text)) {
                return;
            }
            boolean promptVisible = compactText(text).contains("选择附件简历")
                    || compactText(text).contains("附件简历");
            if (promptVisible) {
                promptSeen = true;
            }
            Locator dialog = page.locator(LiepinPageModel.ATTACHMENT_RESUME_DIALOG_SELECTOR).first();
            if (clickConfirmResumeDeliveryButtonInScope(dialog)) {
                return;
            }
            if (clickConfirmResumeDeliveryButtonInScope(page.locator("body").first())) {
                return;
            }
            if ((text.contains("立即投递") || text.contains("确认投递")) && clickConfirmResumeDeliveryButtonByText()) {
                return;
            }
            PlaywrightUtil.sleep(1);
        }
        String finalText = safeBodyText();
        if (promptSeen || finalText.contains("立即投递") || finalText.contains("确认投递")) {
            failPageModel("猎聘附件简历确认弹窗已打开，但未找到立即投递按钮");
        }
    }

    private boolean clickSendResumeButtonInScope(Locator scope) {
        try {
            if (scope.count() == 0) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
        for (String selector : LiepinPageModel.CHAT_READY_SELECTORS) {
            try {
                Locator sendResume = scope.locator(selector);
                int count = sendResume.count();
                for (int i = 0; i < count; i++) {
                    Locator candidate = sendResume.nth(i);
                    if (isVisibleAndEnabled(candidate)) {
                        String text = safeLocatorText(candidate);
                        candidate.click(new Locator.ClickOptions().setTimeout(5000));
                        info("猎聘已点击简历投递按钮: " + (text.isBlank() ? selector : text));
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean clickConfirmResumeDeliveryButtonInScope(Locator scope) {
        try {
            if (scope.count() == 0) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
        for (String selector : LiepinPageModel.CONFIRM_RESUME_DELIVERY_BUTTON_SELECTORS) {
            try {
                Locator confirm = scope.locator(selector);
                int count = confirm.count();
                for (int i = 0; i < count; i++) {
                    Locator candidate = confirm.nth(i);
                    if (isVisibleAndEnabled(candidate)) {
                        String text = safeLocatorText(candidate);
                        candidate.click(new Locator.ClickOptions().setTimeout(5000));
                        info("猎聘已点击附件简历确认按钮: " + (text.isBlank() ? selector : text));
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean clickConfirmResumeDeliveryButtonByText() {
        try {
            Object clicked = page.evaluate("""
                    () => {
                      const labels = ['立即投递', '确认投递'];
                      const isVisible = (element) => {
                        const style = window.getComputedStyle(element);
                        const rect = element.getBoundingClientRect();
                        return style && style.visibility !== 'hidden'
                          && style.display !== 'none'
                          && rect.width > 0
                          && rect.height > 0;
                      };
                      const nodes = Array.from(document.querySelectorAll('button, [role="button"], .ant-btn, a, div, span'));
                      for (const label of labels) {
                        const node = nodes.find((element) => (element.innerText || '').trim() === label && isVisible(element));
                        if (node) {
                          const clickable = node.closest('button, [role="button"], .ant-btn, a') || node;
                          clickable.click();
                          return true;
                        }
                      }
                      return false;
                    }
                    """);
            if (Boolean.TRUE.equals(clicked)) {
                info("猎聘已通过文本兜底点击附件简历确认按钮");
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isVisibleAndEnabled(Locator candidate) {
        try {
            return candidate.isVisible() && candidate.isEnabled();
        } catch (Exception e) {
            try {
                return candidate.isVisible();
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private String safeLocatorText(Locator locator) {
        try {
            String text = locator.textContent();
            return text == null ? "" : compactText(text);
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean waitForLiepinDeliveryConfirmation() {
        for (int i = 0; i < 30; i++) {
            String text = safeBodyText();
            if (LiepinPageModel.isBlockingText(text)) {
                failPageModel("猎聘出现阻塞提示: " + compactText(text));
            }
            if (LiepinPageModel.isConfirmedDeliveryText(text)) {
                return true;
            }
            try {
                Locator confirm = page.locator(LiepinPageModel.SUCCESS_CONFIRM_SELECTOR);
                if (confirm.count() > 0) {
                    for (String item : confirm.allInnerTexts()) {
                        if (LiepinPageModel.isConfirmedDeliveryText(item)) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            PlaywrightUtil.sleep(1);
        }
        return false;
    }

    private void closeAnyBlockingOverlays() {
        try {
            Locator closeButtons = page.locator(LiepinPageModel.CLOSE_BUTTON_SELECTOR);
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

    private void closeChatAndBlockingOverlays() {
        closeLiepinChatPanel();
        closeAnyBlockingOverlays();
    }

    private void closeLiepinChatPanel() {
        boolean clickedClose = false;
        for (String selector : LiepinPageModel.CHAT_CLOSE_BUTTON_SELECTORS) {
            if (clickFirstVisibleChatClose(selector)) {
                clickedClose = true;
                break;
            }
        }
        if (clickedClose) {
            waitForChatPanelToClose();
        }
    }

    private boolean clickFirstVisibleChatClose(String selector) {
        try {
            Locator close = page.locator(selector);
            for (int i = Math.min(close.count(), 3) - 1; i >= 0; i--) {
                try {
                    if (close.nth(i).isVisible()) {
                        close.nth(i).click(new Locator.ClickOptions().setForce(true).setTimeout(1000));
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void waitForChatPanelToClose() {
        try {
            page.waitForSelector(
                    LiepinPageModel.CHAT_HEADER_SELECTOR,
                    new Page.WaitForSelectorOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(3000));
        } catch (Exception ignored) {
        }
    }

    private void failPageModel(String message) {
        log.error(message);
        info(message);
        throw new PlatformPageModelException(message);
    }

    private String safeBodyText() {
        try {
            Object text = page.evaluate("() => document.body ? document.body.innerText : ''");
            return text == null ? "" : String.valueOf(text);
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

    // 从岗位卡片的 data 属性中提取 jobId（兼容 lastApiEntities 缺失场景）
    private Long extractJobIdFromCard(Locator card) {
        try {
            String ext = card.getAttribute("data-tlg-ext");
            if (ext != null && !ext.isEmpty()) {
                try {
                    String decoded = java.net.URLDecoder.decode(ext, java.nio.charset.StandardCharsets.UTF_8);
                    JsonNode node = new ObjectMapper().readTree(decoded);
                    String jobIdStr = node.path("jobId").asText(null);
                    if (jobIdStr != null && !jobIdStr.isEmpty()) {
                        return Long.parseLong(jobIdStr);
                    }
                } catch (Exception ignore) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\\\"jobId\\\\\":\\\\\"(\\d+)\\\\\"").matcher(ext);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                }
            }
            String scm = card.getAttribute("data-tlg-scm");
            if (scm != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("jobId=(\\d+)").matcher(scm);
                if (m.find()) {
                    return Long.parseLong(m.group(1));
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
