package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.liepin.Liepin;
import com.wh.jobsbackend.worker.liepin.LiepinConfig;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class LiepinJobService extends AbstractPlatformJobService {
    private static final String PLATFORM = "liepin";

    private final PlaywrightManager playwrightManager;
    private final ConfigService configService;
    private final ObjectProvider<Liepin> liepinProvider;

    public LiepinJobService(PlaywrightManager playwrightManager, ConfigService configService,
                            ObjectProvider<Liepin> liepinProvider, UserTaskService userTaskService) {
        super(userTaskService);
        this.playwrightManager = playwrightManager;
        this.configService = configService;
        this.liepinProvider = liepinProvider;
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
            throw new PlatformPageModelException("猎聘页面未初始化");
        }

        if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
            throw new PlatformPageModelException("请先登录猎聘");
        }

        playwrightManager.pauseLiepinMonitoring();
        try {
            LiepinConfig config = configService.getLiepinConfig(userId);
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u914d\u7f6e\u52a0\u8f7d\u6210\u529f"));
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "\u5f00\u59cb\u6295\u9012\u4efb\u52a1..."));

            Liepin.ProgressCallback callback = (message, current, total) -> {
                if (current != null && total != null) {
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                } else {
                    progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                }
            };

            Liepin liepin = liepinProvider.getObject();
            liepin.setPage(page);
            liepin.setConfig(config);
            liepin.setProgressCallback(callback);
            liepin.setShouldStopCallback(() -> shouldStop(userId));

            int deliveredCount = liepin.execute();
            progressCallback.accept(JobProgressMessage.success(
                    PLATFORM,
                    String.format("\u6295\u9012\u4efb\u52a1\u5b8c\u6210\uff0c\u5171\u53d1\u8d77%d\u4e2a\u804a\u5929", deliveredCount)
            ));
        } finally {
            try {
                playwrightManager.resumeLiepinMonitoring();
            } catch (Exception ignored) {
            }
        }
        });
    }
}
