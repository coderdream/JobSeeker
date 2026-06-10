package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wh.jobsbackend.application.entity.CityEntity;
import com.wh.jobsbackend.application.entity.CityPlatformCodeEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionEntity;
import com.wh.jobsbackend.application.entity.PlatformOptionTypeEntity;
import com.wh.jobsbackend.application.mapper.CityMapper;
import com.wh.jobsbackend.application.mapper.CityPlatformCodeMapper;
import com.wh.jobsbackend.application.mapper.PlatformOptionMapper;
import com.wh.jobsbackend.application.mapper.PlatformOptionTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReferenceDataService {

    private final JdbcTemplate jdbcTemplate;
    private final CityMapper cityMapper;
    private final CityPlatformCodeMapper cityPlatformCodeMapper;
    private final PlatformOptionMapper platformOptionMapper;
    private final PlatformOptionTypeMapper platformOptionTypeMapper;

    public ReferenceDataService(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, null, null, null, null);
    }

    @Autowired
    public ReferenceDataService(
            JdbcTemplate jdbcTemplate,
            CityMapper cityMapper,
            CityPlatformCodeMapper cityPlatformCodeMapper,
            PlatformOptionMapper platformOptionMapper,
            PlatformOptionTypeMapper platformOptionTypeMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.cityMapper = cityMapper;
        this.cityPlatformCodeMapper = cityPlatformCodeMapper;
        this.platformOptionMapper = platformOptionMapper;
        this.platformOptionTypeMapper = platformOptionTypeMapper;
    }

    public record OptionItem(Long id, String type, String name, String code, Integer sortOrder) {
    }

    public record OptionTypeItem(Long id, String platform, String type, String label, Integer sortOrder, Integer enabled) {
    }

    public List<CityEntity> listCities(Boolean enabled) {
        if (cityMapper == null) {
            return List.of();
        }
        try {
            QueryWrapper<CityEntity> wrapper = new QueryWrapper<>();
            if (enabled != null) {
                wrapper.eq("enabled", enabled ? 1 : 0);
            }
            wrapper.last("ORDER BY sort_order IS NULL, sort_order ASC, id ASC");
            return cityMapper.selectList(wrapper);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @Transactional
    public CityEntity createCity(CityEntity city) {
        requireMapper(cityMapper, "CityMapper");
        LocalDateTime now = LocalDateTime.now();
        if (city.getEnabled() == null) {
            city.setEnabled(1);
        }
        city.setCreatedAt(now);
        city.setUpdatedAt(now);
        cityMapper.insert(city);
        return city;
    }

    @Transactional
    public CityEntity updateCity(Long id, CityEntity city) {
        requireMapper(cityMapper, "CityMapper");
        city.setId(id);
        city.setUpdatedAt(LocalDateTime.now());
        cityMapper.updateById(city);
        return cityMapper.selectById(id);
    }

    @Transactional
    public boolean deleteCity(Long id) {
        requireMapper(cityMapper, "CityMapper");
        return cityMapper.deleteById(id) > 0;
    }

    public List<CityPlatformCodeEntity> listCityPlatformCodes(String platform, Boolean enabled) {
        if (cityPlatformCodeMapper == null) {
            return List.of();
        }
        try {
            QueryWrapper<CityPlatformCodeEntity> wrapper = new QueryWrapper<>();
            if (!isBlank(platform)) {
                wrapper.eq("platform", platform.trim());
            }
            if (enabled != null) {
                wrapper.eq("enabled", enabled ? 1 : 0);
            }
            wrapper.orderByAsc("platform", "id");
            return cityPlatformCodeMapper.selectList(wrapper);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @Transactional
    public CityPlatformCodeEntity createCityPlatformCode(CityPlatformCodeEntity mapping) {
        requireMapper(cityPlatformCodeMapper, "CityPlatformCodeMapper");
        LocalDateTime now = LocalDateTime.now();
        if (mapping.getEnabled() == null) {
            mapping.setEnabled(1);
        }
        mapping.setCreatedAt(now);
        mapping.setUpdatedAt(now);
        cityPlatformCodeMapper.insert(mapping);
        return mapping;
    }

    @Transactional
    public CityPlatformCodeEntity updateCityPlatformCode(Long id, CityPlatformCodeEntity mapping) {
        requireMapper(cityPlatformCodeMapper, "CityPlatformCodeMapper");
        mapping.setId(id);
        mapping.setUpdatedAt(LocalDateTime.now());
        cityPlatformCodeMapper.updateById(mapping);
        return cityPlatformCodeMapper.selectById(id);
    }

    @Transactional
    public boolean deleteCityPlatformCode(Long id) {
        requireMapper(cityPlatformCodeMapper, "CityPlatformCodeMapper");
        return cityPlatformCodeMapper.deleteById(id) > 0;
    }

    public List<PlatformOptionEntity> listPlatformOptions(String platform, String type, Boolean enabled) {
        if (platformOptionMapper == null) {
            return List.of();
        }
        try {
            QueryWrapper<PlatformOptionEntity> wrapper = new QueryWrapper<>();
            if (!isBlank(platform)) {
                wrapper.eq("platform", platform.trim());
            }
            if (!isBlank(type)) {
                wrapper.eq("type", type.trim());
            }
            if (enabled != null) {
                wrapper.eq("enabled", enabled ? 1 : 0);
            }
            wrapper.last("ORDER BY sort_order IS NULL, sort_order ASC, id ASC");
            return platformOptionMapper.selectList(wrapper);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @Transactional
    public PlatformOptionEntity createPlatformOption(PlatformOptionEntity option) {
        requireMapper(platformOptionMapper, "PlatformOptionMapper");
        LocalDateTime now = LocalDateTime.now();
        if (option.getEnabled() == null) {
            option.setEnabled(1);
        }
        option.setCreatedAt(now);
        option.setUpdatedAt(now);
        platformOptionMapper.insert(option);
        return option;
    }

    @Transactional
    public PlatformOptionEntity updatePlatformOption(Long id, PlatformOptionEntity option) {
        requireMapper(platformOptionMapper, "PlatformOptionMapper");
        option.setId(id);
        option.setUpdatedAt(LocalDateTime.now());
        platformOptionMapper.updateById(option);
        return platformOptionMapper.selectById(id);
    }

    @Transactional
    public boolean deletePlatformOption(Long id) {
        requireMapper(platformOptionMapper, "PlatformOptionMapper");
        return platformOptionMapper.deleteById(id) > 0;
    }

    public List<PlatformOptionTypeEntity> listPlatformOptionTypes(String platform, Boolean enabled) {
        if (platformOptionTypeMapper == null) {
            return listPlatformOptionTypesFromJdbc(platform, enabled).stream()
                    .map(this::toPlatformOptionTypeEntity)
                    .toList();
        }
        try {
            QueryWrapper<PlatformOptionTypeEntity> wrapper = new QueryWrapper<>();
            if (!isBlank(platform)) {
                wrapper.eq("platform", platform.trim());
            }
            if (enabled != null) {
                wrapper.eq("enabled", enabled ? 1 : 0);
            }
            wrapper.last("ORDER BY sort_order IS NULL, sort_order ASC, id ASC");
            return platformOptionTypeMapper.selectList(wrapper);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @Transactional
    public PlatformOptionTypeEntity createPlatformOptionType(PlatformOptionTypeEntity type) {
        requireMapper(platformOptionTypeMapper, "PlatformOptionTypeMapper");
        normalizePlatformOptionType(type);
        LocalDateTime now = LocalDateTime.now();
        if (type.getEnabled() == null) {
            type.setEnabled(1);
        }
        type.setCreatedAt(now);
        type.setUpdatedAt(now);
        platformOptionTypeMapper.insert(type);
        return type;
    }

    @Transactional
    public PlatformOptionTypeEntity updatePlatformOptionType(Long id, PlatformOptionTypeEntity type) {
        requireMapper(platformOptionTypeMapper, "PlatformOptionTypeMapper");
        normalizePlatformOptionType(type);
        type.setId(id);
        type.setUpdatedAt(LocalDateTime.now());
        platformOptionTypeMapper.updateById(type);
        return platformOptionTypeMapper.selectById(id);
    }

    @Transactional
    public boolean deletePlatformOptionType(Long id) {
        requireMapper(platformOptionTypeMapper, "PlatformOptionTypeMapper");
        return platformOptionTypeMapper.deleteById(id) > 0;
    }

    public List<OptionItem> listCityOptionsForPlatform(String platform) {
        if (isBlank(platform)) {
            return List.of();
        }
        List<OptionItem> mapperItems = listCityOptionsFromMappers(platform.trim());
        return mapperItems.isEmpty() ? listCityOptionsFromJdbc(platform.trim()) : mapperItems;
    }

    public List<OptionItem> listPlatformOptionItems(String platform, String type) {
        if (isBlank(platform) || isBlank(type)) {
            return List.of();
        }
        List<OptionItem> mapperItems = listPlatformOptions(platform, type, true).stream()
                .map(row -> new OptionItem(row.getId(), row.getType(), row.getName(), row.getCode(), row.getSortOrder()))
                .toList();
        return mapperItems.isEmpty() ? listPlatformOptionsFromJdbc(platform.trim(), type.trim()) : mapperItems;
    }

    public String codeByName(String platform, String type, String name) {
        if (isBlank(name)) {
            return null;
        }
        return optionItems(platform, type).stream()
                .filter(item -> name.trim().equals(item.name()))
                .map(OptionItem::code)
                .findFirst()
                .orElse(null);
    }

    public String nameByCode(String platform, String type, String code) {
        if (isBlank(code)) {
            return null;
        }
        return optionItems(platform, type).stream()
                .filter(item -> code.trim().equals(item.code()))
                .map(OptionItem::name)
                .findFirst()
                .orElse(null);
    }

    private List<OptionItem> optionItems(String platform, String type) {
        if ("city".equals(type)) {
            return listCityOptionsForPlatform(platform);
        }
        return listPlatformOptionItems(platform, type);
    }

    private List<OptionItem> listCityOptionsFromMappers(String platform) {
        if (cityMapper == null || cityPlatformCodeMapper == null) {
            return List.of();
        }
        try {
            List<OptionItem> result = new ArrayList<>();
            for (CityPlatformCodeEntity mapping : listCityPlatformCodes(platform, true)) {
                CityEntity city = cityMapper.selectById(mapping.getCityId());
                if (city == null || city.getEnabled() == null || city.getEnabled() == 0) {
                    continue;
                }
                String name = isBlank(mapping.getPlatformCityName()) ? city.getName() : mapping.getPlatformCityName();
                result.add(new OptionItem(mapping.getId(), "city", name, mapping.getPlatformCityCode(), city.getSortOrder()));
            }
            result.sort(this::compareBySortOrderThenId);
            return result;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<OptionItem> listCityOptionsFromJdbc(String platform) {
        return queryOptions("""
                SELECT cpc.id,
                       'city' AS type,
                       COALESCE(NULLIF(cpc.platform_city_name, ''), c.name) AS name,
                       cpc.platform_city_code AS code,
                       COALESCE(c.sort_order, 0) AS sort_order
                  FROM hub_city_platform_code cpc
                  JOIN hub_city c ON c.id = cpc.city_id
                 WHERE cpc.platform = ?
                   AND cpc.enabled = 1
                   AND c.enabled = 1
                 ORDER BY c.sort_order IS NULL, c.sort_order ASC, c.id ASC
                """, platform);
    }

    private List<OptionItem> listPlatformOptionsFromJdbc(String platform, String type) {
        return queryOptions("""
                SELECT id,
                       type,
                       name,
                       code,
                       COALESCE(sort_order, 0) AS sort_order
                  FROM hub_platform_option
                 WHERE platform = ?
                   AND type = ?
                   AND enabled = 1
                 ORDER BY sort_order IS NULL, sort_order ASC, id ASC
                """, platform, type);
    }

    private List<OptionTypeItem> listPlatformOptionTypesFromJdbc(String platform, Boolean enabled) {
        StringBuilder sql = new StringBuilder("""
                SELECT id,
                       platform,
                       type,
                       label,
                       COALESCE(sort_order, 0) AS sort_order,
                       enabled
                  FROM hub_platform_option_type
                 WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (!isBlank(platform)) {
            sql.append(" AND platform = ?");
            args.add(platform.trim());
        }
        if (enabled != null) {
            sql.append(" AND enabled = ?");
            args.add(enabled ? 1 : 0);
        }
        sql.append(" ORDER BY sort_order IS NULL, sort_order ASC, id ASC");
        try {
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new OptionTypeItem(
                    rs.getLong("id"),
                    rs.getString("platform"),
                    rs.getString("type"),
                    rs.getString("label"),
                    rs.getInt("sort_order"),
                    rs.getInt("enabled")
            ), args.toArray());
        } catch (DataAccessException ignored) {
            return List.of();
        }
    }

    private PlatformOptionTypeEntity toPlatformOptionTypeEntity(OptionTypeItem item) {
        PlatformOptionTypeEntity entity = new PlatformOptionTypeEntity();
        entity.setId(item.id());
        entity.setPlatform(item.platform());
        entity.setType(item.type());
        entity.setLabel(item.label());
        entity.setSortOrder(item.sortOrder());
        entity.setEnabled(item.enabled());
        return entity;
    }

    private void normalizePlatformOptionType(PlatformOptionTypeEntity type) {
        if (type == null) {
            return;
        }
        if (!isBlank(type.getPlatform())) {
            type.setPlatform(type.getPlatform().trim());
        }
        if (!isBlank(type.getType())) {
            type.setType(type.getType().trim());
        }
        if (isBlank(type.getLabel()) && !isBlank(type.getType())) {
            type.setLabel(type.getType().trim());
        } else if (!isBlank(type.getLabel())) {
            type.setLabel(type.getLabel().trim());
        }
    }

    private List<OptionItem> queryOptions(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new OptionItem(
                    rs.getLong("id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getInt("sort_order")
            ), args);
        } catch (DataAccessException ignored) {
            return List.of();
        }
    }

    private int compareBySortOrderThenId(OptionItem left, OptionItem right) {
        int leftOrder = left.sortOrder() == null ? Integer.MAX_VALUE : left.sortOrder();
        int rightOrder = right.sortOrder() == null ? Integer.MAX_VALUE : right.sortOrder();
        int orderCompare = Integer.compare(leftOrder, rightOrder);
        if (orderCompare != 0) {
            return orderCompare;
        }
        return Long.compare(left.id() == null ? 0L : left.id(), right.id() == null ? 0L : right.id());
    }

    private void requireMapper(Object mapper, String mapperName) {
        if (mapper == null) {
            throw new IllegalStateException(mapperName + " is not available");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
