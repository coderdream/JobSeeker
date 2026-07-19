package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.job51.Job51;
import com.wh.jobsbackend.worker.job51.Job51Config;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class Job51JobService extends AbstractPlatformJobService {
    private static final String PLATFORM = "51job";

    private final PlaywrightManager playwrightManager;
    private final ObjectProvider<Job51> job51Provider;
    private final ConfigService configService;

    public Job51JobService(PlaywrightManager playwrightManager, ObjectProvider<Job51> job51Provider,
                           ConfigService configService, UserTaskService userTaskService) {
        super(userTaskService);
        this.playwrightManager = playwrightManager;
        this.job51Provider = job51Provider;
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

    @Override
    protected void doExecute(Long userId, Consumer<JobProgressMessage> progressCallback) {
        playwrightManager.withPlaywrightLock(() -> {
        Page page = playwrightManager.getPage(userId, PLATFORM);
        if (page == null) {
            throw new PlatformPageModelException("51job页面未初始化");
        }

        if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
            throw new PlatformPageModelException("请先登录51job");
        }

        playwrightManager.pause51jobMonitoring();
        try {
            Job51Config config = configService.getJob51Config(userId);
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u914d\u7f6e\u52a0\u8f7d\u6210\u529f"));
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u5f00\u59cb\u6295\u9012\u4efb\u52a1..."));

            Job51.ProgressCallback callback = (message, current, total) -> {
                if (message != null && message.contains("\u5f53\u524d\u9875\u672a\u91c7\u96c6\u5230\u4efb\u4f55 jobId")) {
                    progressCallback.accept(JobProgressMessage.warning(
                            PLATFORM,
                            "\u68c0\u6d4b\u5230\u5f53\u524d\u9875\u65e0\u5c97\u4f4dID\uff0c\u7591\u4f3c\u8fbe\u5230\u4e0a\u9650\u6216\u9875\u9762\u53d8\u5316\uff0c\u4efb\u52a1\u5df2\u505c\u6b62"
                    ));
                    stopDelivery(userId);
                    return;
                }

                if (current != null && total != null) {
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                } else {
                    progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                }
            };

            Job51 job51 = job51Provider.getObject();
            job51.setPage(page);
            job51.setConfig(config);
            job51.setProgressCallback(callback);
            job51.setShouldStopCallback(() -> shouldStop(userId));
            job51.prepare();

            int deliveredCount = job51.execute();
            progressCallback.accept(JobProgressMessage.success(
                    PLATFORM,
                    String.format("\u6295\u9012\u4efb\u52a1\u5b8c\u6210\uff0c\u5171\u6295\u9012%d\u4e2a\u804c\u4f4d", deliveredCount)
            ));
        } finally {
            try {
                playwrightManager.resume51jobMonitoring();
            } catch (Exception ignored) {
            }
        }
        });
    }
}
