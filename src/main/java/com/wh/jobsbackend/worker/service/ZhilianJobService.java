package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.wh.jobsbackend.worker.utils.JobUtils;
import com.wh.jobsbackend.worker.utils.PlaywrightUtil;
import com.wh.jobsbackend.worker.zhilian.ZhiLian;
import com.wh.jobsbackend.worker.zhilian.ZhilianConfig;
import com.wh.jobsbackend.worker.zhilian.ZhilianPageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ZhilianJobService extends AbstractPlatformJobService {
    private static final String PLATFORM = "zhilian";
    private static final String HOME_URL = "https://www.zhaopin.com/sou/";

    private final PlaywrightManager playwrightManager;
    private final ObjectProvider<ZhiLian> zhilianProvider;
    private final ConfigService configService;

    public record ApplyButtonInspection(
            String keyword,
            int jobCardCount,
            int applyButtonCount,
            int visibleApplyButtonCount,
            String firstButtonText,
            String message
    ) {
    }

    public ZhilianJobService(PlaywrightManager playwrightManager, ObjectProvider<ZhiLian> zhilianProvider,
                             ConfigService configService, UserTaskService userTaskService) {
        super(userTaskService);
        this.playwrightManager = playwrightManager;
        this.zhilianProvider = zhilianProvider;
        this.configService = configService;
    }

    @Override
    protected String platform() {
        return PLATFORM;
    }

    @Override
    protected boolean isLoggedIn(Long userId) {
        return playwrightManager.isLoggedIn(userId, PLATFORM);
    }

    public ApplyButtonInspection inspectApplyButtons(Long userId) {
        return playwrightManager.withPlaywrightLock(() -> {
            Page page = playwrightManager.getPage(userId, PLATFORM);
            if (page == null) {
                throw new PlatformPageModelException("智联招聘页面未初始化");
            }
            if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
                throw new PlatformPageModelException("请先登录智联招聘");
            }

            ZhilianConfig config = configService.getZhilianConfig();
            if (config.getKeywords() == null || config.getKeywords().isEmpty()) {
                throw new IllegalArgumentException("请先配置智联招聘关键词");
            }

            String keyword = config.getKeywords().stream()
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("请先配置智联招聘关键词"));

            page.navigate(buildInspectionUrl(config));
            PlaywrightUtil.sleep(2);

            Locator keywordInput = findKeywordInput(page);
            if (keywordInput == null) {
                throw new PlatformPageModelException("智联招聘搜索框未找到，无法执行按钮干跑检查");
            }
            keywordInput.fill("");
            keywordInput.fill(keyword);
            try {
                keywordInput.press("Enter");
            } catch (Exception ignored) {
            }
            PlaywrightUtil.sleep(2);

            try {
                page.waitForSelector(ZhilianPageModel.JOB_CARD_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(10_000));
            } catch (Exception e) {
                throw new PlatformPageModelException("智联招聘职位列表未加载，无法检查投递按钮", e);
            }

            Locator cards = page.locator(ZhilianPageModel.JOB_CARD_SELECTOR);
            int jobCardCount = cards.count();
            int applyButtonCount = 0;
            int visibleApplyButtonCount = 0;
            String firstButtonText = null;

            for (int i = 0; i < jobCardCount; i++) {
                Locator buttons = cards.nth(i).locator(ZhilianPageModel.APPLY_BUTTON_SELECTOR);
                int buttonCount = buttons.count();
                applyButtonCount += buttonCount;
                for (int j = 0; j < buttonCount; j++) {
                    Locator button = buttons.nth(j);
                    String text = safeText(button);
                    if (isVisibleActionButton(button, text)) {
                        visibleApplyButtonCount++;
                        if (firstButtonText == null || firstButtonText.isBlank()) {
                            firstButtonText = text;
                        }
                    }
                }
            }

            String message = visibleApplyButtonCount > 0
                    ? String.format("找到 %d 个可见投递按钮", visibleApplyButtonCount)
                    : "未找到可见投递按钮";
            return new ApplyButtonInspection(keyword, jobCardCount, applyButtonCount,
                    visibleApplyButtonCount, firstButtonText, message);
        });
    }

    @Override
    protected void doExecute(Long userId, Consumer<JobProgressMessage> progressCallback) {
        playwrightManager.withPlaywrightLock(() -> {
        Page page = playwrightManager.getPage(userId, PLATFORM);
        if (page == null) {
            throw new PlatformPageModelException("智联招聘页面未初始化");
        }

        if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
            throw new PlatformPageModelException("请先登录智联招聘");
        }

        playwrightManager.pauseZhilianMonitoring();
        try {
            ZhilianConfig config = configService.getZhilianConfig();
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u914d\u7f6e\u52a0\u8f7d\u6210\u529f"));
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u5f00\u59cb\u6295\u9012\u4efb\u52a1..."));

            ZhiLian.ProgressCallback callback = (message, current, total) -> {
                if (current != null && total != null) {
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                } else {
                    progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                }
            };

            ZhiLian zhilian = zhilianProvider.getObject();
            zhilian.setPage(page);
            zhilian.setConfig(config);
            zhilian.setProgressCallback(callback);
            zhilian.setShouldStopCallback(() -> shouldStop(userId));
            zhilian.prepare();

            int deliveredCount = zhilian.execute();
            progressCallback.accept(JobProgressMessage.success(
                    PLATFORM,
                    String.format("\u6295\u9012\u4efb\u52a1\u5b8c\u6210\uff0c\u5171\u6295\u9012%d\u4e2a\u804c\u4f4d", deliveredCount)
            ));
        } finally {
            try {
                playwrightManager.resumeZhilianMonitoring();
            } catch (Exception ignored) {
            }
        }
        });
    }

    private String buildInspectionUrl(ZhilianConfig config) {
        StringBuilder url = new StringBuilder(HOME_URL);
        url.append("jl").append(config.getCityCode() == null ? "" : config.getCityCode()).append("/");
        url.append("p1?");
        url.append(JobUtils.appendParam("sl", config.getSalary()));
        return url.toString();
    }

    private Locator findKeywordInput(Page page) {
        String[] candidates = {
                "input[placeholder*='职位']",
                "input[placeholder*='公司']",
                "input[name='kw']",
                "input[type='text']",
                "input[class*='search'], input[class*='sou'], input[class*='input']"
        };
        for (String selector : candidates) {
            try {
                Locator input = page.locator(selector);
                if (input.count() > 0 && input.first().isVisible()) {
                    return input.first();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isVisibleActionButton(Locator button, String text) {
        try {
            return button.isVisible() && button.isEnabled() && ZhilianPageModel.isApplyButtonText(text);
        } catch (Exception e) {
            try {
                return button.isVisible() && ZhilianPageModel.isApplyButtonText(text);
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private String safeText(Locator locator) {
        try {
            String text = locator.textContent();
            return text == null ? "" : text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
