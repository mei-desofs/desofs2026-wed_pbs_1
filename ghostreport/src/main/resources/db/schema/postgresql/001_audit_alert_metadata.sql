DO $$
BEGIN
    IF to_regclass('public.audit_logs') IS NOT NULL THEN
        ALTER TABLE public.audit_logs
            ADD COLUMN IF NOT EXISTS correlation_id varchar(80);

        UPDATE public.audit_logs
        SET correlation_id = 'legacy-' || id
        WHERE correlation_id IS NULL OR btrim(correlation_id) = '';

        ALTER TABLE public.audit_logs
            ADD COLUMN IF NOT EXISTS integrity_hash varchar(64);

        UPDATE public.audit_logs
        SET integrity_hash =
            md5(concat_ws('|',
                coalesce(timestamp::text, ''),
                coalesce(correlation_id, ''),
                coalesce(actor, ''),
                coalesce(action, ''),
                coalesce(target_type, ''),
                coalesce(target_id::text, ''),
                coalesce(details, '')
            ))
            ||
            md5(concat_ws('|',
                'legacy-v1',
                coalesce(timestamp::text, ''),
                coalesce(correlation_id, ''),
                coalesce(actor, ''),
                coalesce(action, ''),
                coalesce(target_type, ''),
                coalesce(target_id::text, ''),
                coalesce(details, '')
            ))
        WHERE integrity_hash IS NULL OR btrim(integrity_hash) = '';

        ALTER TABLE public.audit_logs
            ALTER COLUMN correlation_id SET NOT NULL;

        ALTER TABLE public.audit_logs
            ALTER COLUMN integrity_hash SET NOT NULL;
    END IF;

    IF to_regclass('public.security_alerts') IS NOT NULL THEN
        ALTER TABLE public.security_alerts
            ADD COLUMN IF NOT EXISTS correlation_id varchar(80);

        UPDATE public.security_alerts
        SET correlation_id = 'legacy-' || id
        WHERE correlation_id IS NULL OR btrim(correlation_id) = '';

        ALTER TABLE public.security_alerts
            ADD COLUMN IF NOT EXISTS integrity_hash varchar(64);

        UPDATE public.security_alerts
        SET integrity_hash =
            md5(concat_ws('|',
                coalesce(timestamp::text, ''),
                coalesce(correlation_id, ''),
                coalesce(alert_type, ''),
                coalesce(severity, ''),
                coalesce(actor, ''),
                coalesce(target_type, ''),
                coalesce(target_id::text, ''),
                coalesce(description, '')
            ))
            ||
            md5(concat_ws('|',
                'legacy-v1',
                coalesce(timestamp::text, ''),
                coalesce(correlation_id, ''),
                coalesce(alert_type, ''),
                coalesce(severity, ''),
                coalesce(actor, ''),
                coalesce(target_type, ''),
                coalesce(target_id::text, ''),
                coalesce(description, '')
            ))
        WHERE integrity_hash IS NULL OR btrim(integrity_hash) = '';

        ALTER TABLE public.security_alerts
            ALTER COLUMN correlation_id SET NOT NULL;

        ALTER TABLE public.security_alerts
            ALTER COLUMN integrity_hash SET NOT NULL;
    END IF;
END $$;
@@
