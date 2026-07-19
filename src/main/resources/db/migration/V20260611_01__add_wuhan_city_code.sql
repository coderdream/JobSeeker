INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101200100', '武汉', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'wuhan' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101200100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101190100', '南京', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'nanjing' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101190100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101110100', '西安', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'xian' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101110100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101190400', '苏州', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'suzhou' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101190400');
