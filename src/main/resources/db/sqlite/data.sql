INSERT OR IGNORE INTO hub_city (name, province, city_code, sort_order, enabled, created_at, updated_at) VALUES
('全国', '全国', 'all', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('北京', '北京', 'beijing', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('上海', '上海', 'shanghai', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('广州', '广东', 'guangzhou', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('深圳', '广东', 'shenzhen', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('杭州', '浙江', 'hangzhou', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('成都', '四川', 'chengdu', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('南京', '江苏', 'nanjing', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('武汉', '湖北', 'wuhan', 80, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('西安', '陕西', 'xian', 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('苏州', '江苏', 'suzhou', 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT OR IGNORE INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at)
SELECT c.id, v.platform, v.code, v.name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_city c
JOIN (
    SELECT 'all' city_code, 'boss' platform, '0' code, '不限' name UNION ALL
    SELECT 'beijing', 'boss', '101010100', '北京' UNION ALL
    SELECT 'shanghai', 'boss', '101020100', '上海' UNION ALL
    SELECT 'guangzhou', 'boss', '101280100', '广州' UNION ALL
    SELECT 'shenzhen', 'boss', '101280600', '深圳' UNION ALL
    SELECT 'hangzhou', 'boss', '101210100', '杭州' UNION ALL
    SELECT 'chengdu', 'boss', '101270100', '成都' UNION ALL
    SELECT 'all', '51job', '000000', '不限' UNION ALL
    SELECT 'beijing', '51job', '010000', '北京' UNION ALL
    SELECT 'shanghai', '51job', '020000', '上海' UNION ALL
    SELECT 'guangzhou', '51job', '030200', '广州' UNION ALL
    SELECT 'shenzhen', '51job', '040000', '深圳' UNION ALL
    SELECT 'hangzhou', '51job', '080200', '杭州' UNION ALL
    SELECT 'chengdu', '51job', '090200', '成都' UNION ALL
    SELECT 'all', 'liepin', '0', '不限' UNION ALL
    SELECT 'beijing', 'liepin', '010', '北京' UNION ALL
    SELECT 'shanghai', 'liepin', '020', '上海' UNION ALL
    SELECT 'guangzhou', 'liepin', '050020', '广州' UNION ALL
    SELECT 'shenzhen', 'liepin', '050090', '深圳' UNION ALL
    SELECT 'hangzhou', 'liepin', '070020', '杭州' UNION ALL
    SELECT 'chengdu', 'liepin', '280020', '成都' UNION ALL
    SELECT 'all', 'zhilian', '0', '不限' UNION ALL
    SELECT 'beijing', 'zhilian', '530', '北京' UNION ALL
    SELECT 'shanghai', 'zhilian', '538', '上海' UNION ALL
    SELECT 'guangzhou', 'zhilian', '763', '广州' UNION ALL
    SELECT 'shenzhen', 'zhilian', '765', '深圳' UNION ALL
    SELECT 'hangzhou', 'zhilian', '653', '杭州' UNION ALL
    SELECT 'chengdu', 'zhilian', '801', '成都' UNION ALL
    SELECT 'all', 'yupao', 'all', '不限' UNION ALL
    SELECT 'beijing', 'yupao', 'beijing', '北京' UNION ALL
    SELECT 'shanghai', 'yupao', 'shanghai', '上海' UNION ALL
    SELECT 'guangzhou', 'yupao', 'guangzhou', '广州' UNION ALL
    SELECT 'shenzhen', 'yupao', 'shenzhen', '深圳' UNION ALL
    SELECT 'hangzhou', 'yupao', 'hangzhou', '杭州' UNION ALL
    SELECT 'chengdu', 'yupao', 'chengdu', '成都'
) v ON v.city_code = c.city_code;

INSERT OR IGNORE INTO hub_platform_option (platform, type, name, code, sort_order, enabled, created_at, updated_at) VALUES
('boss', 'salary', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'salary', '3K以下', '402', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'salary', '3-5K', '403', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'salary', '5-10K', '404', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'salary', '10-20K', '405', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'salary', '20-50K', '406', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'industry', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'experience', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'jobType', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'degree', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'scale', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'stage', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '不限', '00', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '3-5千/月', '03', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '5-8千/月', '04', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '8千-1万/月', '05', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '1-1.5万/月', '06', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'salary', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'salary', '10-15K', '10$15', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'salary', '15-20K', '15$20', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'compTag', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'pubTime', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'workYearCode', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'degree', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'industry', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'jobType', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'scale', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'stage', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'compKind', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhilian', 'salary', '不限', '0', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhilian', 'salary', '10-15K', '10000,15000', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhilian', 'salary', '15-20K', '15000,20000', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'salary', '不限', '', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'salary', '3-5K', '3-5K', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'salary', '5-8K', '5-8K', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'jobType', '不限', '', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'jobType', '全职', 'fulltime', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT OR IGNORE INTO hub_platform_option_type (platform, type, label, sort_order, enabled, created_at, updated_at) VALUES
('boss', 'salary', '薪资 salary', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'industry', '行业 industry', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'experience', '经验 experience', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'jobType', '职位类型 jobType', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'degree', '学历 degree', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'scale', '公司规模 scale', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('boss', 'stage', '融资阶段 stage', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'salary', '薪资 salary', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('51job', 'jobArea', '城市区域 jobArea', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'salary', '薪资 salary', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'compTag', '名企 compTag', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'pubTime', '发布时间 pubTime', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'workYearCode', '工作年限 workYearCode', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'degree', '学历 degree', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'industry', '行业 industry', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'jobType', '职位类型 jobType', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'scale', '公司规模 scale', 80, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'stage', '融资阶段 stage', 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('liepin', 'compKind', '企业性质 compKind', 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhilian', 'salary', '薪资 salary', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhilian', 'city', '城市 city', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'salary', '薪资 salary', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yupao', 'jobType', '职位类型 jobType', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT OR IGNORE INTO hub_boss_option (type, name, code, sort_order, created_at, updated_at)
SELECT type, name, code, sort_order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_platform_option
WHERE platform = 'boss';

INSERT OR IGNORE INTO hub_job51_option (type, name, code, sort_order, created_at, updated_at)
SELECT CASE WHEN type = 'city' THEN 'jobArea' ELSE type END, name, code, sort_order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_platform_option
WHERE platform = '51job';

INSERT OR IGNORE INTO hub_liepin_option (type, name, code, sort_order, created_at, updated_at)
SELECT type, name, code, sort_order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_platform_option
WHERE platform = 'liepin';

INSERT OR IGNORE INTO hub_zhilian_option (type, name, code, sort_order, created_at, updated_at)
SELECT type, name, code, sort_order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM hub_platform_option
WHERE platform = 'zhilian';
