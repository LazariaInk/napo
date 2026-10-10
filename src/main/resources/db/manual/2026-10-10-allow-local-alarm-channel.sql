-- Local schema fix after adding LOCAL_ALARM and SKIPPED enum values.
-- Hibernate ddl-auto=update does not update existing PostgreSQL check constraints.

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT c.conname
    INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'reminders'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%preferred_channel%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE reminders DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE reminders
    ADD CONSTRAINT reminders_preferred_channel_check
    CHECK (preferred_channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG'));

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT c.conname
    INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'reminder_notification_attempts'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%channel%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE reminder_notification_attempts DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE reminder_notification_attempts
    ADD CONSTRAINT reminder_notification_attempts_channel_check
    CHECK (channel IN ('LOCAL_ALARM', 'IN_APP', 'LOG'));

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT c.conname
    INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'reminder_notification_attempts'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%status%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE reminder_notification_attempts DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE reminder_notification_attempts
    ADD CONSTRAINT reminder_notification_attempts_status_check
    CHECK (status IN ('SUCCESS', 'SKIPPED', 'FAILED'));