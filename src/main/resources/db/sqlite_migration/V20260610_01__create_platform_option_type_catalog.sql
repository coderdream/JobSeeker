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
