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
