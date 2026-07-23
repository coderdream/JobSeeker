CREATE TABLE IF NOT EXISTS hub_boss_filter_condition (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    filter_name TEXT NOT NULL,
    filter_conditions TEXT NOT NULL,
    created_at TEXT,
    updated_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_boss_filter_condition_user ON hub_boss_filter_condition(user_id);
