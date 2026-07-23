CREATE TABLE IF NOT EXISTS hub_boss_filter_condition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filter_name VARCHAR(200) NOT NULL,
    filter_conditions TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_boss_filter_condition_user ON hub_boss_filter_condition(user_id);
