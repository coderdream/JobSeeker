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

DO $$
DECLARE
    admin_id BIGINT;
BEGIN
    SELECT id INTO admin_id FROM app_user WHERE username = 'admin';

    IF to_regclass('public.cookie') IS NOT NULL THEN
        EXECUTE format('UPDATE cookie SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.config') IS NOT NULL THEN
        EXECUTE format('UPDATE config SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.ai') IS NOT NULL THEN
        EXECUTE format('UPDATE ai SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.boss_config') IS NOT NULL THEN
        EXECUTE format('UPDATE boss_config SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.liepin_config') IS NOT NULL THEN
        EXECUTE format('UPDATE liepin_config SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.zhilian_config') IS NOT NULL THEN
        EXECUTE format('UPDATE zhilian_config SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.job51_config') IS NOT NULL THEN
        EXECUTE format('UPDATE job51_config SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.boss_blacklist') IS NOT NULL THEN
        EXECUTE format('UPDATE boss_blacklist SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.boss_data') IS NOT NULL THEN
        EXECUTE format('UPDATE boss_data SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.liepin_data') IS NOT NULL THEN
        EXECUTE format('UPDATE liepin_data SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.zhilian_data') IS NOT NULL THEN
        EXECUTE format('UPDATE zhilian_data SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
    IF to_regclass('public.job51_data') IS NOT NULL THEN
        EXECUTE format('UPDATE job51_data SET user_id = %s WHERE user_id IS NULL', admin_id);
    END IF;
END $$;
