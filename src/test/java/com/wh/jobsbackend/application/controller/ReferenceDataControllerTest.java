package com.wh.jobsbackend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.jobsbackend.application.entity.CityEntity;
import com.wh.jobsbackend.application.entity.CityPlatformCodeEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionTypeEntity;
import com.wh.jobsbackend.application.service.ReferenceDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDataControllerTest {
    private MockMvc mockMvc;
    private ReferenceDataService referenceDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        referenceDataService = mock(ReferenceDataService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReferenceDataController(referenceDataService)).build();
    }

    @Test
    void shouldListCities() throws Exception {
        CityEntity city = new CityEntity();
        city.setId(1L);
        city.setName("北京");
        city.setProvince("北京");
        city.setCityCode("beijing");
        city.setEnabled(1);
        when(referenceDataService.listCities(true)).thenReturn(List.of(city));

        mockMvc.perform(get("/api/cities").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("北京"))
                .andExpect(jsonPath("$[0].cityCode").value("beijing"));
    }

    @Test
    void shouldCreateCity() throws Exception {
        CityEntity city = new CityEntity();
        city.setId(2L);
        city.setName("上海");
        city.setCityCode("shanghai");
        when(referenceDataService.createCity(any(CityEntity.class))).thenReturn(city);

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "上海", "cityCode", "shanghai"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("上海"));
    }

    @Test
    void shouldListPlatformCityCodesByPlatform() throws Exception {
        CityPlatformCodeEntity mapping = new CityPlatformCodeEntity();
        mapping.setId(3L);
        mapping.setCityId(1L);
        mapping.setPlatform("boss");
        mapping.setPlatformCityCode("101010100");
        mapping.setPlatformCityName("北京");
        when(referenceDataService.listCityPlatformCodes("boss", true)).thenReturn(List.of(mapping));

        mockMvc.perform(get("/api/city-platform-codes").param("platform", "boss").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform").value("boss"))
                .andExpect(jsonPath("$[0].platformCityCode").value("101010100"));
    }

    @Test
    void shouldManagePlatformOptions() throws Exception {
        PlatformOptionEntity option = new PlatformOptionEntity();
        option.setId(4L);
        option.setPlatform("boss");
        option.setType("salary");
        option.setName("20-30K");
        option.setCode("405");
        when(referenceDataService.listPlatformOptions("boss", "salary", true)).thenReturn(List.of(option));
        when(referenceDataService.updatePlatformOption(eq(4L), any(PlatformOptionEntity.class))).thenReturn(option);
        when(referenceDataService.deletePlatformOption(4L)).thenReturn(true);

        mockMvc.perform(get("/api/platform-options")
                        .param("platform", "boss")
                        .param("type", "salary")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("20-30K"));

        mockMvc.perform(put("/api/platform-options/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "20-30K", "code", "405"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));

        mockMvc.perform(delete("/api/platform-options/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldManagePlatformOptionTypes() throws Exception {
        PlatformOptionTypeEntity type = new PlatformOptionTypeEntity();
        type.setId(5L);
        type.setPlatform("boss");
        type.setType("salary");
        type.setLabel("Salary salary");
        type.setSortOrder(10);
        type.setEnabled(1);
        when(referenceDataService.listPlatformOptionTypes("boss", true)).thenReturn(List.of(type));
        when(referenceDataService.createPlatformOptionType(any(PlatformOptionTypeEntity.class))).thenReturn(type);
        when(referenceDataService.updatePlatformOptionType(eq(5L), any(PlatformOptionTypeEntity.class))).thenReturn(type);
        when(referenceDataService.deletePlatformOptionType(5L)).thenReturn(true);

        mockMvc.perform(get("/api/platform-option-types")
                        .param("platform", "boss")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("salary"))
                .andExpect(jsonPath("$[0].label").value("Salary salary"));

        mockMvc.perform(post("/api/platform-option-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "platform", "boss",
                                "type", "salary",
                                "label", "Salary salary"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        mockMvc.perform(put("/api/platform-option-types/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "platform", "boss",
                                "type", "salary",
                                "label", "Salary salary"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        mockMvc.perform(delete("/api/platform-option-types/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
