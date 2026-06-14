CREATE TABLE IF NOT EXISTS hub_sqlite_schema_version (
    id INTEGER PRIMARY KEY,
    version INTEGER NOT NULL,
    description TEXT,
    installed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hub_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nickname TEXT NOT NULL,
    role TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hub_user_job_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES hub_user(id),
    platform TEXT NOT NULL,
    status TEXT NOT NULL,
    message TEXT,
    started_at TEXT,
    finished_at TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_job_task_user_platform ON hub_user_job_task(user_id, platform);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_job_task_running ON hub_user_job_task(user_id, platform, status) WHERE status = 'RUNNING';

CREATE TABLE IF NOT EXISTS hub_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    config_key TEXT NOT NULL,
    config_value TEXT,
    config_type TEXT,
    category TEXT,
    description TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    platform TEXT,
    cookie_value TEXT,
    remark TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_ai (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    introduce TEXT,
    prompt TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
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
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_job51_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    keywords TEXT,
    job_area TEXT,
    salary TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_zhilian_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    keywords TEXT,
    city_code TEXT,
    salary TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_liepin_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    keywords TEXT,
    city TEXT,
    salary_code TEXT,
    comp_tag TEXT,
    pub_time TEXT,
    work_year_code TEXT,
    edu_level TEXT,
    industry TEXT,
    job_kind TEXT,
    comp_scale TEXT,
    comp_stage TEXT,
    comp_kind TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_yupao_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    keywords TEXT,
    city_code TEXT,
    salary TEXT,
    job_type TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_config_user_key ON hub_config(user_id, config_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cookie_user_platform ON hub_cookie(user_id, platform);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_user ON hub_ai(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_config_user ON hub_boss_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_job51_config_user ON hub_job51_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_zhilian_config_user ON hub_zhilian_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_config_user ON hub_liepin_config(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_config_user ON hub_yupao_config(user_id);

CREATE TABLE IF NOT EXISTS hub_boss_blacklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    type TEXT,
    value TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    encrypt_id TEXT,
    encrypt_user_id TEXT,
    company_name TEXT,
    job_name TEXT,
    salary TEXT,
    location TEXT,
    experience TEXT,
    degree TEXT,
    hr_name TEXT,
    hr_position TEXT,
    hr_active_status TEXT,
    delivery_status TEXT,
    job_description TEXT,
    job_url TEXT,
    recruitment_status TEXT,
    company_address TEXT,
    industry TEXT,
    introduce TEXT,
    financing_stage TEXT,
    company_scale TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_data_user_encrypt ON hub_boss_data(user_id, encrypt_id, encrypt_user_id);

CREATE TABLE IF NOT EXISTS hub_job51_data (
    job_id INTEGER PRIMARY KEY,
    user_id INTEGER REFERENCES hub_user(id),
    job_title TEXT,
    job_link TEXT,
    job_salary_text TEXT,
    job_area TEXT,
    job_edu_req TEXT,
    job_exp_req TEXT,
    job_publish_time TEXT,
    comp_id INTEGER,
    comp_name TEXT,
    comp_industry TEXT,
    comp_scale TEXT,
    hr_id TEXT,
    hr_name TEXT,
    hr_title TEXT,
    delivered INTEGER DEFAULT 0,
    create_time TEXT,
    update_time TEXT
);

CREATE INDEX IF NOT EXISTS idx_job51_data_user_job ON hub_job51_data(user_id, job_id);

CREATE TABLE IF NOT EXISTS hub_liepin_data (
    job_id INTEGER PRIMARY KEY,
    user_id INTEGER REFERENCES hub_user(id),
    job_title TEXT,
    job_link TEXT,
    job_salary_text TEXT,
    job_area TEXT,
    job_edu_req TEXT,
    job_exp_req TEXT,
    job_publish_time TEXT,
    comp_id INTEGER,
    comp_name TEXT,
    comp_industry TEXT,
    comp_scale TEXT,
    hr_id TEXT,
    hr_name TEXT,
    hr_title TEXT,
    hr_im_id TEXT,
    delivered INTEGER DEFAULT 0,
    create_time TEXT,
    update_time TEXT
);

CREATE INDEX IF NOT EXISTS idx_liepin_data_user_job ON hub_liepin_data(user_id, job_id);

CREATE TABLE IF NOT EXISTS hub_zhilian_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    job_id TEXT,
    job_title TEXT,
    job_link TEXT,
    salary TEXT,
    location TEXT,
    experience TEXT,
    degree TEXT,
    company_name TEXT,
    delivery_status TEXT DEFAULT '未投递',
    create_time TEXT,
    update_time TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_zhilian_data_user_job ON hub_zhilian_data(user_id, job_id);

CREATE TABLE IF NOT EXISTS hub_yupao_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES hub_user(id),
    job_id TEXT,
    job_title TEXT,
    job_link TEXT,
    salary TEXT,
    location TEXT,
    experience TEXT,
    degree TEXT,
    company_name TEXT,
    hr_name TEXT,
    delivery_status TEXT,
    publish_time TEXT,
    create_time TEXT,
    update_time TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_data_user_job ON hub_yupao_data(user_id, job_id);
CREATE INDEX IF NOT EXISTS idx_yupao_data_user_status ON hub_yupao_data(user_id, delivery_status);

CREATE TABLE IF NOT EXISTS hub_boss_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_job51_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_liepin_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_zhilian_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_industry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    code INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_city (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    province TEXT,
    city_code TEXT NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_city_platform_code (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    city_id INTEGER REFERENCES hub_city(id),
    platform TEXT NOT NULL,
    platform_city_code TEXT NOT NULL,
    platform_city_name TEXT,
    enabled INTEGER DEFAULT 1,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_platform_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform TEXT NOT NULL,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_platform_option_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform TEXT NOT NULL,
    type TEXT NOT NULL,
    label TEXT NOT NULL,
    sort_order INTEGER,
    enabled INTEGER DEFAULT 1,
    created_at TEXT,
    updated_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_city_city_code ON hub_city(city_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_city_platform_code_platform_code ON hub_city_platform_code(platform, platform_city_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_option_platform_type_code ON hub_platform_option(platform, type, code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_option_type_platform_type ON hub_platform_option_type(platform, type);
