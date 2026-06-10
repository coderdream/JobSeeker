package com.wh.jobsbackend.worker.yupao;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.wh.jobsbackend.application.entity.YupaoJobDataEntity;
import com.wh.jobsbackend.application.service.YupaoService;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class Yupao {
    @Setter
    private Page page;
    @Setter
    private YupaoConfig config;
    @Setter
    private ProgressCallback progressCallback;
    @Setter
    private Supplier<Boolean> shouldStopCallback;

    private final YupaoService yupaoService;

    @FunctionalInterface
    public interface ProgressCallback {
        void accept(String message, Integer current, Integer total);
    }

    public int execute() {
        if (page == null) {
            throw new PlatformPageModelException("鱼泡页面未初始化");
        }
        if (config == null) {
            config = new YupaoConfig();
        }
        int delivered = 0;
        for (String keyword : YupaoPageModel.normalizedKeywords(config)) {
            if (shouldStop()) {
                sendProgress("用户取消鱼泡任务", null, null);
                break;
            }
            delivered += executeKeyword(keyword);
        }
        return delivered;
    }

    private int executeKeyword(String keyword) {
        int delivered = 0;
        String url = YupaoPageModel.buildSearchUrl(config, keyword);
        sendProgress("正在搜索鱼泡岗位: " + (keyword.isBlank() ? "全部" : keyword), null, null);
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(60000));
        try {
            page.waitForSelector(YupaoPageModel.JOB_LIST_CONTAINER_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(10000));
        } catch (Exception e) {
            throw new PlatformPageModelException("鱼泡岗位列表未出现，可能页面结构变化或未登录", e);
        }

        Locator cards = page.locator(YupaoPageModel.JOB_CARD_SELECTOR);
        int count = Math.min(cards.count(), 50);
        sendProgress("鱼泡检测到岗位数: " + count, 0, count);
        for (int i = 0; i < count; i++) {
            if (shouldStop()) {
                sendProgress("用户取消鱼泡任务", i, count);
                break;
            }
            Locator card = cards.nth(i);
            YupaoJobDataEntity job = parseJob(card);
            if (job.getJobId() == null || job.getJobId().isBlank()) {
                job.setJobId(stableFallbackJobId(job));
            }
            if (!yupaoService.existsByJobId(job.getJobId())) {
                yupaoService.insertJob(job);
            }
            if (tryDeliver(card, job)) {
                delivered++;
            }
            sendProgress("鱼泡岗位处理进度", i + 1, count);
        }
        return delivered;
    }

    private YupaoJobDataEntity parseJob(Locator card) {
        YupaoJobDataEntity entity = new YupaoJobDataEntity();
        entity.setJobTitle(firstText(card,
                "[class*='title']", "[class*='name']", "a", "h3", "h2"));
        entity.setJobLink(firstAttribute(card, "a[href]", "href"));
        entity.setSalary(firstText(card,
                "[class*='salary']", "[class*='wage']", "text=/\\d+[-~至]\\d+[Kk千]?/"));
        entity.setLocation(firstText(card,
                "[class*='city']", "[class*='address']", "[class*='area']"));
        entity.setExperience(firstText(card,
                "[class*='experience']", "[class*='year']"));
        entity.setDegree(firstText(card,
                "[class*='degree']", "[class*='edu']"));
        entity.setCompanyName(firstText(card,
                "[class*='company']", "[class*='corp']"));
        entity.setHrName(firstText(card,
                "[class*='hr']", "[class*='boss']", "[class*='contact']"));
        entity.setPublishTime(firstText(card,
                "[class*='time']", "[class*='date']"));
        entity.setJobId(extractJobId(entity.getJobLink()));
        return entity;
    }

    private boolean tryDeliver(Locator card, YupaoJobDataEntity job) {
        try {
            Locator buttons = card.locator(YupaoPageModel.APPLY_BUTTON_SELECTOR);
            int count = Math.min(buttons.count(), 5);
            for (int i = 0; i < count; i++) {
                Locator button = buttons.nth(i);
                String text = safeText(button);
                if (!YupaoPageModel.isDeliveryActionText(text)) {
                    continue;
                }
                button.click(new Locator.ClickOptions().setTimeout(PlaywrightManager.DEFAULT_TIMEOUT));
                Thread.sleep(1000);
                String body = bodyText();
                if (YupaoPageModel.isBlockingText(body)) {
                    yupaoService.markFailedByJobId(job.getJobId());
                    return false;
                }
                if (YupaoPageModel.isDeliveredText(body) || YupaoPageModel.isDeliveredText(safeText(card))) {
                    yupaoService.markDeliveredByJobId(job.getJobId());
                    return true;
                }
                yupaoService.markDeliveredByJobId(job.getJobId());
                return true;
            }
            if (YupaoPageModel.isDeliveredText(safeText(card))) {
                yupaoService.markDeliveredByJobId(job.getJobId());
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("鱼泡投递失败 jobId={}, title={}, error={}", job.getJobId(), job.getJobTitle(), e.getMessage());
            yupaoService.markFailedByJobId(job.getJobId());
            return false;
        }
    }

    private String firstText(Locator parent, String... selectors) {
        for (String selector : selectors) {
            try {
                Locator locator = parent.locator(selector).first();
                if (locator.count() > 0) {
                    String text = locator.textContent();
                    if (text != null && !text.trim().isEmpty()) return text.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String firstAttribute(Locator parent, String selector, String attribute) {
        try {
            Locator locator = parent.locator(selector).first();
            if (locator.count() > 0) {
                return locator.getAttribute(attribute);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String safeText(Locator locator) {
        try {
            String text = locator.textContent();
            return text == null ? "" : text;
        } catch (Exception e) {
            return "";
        }
    }

    private String bodyText() {
        try {
            Object text = page.evaluate("() => document.body ? document.body.innerText : ''");
            return text == null ? "" : String.valueOf(text);
        } catch (Exception e) {
            return "";
        }
    }

    private String extractJobId(String link) {
        if (link == null || link.isBlank()) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:job|gong|zhaogong|detail)[^0-9A-Za-z]*([0-9A-Za-z_-]{6,})")
                .matcher(link);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return Integer.toHexString(link.hashCode());
    }

    private String stableFallbackJobId(YupaoJobDataEntity job) {
        String raw = String.join("|",
                nullToBlank(job.getJobTitle()),
                nullToBlank(job.getCompanyName()),
                nullToBlank(job.getLocation()));
        if (raw.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return Integer.toHexString(raw.hashCode());
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private boolean shouldStop() {
        return shouldStopCallback != null && Boolean.TRUE.equals(shouldStopCallback.get());
    }

    private void sendProgress(String message, Integer current, Integer total) {
        if (progressCallback != null) {
            progressCallback.accept(message, current, total);
        }
    }
}
