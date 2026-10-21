BEGIN READ ONLY;
SET LOCAL TIME ZONE 'UTC';
DO $$ BEGIN
    IF NOT EXISTS(SELECT 1 FROM flyway_schema_history WHERE version='7' AND success) THEN RAISE EXCEPTION 'Canonical V7 migration history is required'; END IF;
    IF EXISTS(SELECT 1 FROM pg_constraint WHERE connamespace='public'::regnamespace AND NOT convalidated) OR
       EXISTS(SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid WHERE c.relnamespace='public'::regnamespace AND NOT i.indisvalid) THEN RAISE EXCEPTION 'Unvalidated constraint or index'; END IF;
    IF EXISTS(SELECT 1 FROM app_user GROUP BY lower(btrim(email)) HAVING count(*)>1) OR
       EXISTS(SELECT 1 FROM weight_entry GROUP BY user_id,entry_date HAVING count(*)>1) OR
       EXISTS(SELECT 1 FROM food_record GROUP BY user_id,client_entry_id HAVING count(*)>1) OR
       EXISTS(SELECT 1 FROM exercise_record GROUP BY user_id,client_entry_id HAVING count(*)>1) THEN RAISE EXCEPTION 'Duplicate business identity'; END IF;
    IF EXISTS(SELECT 1 FROM food_record WHERE recorded_date IS NULL OR client_entry_id IS NULL OR quantity_grams IS NULL) OR
       EXISTS(SELECT 1 FROM exercise_record WHERE recorded_date IS NULL OR client_entry_id IS NULL) OR
       EXISTS(SELECT 1 FROM submission_receipt WHERE committed_at IS NULL OR result_json IS NULL) THEN RAISE EXCEPTION 'Incomplete canonical identity or creation proof'; END IF;
END $$;
SELECT jsonb_build_object(
    'database',current_database(),
    'rows',jsonb_build_object('users',(SELECT count(*) FROM app_user),'profiles',(SELECT count(*) FROM user_profile),'weights',(SELECT count(*) FROM weight_entry),'foodDatasets',(SELECT count(*) FROM food_dataset),'activityDatasets',(SELECT count(*) FROM activity_dataset),'foodReferences',(SELECT count(*) FROM food_reference),'exerciseTypes',(SELECT count(*) FROM exercise_type),'foods',(SELECT count(*) FROM food_record),'exercises',(SELECT count(*) FROM exercise_record),'receipts',(SELECT count(*) FROM submission_receipt)),
    'legacy',jsonb_build_object('foods',(SELECT count(*) FROM food_record WHERE data_quality='LEGACY_UNVERIFIED'),'exercises',(SELECT count(*) FROM exercise_record WHERE data_quality='LEGACY_UNVERIFIED')),
    'current',jsonb_build_object('foods',(SELECT count(*) FROM food_record WHERE data_quality='CURRENT'),'exercises',(SELECT count(*) FROM exercise_record WHERE data_quality='CURRENT')),
    'activeVerifiedCatalogues',jsonb_build_object('foodReferences',(SELECT count(*) FROM food_reference r JOIN food_dataset d ON d.id=r.dataset_id WHERE r.is_active AND d.verified),'exerciseTypes',(SELECT count(*) FROM exercise_type r JOIN activity_dataset d ON d.id=r.dataset_id WHERE r.is_active AND d.verified)),
    'knownEnergyKj',jsonb_build_object('food',(SELECT sum(energy_kj) FROM food_record),'exercise',(SELECT sum(estimated_kj) FROM exercise_record)),
    'legacyEnergyReconciliation',jsonb_build_object(
      'foodRawKj',(SELECT sum(calories::numeric*4.184) FROM food_record WHERE data_quality='LEGACY_UNVERIFIED' AND calories>=0 AND calories<'Infinity'::float8 AND calories::numeric*4.184<=999999999.999),
      'foodRoundedKj',(SELECT sum(energy_kj) FROM food_record WHERE data_quality='LEGACY_UNVERIFIED' AND calories>=0 AND calories<'Infinity'::float8 AND calories::numeric*4.184<=999999999.999),
      'foodRoundingDelta',(SELECT sum(energy_kj-calories::numeric*4.184) FROM food_record WHERE data_quality='LEGACY_UNVERIFIED' AND calories>=0 AND calories<'Infinity'::float8 AND calories::numeric*4.184<=999999999.999),
      'foodRoundingBound',(SELECT count(energy_kj)*0.0005 FROM food_record WHERE data_quality='LEGACY_UNVERIFIED' AND calories>=0 AND calories<'Infinity'::float8 AND calories::numeric*4.184<=999999999.999)),
    'missingEnergy',jsonb_build_object('food',(SELECT count(*) FROM food_record WHERE energy_kj IS NULL),'exercise',(SELECT count(*) FROM exercise_record WHERE estimated_kj IS NULL)),
    'dates',jsonb_build_object('foodFrom',(SELECT min(recorded_date) FROM food_record),'foodTo',(SELECT max(recorded_date) FROM food_record),'exerciseFrom',(SELECT min(recorded_date) FROM exercise_record),'exerciseTo',(SELECT max(recorded_date) FROM exercise_record)),
    'legacyOriginalChecksums',jsonb_build_object(
      'foodQuantity',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(quantity_grams_legacy::text,'NULL'),',' ORDER BY id),'')) FROM food_record),
      'foodCalories',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(calories::text,'NULL'),',' ORDER BY id),'')) FROM food_record),
      'foodProtein',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(protein_grams_legacy::text,'NULL'),',' ORDER BY id),'')) FROM food_record),
      'foodCarbs',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(carbs_grams_legacy::text,'NULL'),',' ORDER BY id),'')) FROM food_record),
      'foodFat',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(fat_grams_legacy::text,'NULL'),',' ORDER BY id),'')) FROM food_record),
      'foodOriginalTimestamp',(SELECT md5(coalesce(string_agg(id::text||':'||recorded_at::text,',' ORDER BY id),'')) FROM food_record),
      'exerciseCalories',(SELECT md5(coalesce(string_agg(id::text||':'||coalesce(estimated_calories::text,'NULL'),',' ORDER BY id),'')) FROM exercise_record)),
    'validatedConstraints',(SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND convalidated),
    'validIndexes',(SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid WHERE c.relnamespace='public'::regnamespace AND i.indisvalid)
);
COMMIT;
