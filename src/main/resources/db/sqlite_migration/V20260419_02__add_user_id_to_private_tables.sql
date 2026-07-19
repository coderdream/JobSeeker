ALTER TABLE IF EXISTS cookie ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS ai ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS liepin_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS zhilian_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS job51_config ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_blacklist ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS boss_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS liepin_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS zhilian_data ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE IF EXISTS job51_data ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE IF EXISTS liepin_data ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE IF EXISTS job51_data ADD COLUMN IF NOT EXISTS id BIGSERIAL;

DO $$
BEGIN
    IF to_regclass('public.liepin_data') IS NOT NULL AND EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'liepin_data' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        EXECUTE 'ALTER TABLE liepin_data DROP CONSTRAINT liepin_data_pkey';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.job51_data') IS NOT NULL AND EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'job51_data' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        EXECUTE 'ALTER TABLE job51_data DROP CONSTRAINT job51_data_pkey';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.liepin_data') IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'liepin_data' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        EXECUTE 'ALTER TABLE liepin_data ADD CONSTRAINT liepin_data_pkey PRIMARY KEY (id)';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.job51_data') IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'job51_data' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        EXECUTE 'ALTER TABLE job51_data ADD CONSTRAINT job51_data_pkey PRIMARY KEY (id)';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.cookie') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_cookie_user_platform ON cookie(user_id, platform)';
    END IF;
    IF to_regclass('public.config') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_config_user_key ON config(user_id, config_key)';
    END IF;
    IF to_regclass('public.ai') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_user ON ai(user_id)';
    END IF;
    IF to_regclass('public.boss_config') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_config_user ON boss_config(user_id)';
    END IF;
    IF to_regclass('public.liepin_config') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_config_user ON liepin_config(user_id)';
    END IF;
    IF to_regclass('public.zhilian_config') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_zhilian_config_user ON zhilian_config(user_id)';
    END IF;
    IF to_regclass('public.job51_config') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_job51_config_user ON job51_config(user_id)';
    END IF;
    IF to_regclass('public.liepin_data') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_data_user_job ON liepin_data(user_id, job_id)';
    END IF;
    IF to_regclass('public.job51_data') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_job51_data_user_job ON job51_data(user_id, job_id)';
    END IF;
    IF to_regclass('public.zhilian_data') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_zhilian_data_user_job ON zhilian_data(user_id, job_id)';
    END IF;
    IF to_regclass('public.boss_data') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_data_user_encrypt ON boss_data(user_id, encrypt_id, encrypt_user_id)';
    END IF;
END $$;
