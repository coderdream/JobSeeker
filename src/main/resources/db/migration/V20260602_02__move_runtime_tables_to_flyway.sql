CREATE TABLE IF NOT EXISTS job51_option (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job51_data (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50),
    name VARCHAR(100),
    code VARCHAR(100),
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zhilian_data (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
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
