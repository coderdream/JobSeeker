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
