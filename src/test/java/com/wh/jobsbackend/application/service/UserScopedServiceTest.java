package com.wh.jobsbackend.application.service;

import com.wh.jobsbackend.application.entity.AiEntity;
import com.wh.jobsbackend.application.entity.ConfigEntity;
import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.entity.LiepinConfigEntity;
import com.wh.jobsbackend.application.mapper.AiMapper;
import com.wh.jobsbackend.application.mapper.ConfigMapper;
import com.wh.jobsbackend.application.mapper.CookieMapper;
import com.wh.jobsbackend.application.mapper.LiepinConfigMapper;
import com.wh.jobsbackend.application.mapper.LiepinMapper;
import com.wh.jobsbackend.application.mapper.LiepinOptionMapper;
import com.wh.jobsbackend.application.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserScopedServiceTest {

    @Test
    void cookieInsertShouldPersistCurrentUserId() {
        CookieMapper cookieMapper = mock(CookieMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(cookieMapper.selectOne(any())).thenReturn(null);
        when(cookieMapper.insert(any(CookieEntity.class))).thenReturn(1);
        CookieService cookieService = new CookieService(cookieMapper, currentUserService);

        boolean saved = cookieService.saveOrUpdateCookie("boss", "cookie-value", "remark");

        ArgumentCaptor<CookieEntity> captor = ArgumentCaptor.forClass(CookieEntity.class);
        verify(cookieMapper).insert(captor.capture());
        assertTrue(saved);
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("boss", captor.getValue().getPlatform());
    }

    @Test
    void createConfigShouldPersistCurrentUserId() {
        ConfigMapper configMapper = mock(ConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(7L);
        when(configMapper.insert(any(ConfigEntity.class))).thenReturn(1);
        ConfigService configService = new ConfigService(
                configMapper,
                currentUserService,
                mock(LiepinService.class),
                mock(BossService.class),
                mock(ZhilianService.class),
                mock(Job51Service.class),
                mock(YupaoService.class)
        );
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setConfigKey("BASE_URL");
        configEntity.setConfigValue("https://example.com");

        boolean created = configService.createConfig(configEntity);

        ArgumentCaptor<ConfigEntity> captor = ArgumentCaptor.forClass(ConfigEntity.class);
        verify(configMapper).insert(captor.capture());
        assertTrue(created);
        assertEquals(7L, captor.getValue().getUserId());
    }

    @Test
    void saveAiConfigShouldPersistCurrentUserIdOnCreate() {
        AiMapper aiMapper = mock(AiMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(9L);
        when(aiMapper.selectList(any())).thenReturn(List.of());
        when(aiMapper.insert(any(AiEntity.class))).thenReturn(1);
        AiService aiService = new AiService(
                mock(ConfigService.class),
                aiMapper,
                currentUserService
        );

        AiEntity entity = aiService.saveOrUpdateAiConfig("intro", "prompt");

        ArgumentCaptor<AiEntity> captor = ArgumentCaptor.forClass(AiEntity.class);
        verify(aiMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals("intro", entity.getIntroduce());
    }

    @Test
    void getAiConfigShouldAllowDefaultConfigCreation() throws Exception {
        Method method = AiService.class.getDeclaredMethod("getAiConfig");
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional == null || !transactional.readOnly());
    }

    @Test
    void getAiConfigsShouldReadByCurrentUser() {
        ConfigMapper configMapper = mock(ConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(11L);
        ConfigEntity baseUrl = new ConfigEntity();
        baseUrl.setUserId(11L);
        baseUrl.setConfigKey("BASE_URL");
        baseUrl.setConfigValue("https://example.com");
        ConfigEntity apiKey = new ConfigEntity();
        apiKey.setUserId(11L);
        apiKey.setConfigKey("API_KEY");
        apiKey.setConfigValue("key");
        ConfigEntity model = new ConfigEntity();
        model.setUserId(11L);
        model.setConfigKey("MODEL");
        model.setConfigValue("gpt-4o-mini");
        when(configMapper.selectOne(any())).thenReturn(baseUrl, apiKey, model);
        ConfigService configService = new ConfigService(
                configMapper,
                currentUserService,
                mock(LiepinService.class),
                mock(BossService.class),
                mock(ZhilianService.class),
                mock(Job51Service.class),
                mock(YupaoService.class)
        );

        Map<String, String> result = configService.getAiConfigs();

        assertEquals("https://example.com", result.get("BASE_URL"));
        assertEquals("key", result.get("API_KEY"));
        assertEquals("gpt-4o-mini", result.get("MODEL"));
    }

    @Test
    void getLiepinConfigShouldReadByCurrentUser() {
        LiepinConfigMapper liepinConfigMapper = mock(LiepinConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        LiepinService liepinService = new LiepinService(
                liepinConfigMapper,
                mock(LiepinOptionMapper.class),
                mock(LiepinMapper.class),
                mock(DataSource.class),
                mock(ReferenceDataService.class),
                currentUserService
        );

        liepinService.getFirstConfig();

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<LiepinConfigEntity>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(liepinConfigMapper).selectOne(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("user_id"), sqlSegment);
    }

    @Test
    void saveLiepinConfigShouldPersistCurrentUserIdOnCreate() {
        LiepinConfigMapper liepinConfigMapper = mock(LiepinConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(84L);
        when(liepinConfigMapper.selectOne(any())).thenReturn(null);
        when(liepinConfigMapper.insert(any(LiepinConfigEntity.class))).thenReturn(1);
        LiepinService liepinService = new LiepinService(
                liepinConfigMapper,
                mock(LiepinOptionMapper.class),
                mock(LiepinMapper.class),
                mock(DataSource.class),
                mock(ReferenceDataService.class),
                currentUserService
        );
        LiepinConfigEntity config = new LiepinConfigEntity();
        config.setKeywords("[\"Java\"]");
        config.setCompTag("500");

        liepinService.saveOrUpdateFirstSelective(config);

        ArgumentCaptor<LiepinConfigEntity> captor = ArgumentCaptor.forClass(LiepinConfigEntity.class);
        verify(liepinConfigMapper).insert(captor.capture());
        assertEquals(84L, captor.getValue().getUserId());
        assertEquals("500", captor.getValue().getCompTag());
    }
}
