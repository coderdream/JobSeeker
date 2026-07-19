package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.jobsbackend.application.entity.ConfigEntity;
import com.wh.jobsbackend.application.entity.LiepinConfigEntity;
import com.wh.jobsbackend.application.mapper.ConfigMapper;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.worker.boss.BossConfig;
import com.wh.jobsbackend.worker.job51.Job51Config;
import com.wh.jobsbackend.worker.liepin.LiepinConfig;
import com.wh.jobsbackend.worker.yupao.YupaoConfig;
import com.wh.jobsbackend.worker.zhilian.ZhilianConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigMapper configMapper;
    private final CurrentUserService currentUserService;
    private final LiepinService liepinService;
    private final BossService bossService;
    private final ZhilianService zhilianService;
    private final Job51Service job51Service;
    private final YupaoService yupaoService;

    /**
     * 获取所有配置（以Map形式返回）
     * @return 配置Map，key为config_key，value为config_value
     */
    public Map<String, String> getAllConfigsAsMap() {
        List<ConfigEntity> configs = getAllConfigs();
        Map<String, String> configMap = new HashMap<>();

        for (ConfigEntity config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }

        return configMap;
    }

    /**
     * 获取所有配置
     * @return 配置列表
     */
    public List<ConfigEntity> getAllConfigs() {
        LambdaQueryWrapper<ConfigEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigEntity::getUserId, currentUserService.requireUserId());
        return configMapper.selectList(queryWrapper);
    }

    /**
     * 根据配置键获取配置
     * @param configKey 配置键
     * @return 配置实体
     */
    public ConfigEntity getConfigByKey(String configKey) {
        return getConfigByKey(currentUserService.requireUserId(), configKey);
    }

    public ConfigEntity getConfigByKey(Long userId, String configKey) {
        LambdaQueryWrapper<ConfigEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigEntity::getUserId, userId)
                .eq(ConfigEntity::getConfigKey, configKey);
        return configMapper.selectOne(queryWrapper);
    }

    /**
     * 根据分类获取配置列表
     * @param category 分类
     * @return 配置列表
     */
    public List<ConfigEntity> getConfigsByCategory(String category) {
        return getConfigsByCategory(currentUserService.requireUserId(), category);
    }

    public List<ConfigEntity> getConfigsByCategory(Long userId, String category) {
        LambdaQueryWrapper<ConfigEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigEntity::getUserId, userId)
                .eq(ConfigEntity::getCategory, category);
        return configMapper.selectList(queryWrapper);
    }

    /**
     * 根据配置键获取配置值（可能为null）
     * @param configKey 配置键
     * @return 配置值或null
     */
    public String getConfigValue(String configKey) {
        return getConfigValue(currentUserService.requireUserId(), configKey);
    }

    public String getConfigValue(Long userId, String configKey) {
        ConfigEntity entity = getConfigByKey(userId, configKey);
        return entity != null ? entity.getConfigValue() : null;
    }

    /**
     * 根据配置键获取必填配置值（缺失或空则抛异常）
     * @param configKey 配置键
     * @return 配置值（非空）
     * @throws IllegalStateException 当配置缺失或空白时抛出
     */
    public String requireConfigValue(String configKey) {
        String value = getConfigValue(currentUserService.requireUserId(), configKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少必要配置: " + configKey);
        }
        return value;
    }

    /**
     * 获取AI调用所需的基础配置（BASE_URL, API_KEY, MODEL）
     * @return 配置Map，包含BASE_URL, API_KEY, MODEL
     */
    public Map<String, String> getAiConfigs() {
        Map<String, String> result = new HashMap<>();
        String baseUrl = requireConfigValue("BASE_URL");
        String apiKey = requireConfigValue("API_KEY");
        String model = requireConfigValue("MODEL");
        result.put("BASE_URL", baseUrl);
        result.put("API_KEY", apiKey);
        result.put("MODEL", model);
        return result;
    }

    /**
     * 批量更新配置
     * @param configMap 配置Map，key为config_key，value为config_value
     * @return 更新的配置数量
     */
    @Transactional
    public int batchUpdateConfigs(Map<String, String> configMap) {
        Long userId = currentUserService.requireUserId();
        int updateCount = 0;

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ConfigEntity config = getConfigByKey(userId, key);

            if (config != null) {
                config.setConfigValue(value);
                config.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(config);
                updateCount++;
                log.info("更新配置: {} = {}", key, value);
            } else {
                log.warn("配置键不存在: {}", key);
            }
        }

        return updateCount;
    }

    /**
     * 更新单个配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateConfig(String configKey, String configValue) {
        ConfigEntity config = getConfigByKey(currentUserService.requireUserId(), configKey);

        if (config != null) {
            config.setConfigValue(configValue);
            config.setUpdatedAt(LocalDateTime.now());
            int result = configMapper.updateById(config);

            if (result > 0) {
                log.info("更新配置成功: {} = {}", configKey, configValue);
                return true;
            }
        } else {
            log.warn("配置键不存在: {}", configKey);
        }

        return false;
    }

    /**
     * 创建新配置
     * @param config 配置实体
     * @return 是否创建成功
     */
    @Transactional
    public boolean createConfig(ConfigEntity config) {
        config.setUserId(currentUserService.requireUserId());
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        int result = configMapper.insert(config);

        if (result > 0) {
            log.info("创建配置成功: {} = {}", config.getConfigKey(), config.getConfigValue());
            return true;
        }

        return false;
    }

    /**
     * 统一入口：从专表 hub_liepin_config 读取并构造 LiepinConfig
     * 说明：每个平台维护专表，由 ConfigService 暴露统一读取方法供 Worker 使用
     */
    public LiepinConfig getLiepinConfig() {
        return getLiepinConfig(currentUserService.requireUserId());
    }

    public LiepinConfig getLiepinConfig(Long userId) {
        LiepinConfigEntity entity = liepinService.getFirstConfig(userId);

        LiepinConfig config = new LiepinConfig();

        // 关键词解析：支持逗号、中文逗号、或 [a,b] 格式
        List<String> keywords = new java.util.ArrayList<>();
        if (entity != null && entity.getKeywords() != null) {
            String raw = entity.getKeywords().trim();
            raw = raw.replace('\uFF0C', ',');
            if (raw.startsWith("[") && raw.endsWith("]")) {
                raw = raw.substring(1, raw.length() - 1);
            }
            for (String s : raw.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) keywords.add(t);
            }
        }
        config.setKeywords(keywords);

        // 城市编码：允许传中文名或代码；中文名映射为代码；缺省视为不限
        String cityCode = "";
        if (entity != null && !isLiepinUnlimited(entity.getCity())) {
            // 先按中文名查 code；若查不到，尝试该值是否是有效 code
            String city = entity.getCity().trim();
            String codeByName = liepinService.getCodeByTypeAndName("city", city);
            if (isLiepinUnlimited(codeByName)) {
                cityCode = "";
            } else if (codeByName == null || codeByName.isEmpty()) {
                String maybeName = liepinService.getNameByTypeAndCode("city", city);
                if (maybeName == null || maybeName.isEmpty() || maybeName.equals(city)) {
                    throw new IllegalArgumentException("未在数据库中找到城市编码: " + city);
                } else {
                    cityCode = city;
                }
            } else {
                cityCode = codeByName;
            }
        }
        config.setCityCode(cityCode);

        if (entity != null) {
            config.setSalary(resolveLiepinOptionCode("salary", entity.getSalaryCode()));
            config.setCompTag(resolveLiepinOptionCode("compTag", entity.getCompTag()));
            config.setPubTime(resolveLiepinOptionCode("pubTime", entity.getPubTime()));
            config.setWorkYearCode(resolveLiepinOptionCode("workYearCode", entity.getWorkYearCode()));
            config.setEduLevel(resolveLiepinOptionCode("degree", entity.getEduLevel()));
            config.setIndustry(resolveLiepinOptionCode("industry", entity.getIndustry()));
            config.setJobKind(resolveLiepinOptionCode("jobType", entity.getJobKind()));
            config.setCompScale(resolveLiepinOptionCode("scale", entity.getCompScale()));
            config.setCompStage(resolveLiepinOptionCode("stage", entity.getCompStage()));
            config.setCompKind(resolveLiepinOptionCode("compKind", entity.getCompKind()));
        }

        return config;
    }

    private String resolveLiepinOptionCode(String type, String value) {
        if (isLiepinUnlimited(value)) {
            return "";
        }
        String trimmed = value.trim();
        String codeByName = liepinService.getCodeByTypeAndName(type, trimmed);
        if (isLiepinUnlimited(codeByName)) {
            return "";
        }
        return (codeByName != null && !codeByName.isEmpty()) ? codeByName : trimmed;
    }

    private boolean isLiepinUnlimited(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "0".equals(trimmed) || "不限".equals(trimmed);
    }

    /**
     * 统一入口：从专表 hub_boss_config 读取并构造 BossConfig
     */
    public BossConfig getBossConfig() {
        return bossService.loadBossConfig();
    }
    
    public BossConfig getBossConfig(Long userId) {
        return bossService.loadBossConfig(userId);
    }

    /**
     * 统一入口：从专表 hub_zhilian_config 读取并构造 ZhilianConfig
     */
    public ZhilianConfig getZhilianConfig() {
        return zhilianService.loadZhilianConfig();
    }
    
    public ZhilianConfig getZhilianConfig(Long userId) {
        return zhilianService.loadZhilianConfig(userId);
    }

    /**
     * 统一入口：从专表 hub_job51_config 读取并构造 Job51Config
     */
    public Job51Config getJob51Config() {
        return job51Service.loadJob51Config();
    }
    
    public Job51Config getJob51Config(Long userId) {
        return job51Service.loadJob51Config(userId);
    }

    public YupaoConfig getYupaoConfig() {
        return yupaoService.loadYupaoConfig();
    }
    
    public YupaoConfig getYupaoConfig(Long userId) {
        return yupaoService.loadYupaoConfig(userId);
    }
}
