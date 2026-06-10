package com.wh.jobsbackend.worker.zhilian;

import com.wh.jobsbackend.application.entity.ZhilianJobDataEntity;
import com.wh.jobsbackend.application.service.ZhilianService;
import com.wh.jobsbackend.worker.utils.Job;
import com.wh.jobsbackend.worker.utils.JobUtils;
import com.wh.jobsbackend.worker.utils.PlaywrightUtil;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 * 智联招聘自动投递 - Playwright版本
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ZhiLian {

    @Setter
    private Page page;

    @Setter
    private ZhilianConfig config;

    @Setter
    private ProgressCallback progressCallback;

    @Setter
    private Supplier<Boolean> shouldStopCallback;

    private final List<Job> resultList = new ArrayList<>();
    private boolean isLimit = false;
    private int maxPage = 500;

    private static final String HOME_URL = "https://www.zhaopin.com/sou/";

    private final ZhilianService zhilianService;

    private static class PageJob {
        int index;
        String jobId;
        String jobTitle;
        String companyName;

        PageJob(int index, String jobId, String jobTitle, String companyName) {
            this.index = index;
            this.jobId = jobId;
            this.jobTitle = jobTitle;
            this.companyName = companyName;
        }
    }

    /**
     * 进度回调接口
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void accept(String message, Integer current, Integer total);
    }

    /**
     * 准备工作：加载配置、初始化数据
     */
    public void prepare() {
        log.info("智联招聘准备工作开始...");
        resultList.clear();
        isLimit = false;
        log.info("智联招聘准备工作完成");
    }

    /**
     * 执行投递任务
     * @return 投递数量
     */
    public int execute() {
        log.info("智联招聘投递任务开始...");
        long startTime = System.currentTimeMillis();

        try {
            // 遍历所有关键词进行投递
            for (String keyword : config.getKeywords()) {
                if (shouldStop() || isLimit) {
                    sendProgress("用户取消投递或已达上限", null, null);
                    break;
                }

                String baseUrl = buildBaseUrl(1);
                deliverByKeyword(keyword, baseUrl);
            }

            long duration = System.currentTimeMillis() - startTime;
            String message = String.format("智联招聘投递完成，共投递%d个岗位，用时%s",
                resultList.size(), formatDuration(duration));
            log.info(message);
            sendProgress(message, null, null);

            if (!resultList.isEmpty()) {
                log.info("新投递公司如下:");
                resultList.forEach(job -> log.info(job.toString()));
            } else {
                log.info("未投递新的岗位...");
            }

        } catch (Exception e) {
            log.error("智联招聘投递过程出现异常", e);
            sendProgress("投递出现异常: " + e.getMessage(), null, null);
        }

        return resultList.size();
    }

    /**
     * 按关键词投递
     */
    private void deliverByKeyword(String keyword, String baseUrl) {
        if (isLimit) {
            return;
        }

        try {
            log.info("开始投递关键词: {}", keyword);
            sendProgress("正在搜索关键词: " + keyword, null, null);

            // 导航到搜索页面（路径参数：jl+城市码 + p1 + sl）
            page.navigate(baseUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60_000));
            PlaywrightUtil.sleep(2);

            // 在搜索框输入关键词并触发搜索（Enter键更稳健）
            try {
                Locator keywordInput = findKeywordInput();
                if (keywordInput == null || keywordInput.count() == 0) {
                    log.warn("未找到搜索输入框，跳过关键词: {}", keyword);
                    return;
                }
                keywordInput.fill("");
                keywordInput.fill(keyword);
                try { keywordInput.press("Enter"); } catch (Exception ignored) {}
                PlaywrightUtil.sleep(2);
            } catch (Exception e) {
                log.warn("搜索框输入关键词失败，跳过当前关键词: {}", e.getMessage());
                return;
            }

            // 等待岗位列表加载（CSS选择器）
            try {
                page.waitForSelector(ZhilianPageModel.JOB_CARD_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
            } catch (Exception e) {
                failPageModel("智联等待岗位列表超时，可能页面结构变化、未登录或被风控", e);
            }

            // 遍历所有页面：仅以“下一页”按钮禁用状态为主，最多50页
            int pageNum = 1;
            while (pageNum <= 50) {
                if (shouldStop() || isLimit) {
                    sendProgress("用户取消投递或已达上限", null, null);
                    return;
                }

                log.info("开始投递【{}】关键词，第【{}】页...", keyword, pageNum);
                sendProgress(String.format("正在投递第%d页", pageNum), pageNum, 50);

                // 等待岗位列表出现（CSS选择器）
                try {
                    page.waitForSelector(ZhilianPageModel.JOB_LIST_CONTAINER_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(10_000));
                } catch (Exception e) {
                    failPageModel("智联职位列表容器未出现，停止投递", e);
                }

                // 投递当前页面
                if (!deliverCurrentPage(keyword)) {
                    break;
                }

                PlaywrightUtil.sleep(2);

                // 判断是否还有下一页
                if (isNextDisabled()) {
                    log.info("下一页按钮不可点击，结束翻页");
                    break;
                }

                // 点击下一页
                Locator nextBtn = page.locator(ZhilianPageModel.NEXT_PAGE_SELECTOR);
                if (nextBtn.count() > 0) {
                    try { nextBtn.first().scrollIntoViewIfNeeded(); } catch (Exception ignored) {}
                    nextBtn.first().click();
                    PlaywrightUtil.sleep(2);
                    pageNum++;
                } else {
                    failPageModel("智联未找到下一页按钮，页面模型可能已失效", null);
                }
            }

            log.info("关键词【{}】投递完成", keyword);
        } catch (Exception e) {
            log.error("投递关键词【{}】时出现异常", keyword, e);
        }
    }

    /**
     * 投递当前页面的所有职位
     * @return 是否继续投递下一页
     */
    private boolean deliverCurrentPage(String keyword) {
        try {
            page.waitForSelector(ZhilianPageModel.JOB_CARD_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(15000));

            if (checkIsLimit()) {
                sendProgress("用户取消投递或已达上限", null, null);
                return false;
            }

            Locator cards = page.locator(ZhilianPageModel.JOB_CARD_SELECTOR);
            int count = cards.count();
            log.info("检测到当前页岗位数量: {}", count);
            if (count == 0) {
                failPageModel("智联当前页未找到职位卡片，页面模型可能已失效", null);
            }

            List<PageJob> jobs = new ArrayList<>();
            // 统一采集当前页岗位后再保存（不在循环中逐个入库）
            List<ZhilianJobDataEntity> toInsert = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                if (shouldStop()) {
                    sendProgress("用户取消投递或已达上限", null, null);
                    return false;
                }

                Locator card = cards.nth(i);
                String jobTitle = safeGetText(card, "a.jobinfo__name");
                String jobLink = null;
                try { jobLink = card.locator("a.jobinfo__name").getAttribute("href"); } catch (Exception ignored) {}
                String salary = safeGetText(card, "p.jobinfo__salary");
                String location = safeGetText(card, "div.jobinfo__other-info div.jobinfo__other-info-item > span");
                String experience = safeGetText(card, "div.jobinfo__other-info-item:nth-child(2)");
                String degree = safeGetText(card, "div.jobinfo__other-info-item:nth-child(3)");
                String companyName = safeGetText(card, "div.companyinfo__name");

                String jobId = extractJobIdFromLink(jobLink);

                try {
                    String jid = jobId == null ? "" : jobId.trim();
                    String jtitle = jobTitle == null ? "" : jobTitle.trim();
                    if (jid.isEmpty() || jtitle.isEmpty()) {
                        log.info("岗位缺少jobId或jobTitle，跳过采集：title={}，company={}", jtitle, companyName);
                    } else {
                        boolean exists = false;
                        try { exists = zhilianService.existsByJobId(jid); } catch (Exception checkEx) {
                            log.warn("查询jobId是否已存在失败: {}", checkEx.getMessage());
                        }
                        if (exists) {
                            log.info("jobId已存在，跳过采集：jobId={}，title={}", jid, jtitle);
                        } else {
                            ZhilianJobDataEntity entity = new ZhilianJobDataEntity();
                            entity.setJobId(jid);
                            entity.setJobTitle(jtitle);
                            entity.setJobLink(jobLink);
                            entity.setSalary(salary);
                            entity.setLocation(location);
                            entity.setExperience(experience);
                            entity.setDegree(degree);
                            entity.setCompanyName(companyName);
                            entity.setDeliveryStatus("未投递");
                            toInsert.add(entity);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("采集岗位数据失败: {}", ex.getMessage());
                }

                jobs.add(new PageJob(i, jobId, jobTitle, companyName));
            }

            // 统一保存采集到的一整页岗位
            if (!toInsert.isEmpty()) {
                for (ZhilianJobDataEntity entity : toInsert) {
                    try {
                        zhilianService.insertJob(entity);
                        log.info("已保存岗位数据：jobId={}，title={}，company={}", entity.getJobId(), entity.getJobTitle(), entity.getCompanyName());
                    } catch (Exception ex) {
                        log.warn("保存岗位数据失败: {}", ex.getMessage());
                    }
                }
            }

            for (PageJob pj : jobs) {
                if (shouldStop()) {
                    sendProgress("用户取消投递或已达上限", null, null);
                    return false;
                }

                closeAnyBlockingOverlays();
                Locator card = page.locator(ZhilianPageModel.JOB_CARD_SELECTOR).nth(pj.index);
                Locator applyBtn = card.locator(ZhilianPageModel.APPLY_BUTTON_SELECTOR);
                if (applyBtn.count() == 0) {
                    failPageModel("智联岗位未找到立即投递按钮: " + pj.jobTitle, null);
                }
                try {
                    int pagesBeforeClick = page.context().pages().size();
                    applyBtn.click(); /* 点击投递按钮（关键定位注释：delivery-click-line）*/
                    if (!waitForZhilianDeliveryConfirmation(pagesBeforeClick)) {
                        failPageModel("智联点击投递后未检测到平台成功确认: " + pj.jobTitle, null);
                    }

                    try {
                        if (pj.jobId != null && !pj.jobId.isEmpty()) {
                            zhilianService.markDeliveredByJobId(pj.jobId);
                            log.info("已标记投递：jobId={}，title={}，company={}", pj.jobId, pj.jobTitle, pj.companyName);
                        } else if (pj.jobTitle != null && pj.companyName != null) {
                            zhilianService.markDeliveredByTitleAndCompany(pj.jobTitle, pj.companyName);
                            log.info("已标记投递：title={}，company={}", pj.jobTitle, pj.companyName);
                        }
                    } catch (Exception ex) {
                        log.warn("更新投递状态失败: {}", ex.getMessage());
                    }
                } catch (Exception clickEx) {
                    failPageModel("智联投递失败: " + clickEx.getMessage(), clickEx);
                }

                if (checkIsLimit()) {
                    sendProgress("用户取消投递或已达上限", null, null);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("投递当前页面失败", e);
            try {
                saveCurrentPageHtml();
                log.info("已保存当前页面到 src/main/java/com/getjobs/worker/zhilian/page.html 以便排查");
            } catch (Exception saveEx) {
                log.warn("保存当前页面HTML失败: {}", saveEx.getMessage());
            }
            if (e instanceof PlatformPageModelException) {
                throw e;
            }
            throw new PlatformPageModelException("智联投递当前页面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理投递弹窗
     */
    private void handleDeliveryDialog() {
        try {
            // 获取所有页面
            List<Page> pages = page.context().pages();
            if (pages.size() < 2) {
                log.warn("未检测到投递弹窗页面");
                return;
            }

            // 切换到最新打开的页面（投递弹窗）
            Page dialogPage = pages.get(pages.size() - 1);

            try {
                // 检查投递结果（CSS选择器）
                Locator deliverResult = dialogPage.locator("div.deliver-dialog");
                if (deliverResult.count() > 0) {
                    String text = deliverResult.textContent();
                    if (text != null && text.contains("申请成功")) {
                        log.info("岗位申请成功！");
                    }
                }
            } catch (Exception e) {
                log.debug("读取投递结果失败: {}", e.getMessage());
            }

            // 关闭弹窗
            try {
                Locator closeButton = dialogPage.locator("img[title='close-icon']");
                if (closeButton.count() > 0) {
                    closeButton.click();
                    PlaywrightUtil.sleep(1);
                }
            } catch (Exception e) {
                log.debug("关闭投递弹窗失败: {}", e.getMessage());
                if (checkIsLimit()) {
                    return;
                }
            }

            // 投递相似职位
            deliverSimilarJobs(dialogPage);

            // 关闭弹窗页面
            try {
                dialogPage.close();
            } catch (Exception e) {
                log.debug("关闭弹窗页面失败: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("处理投递弹窗失败", e);
        }
    }

    /**
     * 投递相似职位
     */
    private void deliverSimilarJobs(Page dialogPage) {
        try {
            // 全选相似职位
            Locator selectAllCheckbox = dialogPage.locator("div.applied-select-all input");
            if (selectAllCheckbox.count() > 0 && !selectAllCheckbox.isChecked()) {
                selectAllCheckbox.click();
                PlaywrightUtil.sleep(1);
            }

            // 获取相似职位列表
            Locator jobs = dialogPage.locator("div.recommend-job");
            int jobCount = jobs.count();

            if (jobCount == 0) {
                log.info("没有匹配到相似职位");
                return;
            }

            // 记录相似职位信息
            for (int i = 0; i < jobCount; i++) {
                try {
                    Locator jobElement = jobs.nth(i);
                    String jobName = safeGetText(jobElement, ".recommend-job__position");
                    String salary = safeGetText(jobElement, "span.recommend-job__demand__salary");
                    String years = safeGetText(jobElement, "span.recommend-job__demand__experience").replace("\n", " ");
                    String education = safeGetText(jobElement, "span.recommend-job__demand__educational").replace("\n", " ");
                    String companyName = safeGetText(jobElement, ".recommend-job__cname");
                    String companyTag = safeGetText(jobElement, ".recommend-job__demand__cinfo").replace("\n", " ");

                    Job job = new Job();
                    job.setJobName(jobName);
                    job.setSalary(salary);
                    job.setCompanyTag(companyTag);
                    job.setCompanyName(companyName);
                    job.setJobInfo(years + "·" + education);

                    log.info("投递【{}】公司【{}】岗位，薪资【{}】，要求【{}·{}】，规模【{}】",
                        companyName, jobName, salary, years, education, companyTag);
                    resultList.add(job);
                } catch (Exception e) {
                    log.debug("记录相似职位信息失败: {}", e.getMessage());
                }
            }

            // 点击投递按钮
            Locator postButton = dialogPage.locator("div.applied-select-all button");
            if (postButton.count() > 0) {
                postButton.click();
                PlaywrightUtil.sleep(2);
                log.info("相似职位投递成功！");
            }

        } catch (Exception e) {
            log.error("投递相似职位异常: {}", e.getMessage());
        }
    }

    /**
     * 检查是否达到投递上限
     */
    private boolean checkIsLimit() {
        try {
            PlaywrightUtil.sleep(1);
            Locator result = page.locator("div.a-job-apply-workflow");
            if (result.count() > 0) {
                String text = result.textContent();
                if (text != null && text.contains("达到上限")) {
                    log.info("今日投递已达上限！");
                    isLimit = true;
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForZhilianDeliveryConfirmation(int pagesBeforeClick) {
        for (int i = 0; i < 5; i++) {
            if (checkIsLimit()) {
                return false;
            }
            String text = safeBodyText(page);
            if (ZhilianPageModel.isConfirmedDeliveryText(text)) {
                closeNewPopupPages(pagesBeforeClick);
                return true;
            }
            if (submitSelectedResumeIfPresent()) {
                PlaywrightUtil.sleep(1);
                continue;
            }
            if (ZhilianPageModel.isBlockingText(text) && !ZhilianPageModel.isConfirmedDeliveryText(text)) {
                failPageModel("智联出现阻塞提示: " + compactText(text), null);
            }
            try {
                Locator confirm = page.locator(ZhilianPageModel.SUCCESS_DIALOG_SELECTOR);
                if (confirm.count() > 0) {
                    for (String item : confirm.allInnerTexts()) {
                        if (ZhilianPageModel.isConfirmedDeliveryText(item)) {
                            closeAnyBlockingOverlays();
                            closeNewPopupPages(pagesBeforeClick);
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            List<Page> pages = page.context().pages();
            for (int j = pagesBeforeClick; j < pages.size(); j++) {
                Page popup = pages.get(j);
                String popupText = safeBodyText(popup);
                if (ZhilianPageModel.isConfirmedDeliveryText(popupText)) {
                    try {
                        popup.close();
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                if (ZhilianPageModel.isBlockingText(popupText)) {
                    try {
                        popup.close();
                    } catch (Exception ignored) {
                    }
                    failPageModel("智联弹出页阻塞投递: " + compactText(popupText), null);
                }
            }
            PlaywrightUtil.sleep(1);
        }
        closeNewPopupPages(pagesBeforeClick);
        return false;
    }

    private boolean submitSelectedResumeIfPresent() {
        try {
            Locator resumeSelection = page.locator(ZhilianPageModel.RESUME_SELECTION_SELECTOR).first();
            if (resumeSelection.count() == 0 || !resumeSelection.isVisible()) {
                return false;
            }
            String text = resumeSelection.textContent();
            if (!ZhilianPageModel.isResumeSelectionText(text)) {
                return false;
            }
            Locator deliveryButton = resumeSelection.locator(ZhilianPageModel.RESUME_DELIVERY_BUTTON_SELECTOR).first();
            if (deliveryButton.count() == 0) {
                deliveryButton = page.locator(ZhilianPageModel.RESUME_DELIVERY_BUTTON_SELECTOR).first();
            }
            if (deliveryButton.count() == 0 || !deliveryButton.isVisible()) {
                failPageModel("智联简历选择弹窗未找到投递简历按钮", null);
            }
            deliveryButton.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
            log.info("已在智联简历选择弹窗提交默认简历");
            return true;
        } catch (PlatformPageModelException e) {
            throw e;
        } catch (Exception e) {
            log.debug("处理智联简历选择弹窗失败: {}", e.getMessage());
            return false;
        }
    }

    private void closeAnyBlockingOverlays() {
        try {
            Locator closeButtons = page.locator(ZhilianPageModel.CLOSE_BUTTON_SELECTOR);
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

    private void closeNewPopupPages(int pagesBeforeClick) {
        try {
            List<Page> pages = page.context().pages();
            for (int i = pagesBeforeClick; i < pages.size(); i++) {
                try {
                    Page popup = pages.get(i);
                    if (popup != page) {
                        popup.close();
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void failPageModel(String message, Throwable cause) {
        log.error(message, cause);
        sendProgress(message, null, null);
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

    private String compactText(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120);
    }

    /**
     * 设置最大页数（已废弃：使用“下一页不可点击”+最多50页）
     */
    private void setMaxPages() {
        // 保留方法避免旧调用报错，但不再依赖输入框改页码
        maxPage = 50;
    }

    /**
     * 构建基础搜索URL（不含关键词，由搜索框触发）
     */
    private String buildBaseUrl(int pageNum) {
        StringBuilder url = new StringBuilder(HOME_URL);
        url.append("jl").append(config.getCityCode()).append("/");
        url.append("p").append(pageNum).append("?");
        url.append(JobUtils.appendParam("sl", config.getSalary()));
        return url.toString();
    }

    /**
     * 查找搜索关键词输入框（多候选选择器，提高鲁棒性）
     */
    private Locator findKeywordInput() {
        String[] candidates = new String[] {
            "input[placeholder*='职位']",
            "input[placeholder*='公司']",
            "input[name='kw']",
            "input[type='text']",
            "input[class*='search'], input[class*='sou'], input[class*='input']"
        };
        for (String sel : candidates) {
            try {
                Locator lc = page.locator(sel);
                if (lc != null && lc.count() > 0) {
                    return lc.first();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 判断“下一页”是否不可点击
     */
    private boolean isNextDisabled() {
        try {
            Locator nextBtn = page.locator(ZhilianPageModel.NEXT_PAGE_SELECTOR);
            if (nextBtn.count() == 0) return true;
            String cls = null;
            try { cls = nextBtn.first().getAttribute("class"); } catch (Exception ignored) {}
            String disabledAttr = null;
            try { disabledAttr = nextBtn.first().getAttribute("disabled"); } catch (Exception ignored) {}
            if (cls != null && cls.contains(ZhilianPageModel.DISABLED_NEXT_PAGE_CLASS)) return true;
            return disabledAttr != null && ("disabled".equalsIgnoreCase(disabledAttr) || "true".equalsIgnoreCase(disabledAttr));
        } catch (Exception e) {
            return false;
        }
    }

    private String extractJobIdFromLink(String link) {
        if (link == null) return null;
        try {
            int i = link.indexOf("jobdetail/");
            int j = link.lastIndexOf(".htm");
            if (i >= 0 && j > i) {
                return link.substring(i + "jobdetail/".length(), j);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 安全获取文本内容
     */
    private String safeGetText(Locator parent, String selector) {
        try {
            Locator element = parent.locator(selector);
            if (element.count() > 0) {
                return element.textContent();
            }
        } catch (Exception e) {
            log.debug("获取文本失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 将当前页面内容保存到项目内 page.html，覆盖原文件
     */
    private void saveCurrentPageHtml() {
        try {
            String html = page.content();
            java.nio.file.Path path = java.nio.file.Paths.get("src/main/java/com/getjobs/worker/zhilian/page.html");
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("写入 page.html 失败", e);
        }
    }

    /**
     * 发送进度消息
     */
    private void sendProgress(String message, Integer current, Integer total) {
        if (progressCallback != null) {
            progressCallback.accept(message, current, total);
        }
    }

    /**
     * 检查是否应该停止
     */
    private boolean shouldStop() {
        return shouldStopCallback != null && shouldStopCallback.get();
    }
}
