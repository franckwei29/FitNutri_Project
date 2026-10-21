-- UX-D02: versioned minimal proofs; original V1 validation and immutable/commit triggers are retained.
CREATE FUNCTION receipt_result_is_valid_v2(operation text,value jsonb) RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE r jsonb; l jsonb; parent jsonb; root_count integer; leaf_count integer; child_count integer; first_date text;
BEGIN
 IF operation IN ('FOOD_BATCH','EXERCISE_CREATE') THEN RETURN receipt_result_is_minimal(value); END IF;
 IF operation NOT IN ('FOOD_BATCH_V2','EXERCISE_ENTRY_V2') OR operation IS NULL THEN RETURN false; END IF;
 IF value IS NULL THEN RETURN true; END IF;
 IF jsonb_typeof(value)<>'object' OR value-ARRAY['schemaVersion','roots','leaves']<>'{}' OR NOT(value ?& ARRAY['schemaVersion','roots','leaves']) OR value->'schemaVersion'<>'2'::jsonb OR jsonb_typeof(value->'roots')<>'array' OR jsonb_typeof(value->'leaves')<>'array' THEN RETURN false; END IF;
 root_count=jsonb_array_length(value->'roots');leaf_count=jsonb_array_length(value->'leaves');
 IF root_count<1 OR leaf_count<1 OR (operation='FOOD_BATCH_V2' AND (root_count>20 OR leaf_count>50)) OR (operation='EXERCISE_ENTRY_V2' AND (root_count<>1 OR leaf_count>12)) THEN RETURN false; END IF;
 first_date=value->'roots'->0->>'recordDate';
 FOR r IN SELECT * FROM jsonb_array_elements(value->'roots') LOOP
  IF jsonb_typeof(r)<>'object' OR r-ARRAY['kind','id','clientEntryId','recordDate','versionAtCreation']<>'{}' OR NOT(r ?& ARRAY['kind','id','clientEntryId','recordDate','versionAtCreation']) THEN RETURN false; END IF;
  IF jsonb_typeof(r->'kind')<>'string' OR (operation='FOOD_BATCH_V2' AND r->>'kind' NOT IN ('SINGLE','DISH')) OR (operation='EXERCISE_ENTRY_V2' AND r->>'kind' NOT IN ('QUICK','WORKOUT')) THEN RETURN false; END IF;
  IF jsonb_typeof(r->'id')<>'string' OR r->>'id'!~'^[1-9][0-9]{0,18}$' OR (r->>'id')::numeric>9223372036854775807 OR jsonb_typeof(r->'clientEntryId')<>'string' OR r->>'clientEntryId'!~'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' OR jsonb_typeof(r->'recordDate')<>'string' OR r->>'recordDate'!~'^[0-9]{4}-[0-9]{2}-[0-9]{2}$' OR r->>'recordDate'<>first_date OR r->'versionAtCreation'<>'0'::jsonb THEN RETURN false; END IF;
  PERFORM (r->>'recordDate')::date;
  SELECT count(*) INTO child_count FROM jsonb_array_elements(value->'leaves') x WHERE x->>'rootClientEntryId'=r->>'clientEntryId';
  IF child_count<1 OR (r->>'kind' IN ('SINGLE','QUICK') AND child_count<>1) OR (r->>'kind'='DISH' AND child_count>20) OR (r->>'kind'='WORKOUT' AND child_count>12) THEN RETURN false; END IF;
 END LOOP;
 IF (SELECT count(DISTINCT (x->>'kind',x->>'id')) FROM jsonb_array_elements(value->'roots') x)<>root_count OR (SELECT count(DISTINCT x->>'clientEntryId') FROM jsonb_array_elements(value->'roots') x)<>root_count THEN RETURN false; END IF;
 FOR l IN SELECT * FROM jsonb_array_elements(value->'leaves') LOOP
  IF jsonb_typeof(l)<>'object' OR l-ARRAY['kind','id','clientEntryId','rootClientEntryId','recordDate','versionAtCreation']<>'{}' OR NOT(l ?& ARRAY['kind','id','clientEntryId','rootClientEntryId','recordDate','versionAtCreation']) THEN RETURN false; END IF;
  IF (operation='FOOD_BATCH_V2' AND l->>'kind' IS DISTINCT FROM 'FOOD_COMPONENT') OR (operation='EXERCISE_ENTRY_V2' AND l->>'kind' IS DISTINCT FROM 'ACTIVITY_PERIOD') THEN RETURN false; END IF;
  IF jsonb_typeof(l->'id')<>'string' OR l->>'id'!~'^[1-9][0-9]{0,18}$' OR (l->>'id')::numeric>9223372036854775807 OR jsonb_typeof(l->'clientEntryId')<>'string' OR l->>'clientEntryId'!~'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' OR jsonb_typeof(l->'rootClientEntryId')<>'string' OR jsonb_typeof(l->'recordDate')<>'string' OR l->>'recordDate'<>first_date OR l->'versionAtCreation'<>'0'::jsonb THEN RETURN false; END IF;
  SELECT x INTO parent FROM jsonb_array_elements(value->'roots') x WHERE x->>'clientEntryId'=l->>'rootClientEntryId';
  IF parent IS NULL THEN RETURN false; END IF;
  IF parent->>'kind' IN ('SINGLE','QUICK') THEN
   IF l->>'clientEntryId'<>parent->>'clientEntryId' OR l->>'id'<>parent->>'id' THEN RETURN false; END IF;
  ELSIF EXISTS(SELECT 1 FROM jsonb_array_elements(value->'roots') x WHERE x->>'clientEntryId'=l->>'clientEntryId') THEN RETURN false;
  END IF;
 END LOOP;
 IF (SELECT count(DISTINCT (x->>'kind',x->>'id')) FROM jsonb_array_elements(value->'leaves') x)<>leaf_count OR (SELECT count(DISTINCT x->>'clientEntryId') FROM jsonb_array_elements(value->'leaves') x)<>leaf_count THEN RETURN false; END IF;
 RETURN true;
EXCEPTION WHEN OTHERS THEN RETURN false;
END $$;
ALTER TABLE submission_receipt DROP CONSTRAINT submission_receipt_operation_check;
ALTER TABLE submission_receipt ADD CONSTRAINT submission_receipt_operation_check CHECK(operation IN ('FOOD_BATCH','EXERCISE_CREATE','FOOD_BATCH_V2','EXERCISE_ENTRY_V2'));
ALTER TABLE submission_receipt DROP CONSTRAINT submission_receipt_minimal_result;
ALTER TABLE submission_receipt ADD CONSTRAINT submission_receipt_minimal_result CHECK(receipt_result_is_valid_v2(operation,result_json));
