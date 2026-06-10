package com.wh.jobsbackend.application.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wh.jobsbackend.application.entity.ZhilianOptionEntity;
import com.wh.jobsbackend.application.mapper.ZhilianOptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
 

/**
 * 应用启动时：
 * 1) 创建 hub_zhilian_option 表（若不存在）
 * 2) 从 city.json 解析城市码并插入（若不存在）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.zhilian-option-init", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ZhilianOptionInitializer implements CommandLineRunner {

    private final ZhilianOptionMapper zhilianOptionMapper;

    @Override
    public void run(String... args) throws Exception {
        log.debug("hub_zhilian_option schema is managed by Flyway");
        // 已改为从数据库获取城市选项，移除基于 city.json 的种子插入逻辑
    }

    // 已移除基于 city.json 的种子插入方法
    private void insertIfNotExists(String type, String name, String code, int sortOrder) {
        try {
            ZhilianOptionEntity existing = zhilianOptionMapper.selectOne(
                    new QueryWrapper<ZhilianOptionEntity>()
                            .eq("type", type)
                            .eq("code", code)
                            .last("LIMIT 1")
            );
            if (existing != null) return;

            LocalDateTime now = LocalDateTime.now();
            ZhilianOptionEntity e = new ZhilianOptionEntity();
            e.setType(type);
            e.setName(name);
            e.setCode(code);
            e.setSortOrder(sortOrder);
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            zhilianOptionMapper.insert(e);
        } catch (Exception ex) {
            log.warn("插入城市选项失败 code={}: {}", code, ex.getMessage());
        }
    }

    // 已移除 CityItem 记录类型
}
