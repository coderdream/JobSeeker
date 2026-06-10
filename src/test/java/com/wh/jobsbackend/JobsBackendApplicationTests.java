package com.wh.jobsbackend;

import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.startup.enabled=false",
        "app.cookie-seed.enabled=false",
        "app.zhilian-option-init.enabled=false"
})
class JobsBackendApplicationTests {

    @MockBean
    private PlaywrightManager playwrightManager;

    @MockBean
    private CookieService cookieService;

    @MockBean
    private ConfigService configService;

    @Test
    void contextLoads() {
    }

}
