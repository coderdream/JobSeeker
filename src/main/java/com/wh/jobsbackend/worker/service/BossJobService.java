package com.wh.jobsbackend.worker.service;

import com.microsoft.playwright.Page;
import com.wh.jobsbackend.application.entity.BossJobDataEntity;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.UserTaskService;
import com.wh.jobsbackend.worker.boss.Boss;
import com.wh.jobsbackend.worker.boss.BossConfig;
import com.wh.jobsbackend.worker.dto.JobProgressMessage;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import com.wh.jobsbackend.worker.PlatformPageModelException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class BossJobService extends AbstractPlatformJobService {
    private static final String PLATFORM = "boss";

    private final PlaywrightManager playwrightManager;
    private final ConfigService configService;
    private final ObjectProvider<Boss> bossProvider;

    public BossJobService(PlaywrightManager playwrightManager, ConfigService configService,
                          ObjectProvider<Boss> bossProvider, UserTaskService userTaskService) {
        super(userTaskService);
        this.playwrightManager = playwrightManager;
        this.configService = configService;
        this.bossProvider = bossProvider;
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
        if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
            throw new PlatformPageModelException("请先登录Boss直聘");
        }

        try {
            long startTime = System.currentTimeMillis();
            BossConfig config = configService.getBossConfig(userId);
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "配置加载成功"));
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "开始抓取职位列表..."));

            Boss.ProgressCallback callback = (message, current, total) -> {
                if (current != null && total != null) {
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                } else {
                    progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                }
            };

            Boss boss = bossProvider.getObject();
            boss.setConfig(config);
            boss.setUserId(userId);
            boss.setProgressCallback(callback);
            boss.setShouldStopCallback(() -> shouldStop(userId));
            boss.prepare();

            int deliveredCount = boss.execute();
            long elapsed = System.currentTimeMillis() - startTime;
            long hours = elapsed / 3600000;
            long minutes = (elapsed % 3600000) / 60000;
            double seconds = (elapsed % 60000) / 1000.0;
            String timeStr = hours > 0 
                    ? String.format("%d时%d分%.3f秒", hours, minutes, seconds)
                    : (minutes > 0 ? String.format("%d分%.3f秒", minutes, seconds) : String.format("%.3f秒", seconds));

            progressCallback.accept(JobProgressMessage.success(
                    PLATFORM,
                    String.format("抓取任务完成，共抓取 %d 个岗位，耗时 %s", deliveredCount, timeStr)
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 一键投递指定岗位集合
     */
    public void applySpecificJobs(Long userId, List<BossJobDataEntity> jobs, Consumer<JobProgressMessage> progressCallback) {
        playwrightManager.withPlaywrightLock(() -> {
            Page page = playwrightManager.getPage(userId, PLATFORM);
            if (page == null) {
                throw new PlatformPageModelException("Boss页面未初始化");
            }
            if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
                throw new PlatformPageModelException("请先登录Boss直聘");
            }

            playwrightManager.pauseBossMonitoring();
            try {
                progressCallback.accept(JobProgressMessage.info(PLATFORM, "开始一键投递选定岗位..."));
                Boss.ProgressCallback callback = (message, current, total) -> {
                    if (current != null && total != null) {
                        progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                    } else {
                        progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                    }
                };

                Boss boss = bossProvider.getObject();
                boss.setPage(page);
                BossConfig config = configService.getBossConfig(userId);
                boss.setConfig(config);
                boss.setUserId(userId);
                boss.setProgressCallback(callback);
                boss.setShouldStopCallback(() -> shouldStop(userId));
                boss.prepare();

                int successCount = 0;
                int total = jobs.size();
                for (int i = 0; i < total; i++) {
                    if (shouldStop(userId)) {
                        progressCallback.accept(JobProgressMessage.info(PLATFORM, "投递任务被手动终止"));
                        break;
                    }
                    BossJobDataEntity job = jobs.get(i);
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, "正在投递: " + job.getJobName(), i + 1, total));
                    try {
                        String result = boss.applyJobByUrl(
                                job.getId(),
                                job.getJobUrl(),
                                job.getCompanyName(),
                                job.getJobName(),
                                config.getSayHi()
                        );
                        if ("已投递".equals(result)) {
                            successCount++;
                        } else {
                            progressCallback.accept(JobProgressMessage.info(PLATFORM,
                                    "岗位投递失败: " + job.getJobName()));
                        }
                    } catch (Exception e) {
                        progressCallback.accept(JobProgressMessage.info(PLATFORM, "岗位投递失败: " + job.getJobName() + " - " + e.getMessage()));
                    }
                }
                progressCallback.accept(JobProgressMessage.success(PLATFORM, "一键投递完成，成功: " + successCount + "/" + total));
            } finally {
                try {
                    playwrightManager.resumeBossMonitoring();
                } catch (Exception ignored) {
                }
            }
        });
    }

    /**
     * 批量获取指定岗位详情
     */
    public void fetchSpecificJobDetails(Long userId, List<BossJobDataEntity> jobs, Consumer<JobProgressMessage> progressCallback) {
        if (!playwrightManager.isLoggedIn(userId, PLATFORM)) {
            throw new PlatformPageModelException("请先登录Boss直聘");
        }

        try {
            progressCallback.accept(JobProgressMessage.info(PLATFORM, "开始批量获取选定岗位详情..."));
            Boss.ProgressCallback callback = (message, current, total) -> {
                if (current != null && total != null) {
                    progressCallback.accept(JobProgressMessage.progress(PLATFORM, message, current, total));
                } else {
                    progressCallback.accept(JobProgressMessage.info(PLATFORM, message));
                }
            };
            Boss boss = bossProvider.getObject();
            boss.setUserId(userId);
            BossConfig config = configService.getBossConfig(userId);
            boss.setConfig(config);
            boss.setProgressCallback(callback);
            boss.setShouldStopCallback(() -> shouldStop(userId));
            boss.prepare();

            boss.fetchJobDetails(jobs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
