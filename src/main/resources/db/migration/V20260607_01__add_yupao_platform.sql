CREATE TABLE IF NOT EXISTS hub_yupao_config (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES hub_user(id),
    keywords TEXT,
    city_code VARCHAR(100),
    salary VARCHAR(100),
    job_type VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hub_yupao_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES hub_user(id),
    job_id VARCHAR(128),
    job_title VARCHAR(200),
    job_link VARCHAR(500),
    salary VARCHAR(100),
    location VARCHAR(100),
    experience VARCHAR(100),
    degree VARCHAR(100),
    company_name VARCHAR(200),
    hr_name VARCHAR(100),
    delivery_status VARCHAR(50),
    publish_time VARCHAR(100),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_config_user ON hub_yupao_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_data_user_job ON hub_yupao_data(user_id, job_id);
CREATE INDEX IF NOT EXISTS idx_yupao_data_user_status ON hub_yupao_data(user_id, delivery_status);

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'yupao', v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_city c JOIN (
    SELECT 'all' city_code, 'all' code, '不限' name UNION ALL
    SELECT 'beijing', 'beijing', '北京' UNION ALL
    SELECT 'shanghai', 'shanghai', '上海' UNION ALL
    SELECT 'guangzhou', 'guangzhou', '广州' UNION ALL
    SELECT 'shenzhen', 'shenzhen', '深圳' UNION ALL
    SELECT 'hangzhou', 'hangzhou', '杭州' UNION ALL
    SELECT 'chengdu', 'chengdu', '成都' UNION ALL
    SELECT 'nanjing', 'nanjing', '南京' UNION ALL
    SELECT 'wuhan', 'wuhan', '武汉' UNION ALL
    SELECT 'xian', 'xian', '西安' UNION ALL
    SELECT 'suzhou', 'suzhou', '苏州'
) v ON v.city_code = c.city_code
WHERE NOT EXISTS (
    SELECT 1 FROM hub_city_platform_code x
    WHERE x.platform = 'yupao' AND x.platform_city_code = v.code
);

INSERT INTO hub_platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT 'yupao', v.type, v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'salary' type, '不限' name, '' code, 0 sort_order UNION ALL
    SELECT 'salary', '3-5K', '3-5K', 10 UNION ALL
    SELECT 'salary', '5-8K', '5-8K', 20 UNION ALL
    SELECT 'salary', '8-10K', '8-10K', 30 UNION ALL
    SELECT 'salary', '10-15K', '10-15K', 40 UNION ALL
    SELECT 'salary', '15-20K', '15-20K', 50 UNION ALL
    SELECT 'salary', '20-30K', '20-30K', 60 UNION ALL
    SELECT 'salary', '30K以上', '30K以上', 70 UNION ALL
    SELECT 'jobType', '不限', '' , 0 UNION ALL
    SELECT 'jobType', '全职', 'fulltime', 10 UNION ALL
    SELECT 'jobType', '兼职', 'parttime', 20 UNION ALL
    SELECT 'jobType', '临时工', 'temporary', 30
) v
WHERE NOT EXISTS (
    SELECT 1 FROM hub_platform_option p
    WHERE p.platform = 'yupao' AND p.type = v.type AND p.code = v.code
);
