package com.wh.jobsbackend.application.config;

import com.wh.jobsbackend.application.dto.LoginRequest;
import com.wh.jobsbackend.application.dto.RegisterRequest;
import com.wh.jobsbackend.application.entity.BossConfigEntity;
import com.wh.jobsbackend.application.entity.ConfigEntity;
import com.wh.jobsbackend.application.entity.Job51ConfigEntity;
import com.wh.jobsbackend.application.entity.LiepinConfigEntity;
import com.wh.jobsbackend.application.entity.YupaoConfigEntity;
import com.wh.jobsbackend.application.entity.ZhilianConfigEntity;
import com.wh.jobsbackend.application.security.AppUserPrincipal;
import com.wh.jobsbackend.application.service.AiService;
import com.wh.jobsbackend.application.service.AuthService;
import com.wh.jobsbackend.application.service.BossService;
import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.Job51Service;
import com.wh.jobsbackend.application.service.LiepinService;
import com.wh.jobsbackend.application.service.ReferenceDataService;
import com.wh.jobsbackend.application.service.YupaoService;
import com.wh.jobsbackend.application.service.ZhilianService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.username=",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "app.startup.enabled=false",
        "app.cookie-seed.enabled=false",
        "app.zhilian-option-init.enabled=false"
})
@ActiveProfiles("portable")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortableSqliteBusinessFlowTest {

    @DynamicPropertySource
    static void portableSqliteProperties(DynamicPropertyRegistry registry) throws Exception {
        Path databasePath = Path.of("target", "portable-business-" + UUID.randomUUID() + ".db").toAbsolutePath();
        Files.createDirectories(databasePath.getParent());
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databasePath);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AiService aiService;

    @Autowired
    private ReferenceDataService referenceDataService;

    @Autowired
    private BossService bossService;

    @Autowired
    private Job51Service job51Service;

    @Autowired
    private LiepinService liepinService;

    @Autowired
    private ZhilianService zhilianService;

    @Autowired
    private YupaoService yupaoService;

    @MockBean
    private PlaywrightManager playwrightManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void portableDatabaseShouldSupportAuthReferenceConfigAiAndPlatformConfigFlows() {
        String username = "portable_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var registerResponse = authService.register(new RegisterRequest(
                username,
                "Portable User",
                "Password123",
                "Password123"
        ));

        assertTrue(registerResponse.success());
        assertNotNull(authService.login(new LoginRequest(username, "Password123")).token());
        authenticate(registerResponse.id(), username);

        assertFalse(referenceDataService.listCities(true).isEmpty());
        assertFalse(referenceDataService.listCityOptionsForPlatform("boss").isEmpty());
        assertFalse(referenceDataService.listPlatformOptionItems("51job", "salary").isEmpty());

        ConfigEntity baseUrl = new ConfigEntity();
        baseUrl.setConfigKey("BASE_URL");
        baseUrl.setConfigValue("http://localhost:9999");
        baseUrl.setConfigType("string");
        baseUrl.setCategory("ai");
        assertTrue(configService.createConfig(baseUrl));
        assertEquals("http://localhost:9999", configService.getConfigValue("BASE_URL"));
        assertTrue(configService.updateConfig("BASE_URL", "http://localhost:9998"));
        assertEquals("http://localhost:9998", configService.getAllConfigsAsMap().get("BASE_URL"));

        assertNotNull(aiService.getAiConfig());
        assertEquals("intro", aiService.saveOrUpdateAiConfig("intro", "prompt").getIntroduce());

        BossConfigEntity bossConfig = new BossConfigEntity();
        bossConfig.setKeywords("[\"Java\"]");
        bossConfig.setCityCode("[\"北京\"]");
        bossConfig.setSalary("[\"10-20K\"]");
        BossConfigEntity savedConfig = bossService.saveOrUpdateFirstSelective(1L, bossConfig);
        assertDoesNotThrow(() -> { bossService.getOptionsByType("salary"); });
        assertDoesNotThrow(() -> { bossService.loadBossConfig(); });

        Job51ConfigEntity job51Config = new Job51ConfigEntity();
        job51Config.setKeywords("[\"Java\"]");
        job51Config.setJobArea("[\"北京\"]");
        job51Config.setSalary("[\"10-20K\"]");
        assertNotNull(job51Service.updateConfig(job51Config));
        assertDoesNotThrow(() -> { job51Service.getOptionsByType("salary"); });
        assertDoesNotThrow(() -> { job51Service.loadJob51Config(); });

        LiepinConfigEntity liepinConfig = new LiepinConfigEntity();
        liepinConfig.setKeywords("[\"Java\"]");
        liepinConfig.setCity("北京");
        liepinConfig.setSalaryCode("10$15");
        assertNotNull(liepinService.updateConfig(liepinConfig));
        assertDoesNotThrow(() -> liepinService.getOptionsByType("salary"));
        assertDoesNotThrow(() -> configService.getLiepinConfig(registerResponse.id()));

        ZhilianConfigEntity zhilianConfig = new ZhilianConfigEntity();
        zhilianConfig.setKeywords("[\"Java\"]");
        zhilianConfig.setCityCode("北京");
        zhilianConfig.setSalary("10000,15000");
        assertNotNull(zhilianService.updateConfig(zhilianConfig));
        assertDoesNotThrow(() -> { zhilianService.getOptionsByType("salary"); });
        assertDoesNotThrow(() -> { zhilianService.loadZhilianConfig(); });

        YupaoConfigEntity yupaoConfig = new YupaoConfigEntity();
        yupaoConfig.setKeywords("[\"Java\"]");
        yupaoConfig.setCityCode("北京");
        yupaoConfig.setSalary("5-8K");
        yupaoConfig.setJobType("fulltime");
        assertNotNull(yupaoService.updateConfig(yupaoConfig));
        assertDoesNotThrow(() -> { yupaoService.getOptionsByType("salary"); });
        assertDoesNotThrow(() -> { yupaoService.loadYupaoConfig(); });
    }

    private void authenticate(Long userId, String username) {
        AppUserPrincipal principal = new AppUserPrincipal(userId, username, "n/a", "USER", "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
    }
}
