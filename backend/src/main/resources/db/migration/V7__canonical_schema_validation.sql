-- Validate structure and provenance honesty, not external-service/catalogue readiness.
DO $$ BEGIN
    IF (SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN
        ('app_user','user_profile','weight_entry','food_dataset','food_reference','food_record','activity_dataset','exercise_type','exercise_record','submission_receipt'))<>10 THEN
        RAISE EXCEPTION 'Canonical application tables are incomplete';
    END IF;
    IF EXISTS(SELECT 1 FROM pg_constraint WHERE connamespace='public'::regnamespace AND NOT convalidated) OR
       EXISTS(SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid WHERE c.relnamespace='public'::regnamespace AND NOT i.indisvalid) THEN
        RAISE EXCEPTION 'Canonical constraints/indexes must be fully validated';
    END IF;
    IF EXISTS(SELECT 1 FROM food_record WHERE recorded_date IS NULL OR client_entry_id IS NULL OR quantity_grams IS NULL) OR
       EXISTS(SELECT 1 FROM exercise_record WHERE recorded_date IS NULL OR client_entry_id IS NULL) THEN
        RAISE EXCEPTION 'Canonical record identity/date backfill is incomplete';
    END IF;
    IF EXISTS(SELECT 1 FROM food_dataset WHERE name='LEGACY_AFCD_UNVERIFIED' AND verified) OR
       EXISTS(SELECT 1 FROM activity_dataset WHERE name='LEGACY_CONFIG_UNVERIFIED' AND verified) THEN
        RAISE EXCEPTION 'Legacy source must not be represented as verified official provenance';
    END IF;
END $$;
