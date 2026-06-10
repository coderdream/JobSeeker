package com.wh.jobsbackend.application.service;

import com.wh.jobsbackend.application.entity.YupaoConfigEntity;
import com.wh.jobsbackend.application.entity.YupaoJobDataEntity;
import com.wh.jobsbackend.application.mapper.YupaoConfigMapper;
import com.wh.jobsbackend.application.mapper.YupaoJobDataMapper;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.worker.yupao.YupaoConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YupaoServiceTest {

    @Test
    void saveOrUpdateFirstSelectiveShouldPersistCurrentUserOnCreate() {
        YupaoConfigMapper configMapper = mock(YupaoConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(42L);
        when(configMapper.selectOne(any())).thenReturn(null);
        when(configMapper.insert(any(YupaoConfigEntity.class))).thenReturn(1);
        YupaoService service = new YupaoService(
                configMapper,
                mock(YupaoJobDataMapper.class),
                mock(DataSource.class),
                mock(ReferenceDataService.class),
                currentUserService
        );
        YupaoConfigEntity incoming = new YupaoConfigEntity();
        incoming.setKeywords("[\"Java\"]");
        incoming.setCityCode("beijing");

        service.saveOrUpdateFirstSelective(incoming);

        ArgumentCaptor<YupaoConfigEntity> captor = ArgumentCaptor.forClass(YupaoConfigEntity.class);
        verify(configMapper).insert(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("beijing", captor.getValue().getCityCode());
    }

    @Test
    void loadYupaoConfigShouldMapStoredValuesToWorkerConfig() {
        YupaoConfigMapper configMapper = mock(YupaoConfigMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ReferenceDataService referenceDataService = mock(ReferenceDataService.class);
        when(currentUserService.requireUserId()).thenReturn(7L);
        YupaoConfigEntity entity = new YupaoConfigEntity();
        entity.setKeywords("[\"Java\",\"后端\"]");
        entity.setCityCode("北京");
        entity.setSalary("10-15K");
        entity.setJobType("全职");
        when(configMapper.selectOne(any())).thenReturn(entity);
        when(referenceDataService.codeByName("yupao", "city", "北京")).thenReturn("beijing");
        when(referenceDataService.codeByName("yupao", "jobType", "全职")).thenReturn("fulltime");
        YupaoService service = new YupaoService(
                configMapper,
                mock(YupaoJobDataMapper.class),
                mock(DataSource.class),
                referenceDataService,
                currentUserService
        );

        YupaoConfig config = service.loadYupaoConfig();

        assertEquals(List.of("Java", "后端"), config.getKeywords());
        assertEquals("beijing", config.getCityCode());
        assertEquals("10-15K", config.getSalary());
        assertEquals("fulltime", config.getJobType());
    }

    @Test
    void insertJobShouldPersistCurrentUserIdAndDefaultStatus() {
        YupaoJobDataMapper jobDataMapper = mock(YupaoJobDataMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(11L);
        YupaoService service = new YupaoService(
                mock(YupaoConfigMapper.class),
                jobDataMapper,
                mock(DataSource.class),
                mock(ReferenceDataService.class),
                currentUserService
        );
        YupaoJobDataEntity job = new YupaoJobDataEntity();
        job.setJobId("job-1");
        job.setJobTitle("Java 后端");

        service.insertJob(job);

        ArgumentCaptor<YupaoJobDataEntity> captor = ArgumentCaptor.forClass(YupaoJobDataEntity.class);
        verify(jobDataMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getUserId());
        assertEquals("未投递", captor.getValue().getDeliveryStatus());
        assertTrue(captor.getValue().getCreateTime() != null);
    }
}
