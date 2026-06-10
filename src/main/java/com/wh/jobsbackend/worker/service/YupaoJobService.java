package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.yupao.Yupao;
import com.wh.jobsbackend.worker.yupao.YupaoConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class YupaoJobService extends AbstractPlatformJobService {
    private static final String PLATFORM = "yupao";

    private final PlaywrightManager playwrightManager;
    private final ConfigService configService;
    private final ObjectProvider<Yupao> yupaoProvider;

    public YupaoJobService(PlaywrightManager playwrightManager, ConfigService configService,
                           ObjectProvider<Yupao> yupaoProvider, UserTaskService userTaskService) {
        super(userTaskService);
        this.playwrightManager = playwrightManager;
        this.configService = configService;
        this.yupaoProvider = yupaoProvider;
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
                throw new PlatformPageModelException("鱼泡页面未初始化");
            }
            if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
                throw new PlatformPageModelException("请先登录鱼泡直聘");
            }
            playwrightManager.pauseYupaoMonitoring();
            try {
                YupaoConfig config = configService.getYupaoConfig();
                progressCallback.accept(JobProgressMessage.info(PLATFORM, "配置加载成功"));
                Yupao yupao = yupaoProvider.getObject();
                yupao.setPage(page);
                yupao.setConfig(config);
                yupao.setShouldStopCallback(() -> shouldStop(userId));
                yupao.setProgressCallback((message, current, total) -> {
                    if (current != null && total != null) {
                        progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                    } else {
                        progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                    }
                });
                int deliveredCount = yupao.execute();
                progressCallback.accept(JobProgressMessage.success(PLATFORM, "鱼泡任务完成，共沟通/投递 " + deliveredCount + " 个岗位"));
            } finally {
                try {
                    playwrightManager.resumeYupaoMonitoring();
                } catch (Exception ignored) {
                }
            }
        });
    }
}
