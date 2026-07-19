CREATE TABLE IF NOT EXISTS hub_ai (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    introduce TEXT,
    prompt TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_blacklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    value TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
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

CREATE TABLE IF NOT EXISTS hub_boss_industry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    code INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_boss_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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

CREATE TABLE IF NOT EXISTS hub_boss_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_city (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    province TEXT,
    city_code TEXT,
    sort_order INTEGER,
    enabled INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_city_platform_code (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    city_id INTEGER,
    platform TEXT,
    platform_city_code TEXT,
    platform_city_name TEXT,
    enabled INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    config_key TEXT,
    config_value TEXT,
    config_type TEXT,
    category TEXT,
    description TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    platform TEXT,
    cookie_value TEXT,
    remark TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_job51_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    keywords TEXT,
    job_area TEXT,
    salary TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_job51_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER,
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
    delivered INTEGER,
    create_time TEXT,
    update_time TEXT
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

CREATE TABLE IF NOT EXISTS hub_liepin_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
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

CREATE TABLE IF NOT EXISTS hub_liepin_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER,
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
    delivered INTEGER,
    create_time TEXT,
    update_time TEXT
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

CREATE TABLE IF NOT EXISTS hub_platform_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform TEXT,
    type TEXT,
    name TEXT,
    code TEXT,
    sort_order INTEGER,
    enabled INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_platform_option_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform TEXT,
    type TEXT,
    label TEXT,
    sort_order INTEGER,
    enabled INTEGER,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT,
    password_hash TEXT,
    nickname TEXT,
    role TEXT,
    status TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_user_job_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    platform TEXT,
    status TEXT,
    message TEXT,
    started_at TEXT,
    finished_at TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_yupao_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    keywords TEXT,
    city_code TEXT,
    salary TEXT,
    job_type TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_yupao_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
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

CREATE TABLE IF NOT EXISTS hub_zhilian_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    keywords TEXT,
    city_code TEXT,
    salary TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS hub_zhilian_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id TEXT,
    job_title TEXT,
    job_link TEXT,
    salary TEXT,
    location TEXT,
    experience TEXT,
    degree TEXT,
    company_name TEXT,
    delivery_status TEXT,
    create_time TEXT,
    update_time TEXT
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


INSERT INTO hub_platform_option_type (platform, type, label, sort_order, enabled, created_at, updated_at) VALUES ('boss', 'city', 'city', 1000, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at) VALUES (1, 'boss', '101200100', '武汉', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
