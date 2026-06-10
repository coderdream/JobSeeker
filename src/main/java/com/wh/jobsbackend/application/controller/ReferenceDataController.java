package com.wh.jobsbackend.application.controller;

import com.wh.jobsbackend.application.entity.CityEntity;
import com.wh.jobsbackend.application.entity.CityPlatformCodeEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionTypeEntity;
import com.wh.jobsbackend.application.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReferenceDataController {
    private final ReferenceDataService referenceDataService;

    @GetMapping("/api/cities")
    public List<CityEntity> listCities(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return referenceDataService.listCities(enabled);
    }

    @PostMapping("/api/cities")
    public CityEntity createCity(@RequestBody CityEntity city) {
        return referenceDataService.createCity(city);
    }

    @PutMapping("/api/cities/{id}")
    public CityEntity updateCity(@PathVariable Long id, @RequestBody CityEntity city) {
        return referenceDataService.updateCity(id, city);
    }

    @DeleteMapping("/api/cities/{id}")
    public Map<String, Object> deleteCity(@PathVariable Long id) {
        return Map.of("success", referenceDataService.deleteCity(id));
    }

    @GetMapping("/api/city-platform-codes")
    public List<CityPlatformCodeEntity> listCityPlatformCodes(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return referenceDataService.listCityPlatformCodes(platform, enabled);
    }

    @PostMapping("/api/city-platform-codes")
    public CityPlatformCodeEntity createCityPlatformCode(@RequestBody CityPlatformCodeEntity mapping) {
        return referenceDataService.createCityPlatformCode(mapping);
    }

    @PutMapping("/api/city-platform-codes/{id}")
    public CityPlatformCodeEntity updateCityPlatformCode(@PathVariable Long id, @RequestBody CityPlatformCodeEntity mapping) {
        return referenceDataService.updateCityPlatformCode(id, mapping);
    }

    @DeleteMapping("/api/city-platform-codes/{id}")
    public Map<String, Object> deleteCityPlatformCode(@PathVariable Long id) {
        return Map.of("success", referenceDataService.deleteCityPlatformCode(id));
    }

    @GetMapping("/api/platform-options")
    public List<PlatformOptionEntity> listPlatformOptions(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return referenceDataService.listPlatformOptions(platform, type, enabled);
    }

    @PostMapping("/api/platform-options")
    public PlatformOptionEntity createPlatformOption(@RequestBody PlatformOptionEntity option) {
        return referenceDataService.createPlatformOption(option);
    }

    @PutMapping("/api/platform-options/{id}")
    public PlatformOptionEntity updatePlatformOption(@PathVariable Long id, @RequestBody PlatformOptionEntity option) {
        return referenceDataService.updatePlatformOption(id, option);
    }

    @DeleteMapping("/api/platform-options/{id}")
    public Map<String, Object> deletePlatformOption(@PathVariable Long id) {
        return Map.of("success", referenceDataService.deletePlatformOption(id));
    }

    @GetMapping("/api/platform-option-types")
    public List<PlatformOptionTypeEntity> listPlatformOptionTypes(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return referenceDataService.listPlatformOptionTypes(platform, enabled);
    }

    @PostMapping("/api/platform-option-types")
    public PlatformOptionTypeEntity createPlatformOptionType(@RequestBody PlatformOptionTypeEntity type) {
        return referenceDataService.createPlatformOptionType(type);
    }

    @PutMapping("/api/platform-option-types/{id}")
    public PlatformOptionTypeEntity updatePlatformOptionType(@PathVariable Long id, @RequestBody PlatformOptionTypeEntity type) {
        return referenceDataService.updatePlatformOptionType(id, type);
    }

    @DeleteMapping("/api/platform-option-types/{id}")
    public Map<String, Object> deletePlatformOptionType(@PathVariable Long id) {
        return Map.of("success", referenceDataService.deletePlatformOptionType(id));
    }
}
