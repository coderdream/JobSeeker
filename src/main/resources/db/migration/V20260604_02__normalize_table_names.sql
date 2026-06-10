DO $$
DECLARE
    table_rename TEXT[];
    old_table TEXT;
    new_table TEXT;
BEGIN
    FOREACH table_rename SLICE 1 IN ARRAY ARRAY[
        ARRAY['app_user', 'hub_user'],
        ARRAY['user_job_task', 'hub_user_job_task'],
        ARRAY['config', 'hub_config'],
        ARRAY['cookie', 'hub_cookie'],
        ARRAY['ai', 'hub_ai'],
        ARRAY['boss_config', 'hub_boss_config'],
        ARRAY['job51_config', 'hub_job51_config'],
        ARRAY['liepin_config', 'hub_liepin_config'],
        ARRAY['zhilian_config', 'hub_zhilian_config'],
        ARRAY['boss_data', 'hub_boss_data'],
        ARRAY['job51_data', 'hub_job51_data'],
        ARRAY['liepin_data', 'hub_liepin_data'],
        ARRAY['zhilian_data', 'hub_zhilian_data'],
        ARRAY['boss_option', 'hub_boss_option'],
        ARRAY['job51_option', 'hub_job51_option'],
        ARRAY['liepin_option', 'hub_liepin_option'],
        ARRAY['zhilian_option', 'hub_zhilian_option'],
        ARRAY['boss_industry', 'hub_boss_industry'],
        ARRAY['city', 'hub_city'],
        ARRAY['city_platform_code', 'hub_city_platform_code'],
        ARRAY['platform_option', 'hub_platform_option'],
        ARRAY['boss_blacklist', 'hub_boss_blacklist']
    ]
    LOOP
        old_table := table_rename[1];
        new_table := table_rename[2];

        IF to_regclass('public.' || old_table) IS NOT NULL
           AND to_regclass('public.' || new_table) IS NULL THEN
            EXECUTE 'ALTER TABLE ' || old_table || ' RENAME TO ' || new_table;
        END IF;
    END LOOP;
END $$;
