CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_job_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    platform VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(512),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_job_task_user_platform ON user_job_task(user_id, platform);

ALTER TABLE IF EXISTS cookie ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS ai ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS liepin_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS zhilian_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS job51_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_blacklist ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS liepin_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS zhilian_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS job51_data ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE IF EXISTS liepin_data ADD COLUMN IF NOT EXISTS id INTEGER PRIMARY KEY AUTOINCREMENT;
ALTER TABLE IF EXISTS job51_data ADD COLUMN IF NOT EXISTS id INTEGER PRIMARY KEY AUTOINCREMENT;











INSERT INTO app_user(username, password_hash, nickname, role, status)
SELECT
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '默认管理员',
    'ADMIN',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE username = 'admin'
);



CREATE UNIQUE INDEX IF NOT EXISTS uk_user_job_task_running
    ON user_job_task(user_id, platform)
    WHERE status = 'RUNNING';

CREATE TABLE IF NOT EXISTS job51_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job51_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    job_id BIGINT,
    job_title VARCHAR(200),
    job_link VARCHAR(300),
    job_salary_text VARCHAR(100),
    job_area VARCHAR(100),
    job_edu_req VARCHAR(50),
    job_exp_req VARCHAR(50),
    job_publish_time VARCHAR(50),
    comp_id BIGINT,
    comp_name VARCHAR(200),
    comp_industry VARCHAR(100),
    comp_scale VARCHAR(50),
    hr_id VARCHAR(64),
    hr_name VARCHAR(50),
    hr_title VARCHAR(100),
    delivered INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zhilian_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zhilian_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    job_id VARCHAR(64),
    job_title VARCHAR(200),
    job_link VARCHAR(300),
    salary VARCHAR(100),
    location VARCHAR(100),
    experience VARCHAR(100),
    degree VARCHAR(100),
    company_name VARCHAR(200),
    delivery_status VARCHAR(20) DEFAULT '未投递',
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS liepin_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    job_id BIGINT,
    job_title VARCHAR(200),
    job_link VARCHAR(300),
    job_salary_text VARCHAR(100),
    job_area VARCHAR(100),
    job_edu_req VARCHAR(50),
    job_exp_req VARCHAR(50),
    job_publish_time VARCHAR(50),
    comp_id BIGINT,
    comp_name VARCHAR(200),
    comp_industry VARCHAR(100),
    comp_scale VARCHAR(50),
    hr_id VARCHAR(64),
    hr_name VARCHAR(50),
    hr_title VARCHAR(100),
    hr_im_id VARCHAR(64),
    delivered INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50),
    category VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    platform VARCHAR(50),
    cookie_value TEXT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    introduce TEXT,
    prompt TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS boss_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    debugger INTEGER,
    wait_time INTEGER,
    keywords TEXT,
    city_code TEXT,
    industry TEXT,
    job_type TEXT,
    experience TEXT,
    degree TEXT,
    salary TEXT,
    scale TEXT,
    stage TEXT,
    say_hi TEXT,
    expected_salary_min INTEGER,
    expected_salary_max INTEGER,
    enable_ai INTEGER,
    send_img_resume INTEGER,
    filter_dead_hr INTEGER,
    dead_status TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job51_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    keywords TEXT,
    job_area TEXT,
    salary TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zhilian_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    keywords TEXT,
    city_code TEXT,
    salary TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS liepin_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    keywords TEXT,
    city TEXT,
    salary_code TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS boss_blacklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    type VARCHAR(50),
    "value" TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS boss_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES app_user(id),
    encrypt_id VARCHAR(128),
    encrypt_user_id VARCHAR(128),
    company_name VARCHAR(200),
    job_name VARCHAR(200),
    salary VARCHAR(100),
    location VARCHAR(100),
    experience VARCHAR(100),
    degree VARCHAR(100),
    hr_name VARCHAR(100),
    hr_position VARCHAR(100),
    hr_active_status VARCHAR(100),
    delivery_status VARCHAR(50),
    job_description TEXT,
    job_url TEXT,
    recruitment_status VARCHAR(100),
    company_address TEXT,
    industry VARCHAR(200),
    introduce TEXT,
    financing_stage VARCHAR(100),
    company_scale VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS boss_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS boss_industry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100),
    code INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS liepin_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE IF EXISTS config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS cookie ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS ai ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS job51_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS zhilian_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS liepin_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_blacklist ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_data ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_config_user_key ON config(user_id, config_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cookie_user_platform ON cookie(user_id, platform);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_user ON ai(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_config_user ON boss_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_job51_config_user ON job51_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_zhilian_config_user ON zhilian_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_config_user ON liepin_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_data_user_encrypt ON boss_data(user_id, encrypt_id, encrypt_user_id);

CREATE TABLE IF NOT EXISTS city (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    province VARCHAR(100),
    city_code VARCHAR(100) NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS city_platform_code (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    city_id BIGINT REFERENCES city(id),
    platform VARCHAR(50) NOT NULL,
    platform_city_code VARCHAR(100) NOT NULL,
    platform_city_name VARCHAR(100),
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS platform_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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



CREATE TABLE IF NOT EXISTS hub_yupao_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT REFERENCES hub_user(id),
    keywords TEXT,
    city_code VARCHAR(100),
    salary VARCHAR(100),
    job_type VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hub_yupao_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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

ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS comp_tag VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS pub_time VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS work_year_code VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS edu_level VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS industry VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS job_kind VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS comp_scale VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS comp_stage VARCHAR(100);
ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS comp_kind VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_config_user ON hub_liepin_config(user_id);

INSERT INTO hub_platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at)
SELECT v.platform, v.type, v.name, v.code, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'liepin' platform, 'compTag' type, '不限' name, '0' code, 0 sort_order UNION ALL
    SELECT 'liepin', 'compTag', '名企', '500', 10 UNION ALL
    SELECT 'liepin', 'pubTime', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'pubTime', '近三天', '3', 10 UNION ALL
    SELECT 'liepin', 'pubTime', '近一周', '7', 20 UNION ALL
    SELECT 'liepin', 'pubTime', '近一月', '30', 30 UNION ALL
    SELECT 'liepin', 'workYearCode', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'workYearCode', '1年以内', '0$1', 10 UNION ALL
    SELECT 'liepin', 'workYearCode', '1-3年', '1$3', 20 UNION ALL
    SELECT 'liepin', 'workYearCode', '3-5年', '2$5', 30 UNION ALL
    SELECT 'liepin', 'workYearCode', '5-10年', '5$10', 40 UNION ALL
    SELECT 'liepin', 'degree', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'degree', '大专', '030', 10 UNION ALL
    SELECT 'liepin', 'degree', '本科', '040', 20 UNION ALL
    SELECT 'liepin', 'degree', '硕士', '050', 30 UNION ALL
    SELECT 'liepin', 'industry', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'industry', '互联网/IT', '040', 10 UNION ALL
    SELECT 'liepin', 'jobType', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'jobType', '全职', '2', 10 UNION ALL
    SELECT 'liepin', 'scale', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'scale', '100-499人', '050', 10 UNION ALL
    SELECT 'liepin', 'stage', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'stage', 'A轮', '020', 10 UNION ALL
    SELECT 'liepin', 'compKind', '不限', '0', 0 UNION ALL
    SELECT 'liepin', 'compKind', '民营', '010', 10
) v
WHERE NOT EXISTS (
    SELECT 1
    FROM hub_platform_option p
    WHERE p.platform = v.platform
      AND p.type = v.type
      AND p.code = v.code
);

CREATE TABLE IF NOT EXISTS hub_platform_option_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_option_type_platform_type
    ON hub_platform_option_type(platform, type);

INSERT INTO hub_platform_option_type (platform, type, label, sort_order, enabled, created_at, updated_at)
SELECT v.platform, v.type, v.label, v.sort_order, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'boss' platform, 'salary' type, '薪资 salary' label, 10 sort_order UNION ALL
    SELECT 'boss', 'industry', '行业 industry', 20 UNION ALL
    SELECT 'boss', 'experience', '经验 experience', 30 UNION ALL
    SELECT 'boss', 'jobType', '职位类型 jobType', 40 UNION ALL
    SELECT 'boss', 'degree', '学历 degree', 50 UNION ALL
    SELECT 'boss', 'scale', '公司规模 scale', 60 UNION ALL
    SELECT 'boss', 'stage', '融资阶段 stage', 70 UNION ALL
    SELECT '51job', 'salary', '薪资 salary', 10 UNION ALL
    SELECT '51job', 'jobArea', '城市区域 jobArea', 20 UNION ALL
    SELECT 'liepin', 'salary', '薪资 salary', 10 UNION ALL
    SELECT 'liepin', 'compTag', '名企 compTag', 20 UNION ALL
    SELECT 'liepin', 'pubTime', '招聘者活跃 pubTime', 30 UNION ALL
    SELECT 'liepin', 'workYearCode', '工作年限 workYearCode', 40 UNION ALL
    SELECT 'liepin', 'degree', '学历 degree', 50 UNION ALL
    SELECT 'liepin', 'industry', '行业 industry', 60 UNION ALL
    SELECT 'liepin', 'jobType', '职位类型 jobType', 70 UNION ALL
    SELECT 'liepin', 'scale', '公司规模 scale', 80 UNION ALL
    SELECT 'liepin', 'stage', '融资阶段 stage', 90 UNION ALL
    SELECT 'liepin', 'compKind', '企业性质 compKind', 100 UNION ALL
    SELECT 'zhilian', 'salary', '薪资 salary', 10 UNION ALL
    SELECT 'zhilian', 'city', '城市 city', 20
) v
WHERE NOT EXISTS (
    SELECT 1
    FROM hub_platform_option_type t
    WHERE t.platform = v.platform AND t.type = v.type
);

INSERT INTO hub_platform_option_type (platform, type, label, sort_order, enabled, created_at, updated_at)
SELECT source.platform,
       source.type,
       source.type || ' ' || source.type,
       1000,
       1,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT platform, type
    FROM hub_platform_option
    WHERE platform IS NOT NULL AND type IS NOT NULL
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM hub_platform_option_type target
    WHERE target.platform = source.platform AND target.type = source.type
);

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101200100', '武汉', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'wuhan' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101200100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101190100', '南京', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'nanjing' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101190100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101110100', '西安', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'xian' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101110100');

INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, 'boss', '101190400', '苏州', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM hub_city c WHERE c.city_code = 'suzhou' AND NOT EXISTS (SELECT 1 FROM hub_city_platform_code x WHERE x.platform = 'boss' AND x.platform_city_code = '101190400');
