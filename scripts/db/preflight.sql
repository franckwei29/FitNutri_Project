-- Read-only diagnostic: no business values, credentials or health responses.
BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';
SELECT jsonb_build_object('kind','identity','database',current_database(),'serverVersion',current_setting('server_version'),'schema',current_schema());
SELECT 1 / CASE WHEN count(*)=6 THEN 1 ELSE 0 END AS six_legacy_tables_present
FROM information_schema.tables WHERE table_schema='public' AND table_name IN
('app_user','user_profile','weight_entry','food_reference','food_record','exercise_record');
SELECT jsonb_build_object('kind','columns','data',jsonb_agg(x ORDER BY x.table_name,x.ordinal_position))
FROM (SELECT table_name,column_name,data_type,is_nullable,column_default,ordinal_position
      FROM information_schema.columns WHERE table_schema='public') x;
SELECT jsonb_build_object('kind','constraints','data',jsonb_agg(x ORDER BY x.table_name,x.name))
FROM (SELECT c.relname AS table_name,co.conname AS name,co.contype AS type,co.convalidated AS validated,
             pg_get_constraintdef(co.oid) AS definition
      FROM pg_constraint co JOIN pg_class c ON c.oid=co.conrelid
      JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public') x;
SELECT jsonb_build_object('kind','indexes','data',jsonb_agg(x ORDER BY x.tablename,x.indexname))
FROM (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public') x;
SELECT jsonb_build_object('kind','counts','app_user',(SELECT count(*) FROM app_user),
 'user_profile',(SELECT count(*) FROM user_profile),'weight_entry',(SELECT count(*) FROM weight_entry),
 'food_reference',(SELECT count(*) FROM food_reference),'food_record',(SELECT count(*) FROM food_record),
 'exercise_record',(SELECT count(*) FROM exercise_record),
 'flywayPresent',to_regclass('public.flyway_schema_history') IS NOT NULL);
SELECT jsonb_build_object('kind','conflicts',
 'canonicalEmailGroups',(SELECT count(*) FROM (SELECT lower(btrim(email)) FROM app_user GROUP BY lower(btrim(email)) HAVING count(*)>1) e),
 'duplicateWeightDates',(SELECT count(*) FROM (SELECT user_id,entry_date FROM weight_entry GROUP BY user_id,entry_date HAVING count(*)>1) w),
 'invalidWeights',(SELECT count(*) FROM weight_entry WHERE weight_kg<=0 OR weight_kg::text IN ('NaN','Infinity','-Infinity') OR NOT isfinite(entry_date)),
 'invalidFoodQuantities',(SELECT count(*) FROM food_record WHERE quantity_grams<=0 OR quantity_grams::text IN ('NaN','Infinity','-Infinity')),
 'invalidDurations',(SELECT count(*) FROM exercise_record WHERE duration_minutes<=0),
 'nonfiniteRecordTimes',(SELECT count(*) FROM food_record WHERE NOT isfinite(recorded_at))+(SELECT count(*) FROM exercise_record WHERE NOT isfinite(recorded_at)),
 'foodReferenceForeignKeys',(SELECT count(*) FROM pg_constraint WHERE contype='f' AND confrelid='public.food_reference'::regclass));
ROLLBACK;
