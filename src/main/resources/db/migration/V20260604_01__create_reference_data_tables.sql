CREATE TABLE IF NOT EXISTS city (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    province VARCHAR(100),
    city_code VARCHAR(100) NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS city_platform_code (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT REFERENCES city(id),
    platform VARCHAR(50) NOT NULL,
    platform_city_code VARCHAR(100) NOT NULL,
    platform_city_name VARCHAR(100),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS platform_option (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_city_city_code ON city(city_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_city_platform_code_platform_code ON city_platform_code(platform, platform_city_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_option_platform_type_code ON platform_option(platform, type, code);

INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '全国', '全国', 'all', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'all');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '北京', '北京', 'beijing', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'beijing');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '上海', '上海', 'shanghai', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'shanghai');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '广州', '广东', 'guangzhou', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'guangzhou');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '深圳', '广东', 'shenzhen', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'shenzhen');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '杭州', '浙江', 'hangzhou', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'hangzhou');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '成都', '四川', 'chengdu', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'chengdu');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '南京', '江苏', 'nanjing', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'nanjing');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '武汉', '湖北', 'wuhan', 80, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'wuhan');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '西安', '陕西', 'xian', 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'xian');
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT '苏州', '江苏', 'suzhou', 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM city WHERE city_code = 'suzhou');

INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT DISTINCT o.name, o.name, o.name, COALESCE(o.sort_order, 1000), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM boss_option o
WHERE o.type = 'city' AND o.name IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city c WHERE c.name = o.name);
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT DISTINCT o.name, o.name, o.name, COALESCE(o.sort_order, 1000), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM job51_option o
WHERE o.type = 'jobArea' AND o.name IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city c WHERE c.name = o.name);
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT DISTINCT o.name, o.name, o.name, COALESCE(o.sort_order, 1000), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM liepin_option o
WHERE o.type = 'city' AND o.name IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city c WHERE c.name = o.name);
INSERT INTO city (name, province, city_code, sort_order, enabled, created_at, updated_at)
SELECT DISTINCT o.name, o.name, o.name, COALESCE(o.sort_order, 1000), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM zhilian_option o
WHERE o.type = 'city' AND o.name IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city c WHERE c.name = o.name);

INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', o.code, o.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM boss_option o JOIN city c ON c.name = o.name
WHERE o.type = 'city' AND o.code IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = o.code);
INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, '51job', o.code, o.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM job51_option o JOIN city c ON c.name = o.name
WHERE o.type = 'jobArea' AND o.code IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = '51job' AND x.platform_city_code = o.code);
INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'liepin', o.code, o.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM liepin_option o JOIN city c ON c.name = o.name
WHERE o.type = 'city' AND o.code IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'liepin' AND x.platform_city_code = o.code);
INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'zhilian', o.code, o.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM zhilian_option o JOIN city c ON c.name = o.name
WHERE o.type = 'city' AND o.code IS NOT NULL AND NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'zhilian' AND x.platform_city_code = o.code);

INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM city c JOIN (
    SELECT 'all' city_code, '0' code, '不限' name UNION ALL
    SELECT 'beijing', '101010100', '北京' UNION ALL
    SELECT 'shanghai', '101020100', '上海' UNION ALL
    SELECT 'guangzhou', '101280100', '广州' UNION ALL
    SELECT 'shenzhen', '101280600', '深圳' UNION ALL
    SELECT 'hangzhou', '101210100', '杭州' UNION ALL
    SELECT 'chengdu', '101270100', '成都'
) v ON v.city_code = c.city_code
WHERE NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = v.code);

INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, '51job', v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM city c JOIN (
    SELECT 'all' city_code, '000000' code, '不限' name UNION ALL
    SELECT 'beijing', '010000', '北京' UNION ALL
    SELECT 'shanghai', '020000', '上海' UNION ALL
    SELECT 'guangzhou', '030200', '广州' UNION ALL
    SELECT 'shenzhen', '040000', '深圳' UNION ALL
    SELECT 'hangzhou', '080200', '杭州' UNION ALL
    SELECT 'chengdu', '090200', '成都'
) v ON v.city_code = c.city_code
WHERE NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = '51job' AND x.platform_city_code = v.code);

INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'liepin', v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM city c JOIN (
    SELECT 'all' city_code, '0' code, '不限' name UNION ALL
    SELECT 'beijing', '010', '北京' UNION ALL
    SELECT 'shanghai', '020', '上海' UNION ALL
    SELECT 'guangzhou', '050020', '广州' UNION ALL
    SELECT 'shenzhen', '050090', '深圳' UNION ALL
    SELECT 'hangzhou', '070020', '杭州' UNION ALL
    SELECT 'chengdu', '280020', '成都'
) v ON v.city_code = c.city_code
WHERE NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'liepin' AND x.platform_city_code = v.code);

INSERT INTO city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'zhilian', v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM city c JOIN (
    SELECT 'all' city_code, '0' code, '不限' name UNION ALL
    SELECT 'beijing', '530', '北京' UNION ALL
    SELECT 'shanghai', '538', '上海' UNION ALL
    SELECT 'guangzhou', '763', '广州' UNION ALL
    SELECT 'shenzhen', '765', '深圳' UNION ALL
    SELECT 'hangzhou', '653', '杭州' UNION ALL
    SELECT 'chengdu', '801', '成都'
) v ON v.city_code = c.city_code
WHERE NOT EXISTS (SELECT 1 FROM city_platform_code x WHERE x.platform = 'zhilian' AND x.platform_city_code = v.code);

INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'boss', o.type, o.name, o.code, o.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM boss_option o
WHERE o.type <> 'city' AND o.name IS NOT NULL AND o.code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'boss' AND p.type = o.type AND p.code = o.code);
INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT '51job', 'salary', o.name, o.code, o.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM job51_option o
WHERE o.type = 'salary' AND o.name IS NOT NULL AND o.code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = '51job' AND p.type = 'salary' AND p.code = o.code);
INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'liepin', o.type, o.name, o.code, o.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM liepin_option o
WHERE o.type <> 'city' AND o.name IS NOT NULL AND o.code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'liepin' AND p.type = o.type AND p.code = o.code);
INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'zhilian', o.type, o.name, o.code, o.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM zhilian_option o
WHERE o.type <> 'city' AND o.name IS NOT NULL AND o.code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'zhilian' AND p.type = o.type AND p.code = o.code);

INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'boss', v.type, v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'salary' type, '不限' name, '0' code, 0 sort_order UNION ALL
    SELECT 'salary', '3K以下', '402', 10 UNION ALL
    SELECT 'salary', '3-5K', '403', 20 UNION ALL
    SELECT 'salary', '5-10K', '404', 30 UNION ALL
    SELECT 'salary', '10-20K', '405', 40 UNION ALL
    SELECT 'salary', '20-50K', '406', 50 UNION ALL
    SELECT 'salary', '50K以上', '407', 60 UNION ALL
    SELECT 'industry', '不限', '0', 0 UNION ALL
    SELECT 'experience', '不限', '0', 0 UNION ALL
    SELECT 'jobType', '不限', '0', 0 UNION ALL
    SELECT 'degree', '不限', '0', 0 UNION ALL
    SELECT 'scale', '不限', '0', 0 UNION ALL
    SELECT 'stage', '不限', '0', 0
) v
WHERE NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'boss' AND p.type = v.type AND p.code = v.code);

INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT '51job', 'salary', v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT '不限' name, '00' code, 0 sort_order UNION ALL
    SELECT '3-5千/月', '03', 10 UNION ALL
    SELECT '5-8千/月', '04', 20 UNION ALL
    SELECT '8千-1万/月', '05', 30 UNION ALL
    SELECT '1-1.5万/月', '06', 40 UNION ALL
    SELECT '1.5-2万/月', '07', 50 UNION ALL
    SELECT '2-3万/月', '08', 60
) v
WHERE NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = '51job' AND p.type = 'salary' AND p.code = v.code);

INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'liepin', 'salary', v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT '不限' name, '0' code, 0 sort_order UNION ALL
    SELECT '10-15K', '10$15', 10 UNION ALL
    SELECT '15-20K', '15$20', 20 UNION ALL
    SELECT '20-30K', '20$30', 30 UNION ALL
    SELECT '30-50K', '30$50', 40
) v
WHERE NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'liepin' AND p.type = 'salary' AND p.code = v.code);

INSERT INTO platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'zhilian', 'salary', v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT '不限' name, '0' code, 0 sort_order UNION ALL
    SELECT '10-15K', '10000,15000', 10 UNION ALL
    SELECT '15-20K', '15000,20000', 20 UNION ALL
    SELECT '20-30K', '20000,30000', 30 UNION ALL
    SELECT '30-50K', '30000,50000', 40
) v
WHERE NOT EXISTS (SELECT 1 FROM platform_option p WHERE p.platform = 'zhilian' AND p.type = 'salary' AND p.code = v.code);
