CREATE UNIQUE INDEX IF NOT EXISTS uk_user_job_task_running
    ON user_job_task(user_id, platform)
    WHERE status = 'RUNNING';
