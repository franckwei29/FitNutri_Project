-- Expand snapshots without pretending unknown legacy dates/references/calculation inputs are known.
-- Preserve original floating inputs verbatim before adding canonical decimal columns.
ALTER TABLE food_record RENAME COLUMN quantity_grams TO quantity_grams_legacy;
ALTER TABLE food_record ALTER COLUMN quantity_grams_legacy DROP NOT NULL;
ALTER TABLE food_record RENAME COLUMN protein_grams TO protein_grams_legacy;
ALTER TABLE food_record RENAME COLUMN carbs_grams TO carbs_grams_legacy;
ALTER TABLE food_record RENAME COLUMN fat_grams TO fat_grams_legacy;
ALTER TABLE food_record ADD COLUMN quantity_grams numeric(10,3);
ALTER TABLE food_record ADD COLUMN protein_grams numeric(12,3);
ALTER TABLE food_record ADD COLUMN carbs_grams numeric(12,3);
ALTER TABLE food_record ADD COLUMN fat_grams numeric(12,3);
ALTER TABLE food_record ADD COLUMN energy_kj numeric(12,3);
ALTER TABLE food_record ADD COLUMN food_reference_id bigint REFERENCES food_reference(id) ON DELETE RESTRICT;

ALTER TABLE exercise_record RENAME COLUMN duration_minutes TO duration_minutes_legacy;
ALTER TABLE exercise_record ALTER COLUMN duration_minutes_legacy DROP NOT NULL;
ALTER TABLE exercise_record ADD COLUMN duration_minutes numeric(8,2);
-- Integer-to-decimal conversion is exact; the original integer column is retained.
UPDATE exercise_record SET duration_minutes=duration_minutes_legacy;
ALTER TABLE exercise_record ALTER COLUMN duration_minutes SET NOT NULL;
ALTER TABLE exercise_record ADD COLUMN exercise_type_id bigint REFERENCES exercise_type(id) ON DELETE RESTRICT;
ALTER TABLE exercise_record ADD COLUMN weight_kg_used numeric(6,2);
ALTER TABLE exercise_record ADD COLUMN weight_date_used date;
ALTER TABLE exercise_record ADD COLUMN met_used numeric(6,2);
ALTER TABLE exercise_record ADD COLUMN estimated_kj numeric(12,3);
ALTER TABLE exercise_record ADD COLUMN estimate_status varchar(32) NOT NULL DEFAULT 'LEGACY_UNVERIFIED';
ALTER TABLE exercise_record ADD COLUMN formula_version varchar(64);

ALTER TABLE food_record ADD COLUMN recorded_date date;
ALTER TABLE food_record ADD COLUMN updated_at timestamptz;
ALTER TABLE food_record ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE food_record ADD COLUMN deleted_at timestamptz;
ALTER TABLE food_record ADD COLUMN client_entry_id uuid;
ALTER TABLE food_record ADD COLUMN data_quality varchar(24) NOT NULL DEFAULT 'LEGACY_UNVERIFIED';
UPDATE food_record SET updated_at=created_at;
ALTER TABLE food_record ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE food_record ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE food_record ADD CONSTRAINT food_record_client_unique UNIQUE(user_id,client_entry_id);
ALTER TABLE food_record ADD CONSTRAINT food_record_quality_valid CHECK(data_quality IN ('CURRENT','LEGACY_UNVERIFIED'));
ALTER TABLE food_record DROP CONSTRAINT food_record_source_check;
ALTER TABLE food_record ADD CONSTRAINT food_record_source_check CHECK(source IN ('MANUAL','PHOTO','IMAGE','VOICE'));
ALTER TABLE food_record ADD CONSTRAINT food_record_current_complete CHECK(data_quality<>'CURRENT' OR
    (recorded_date IS NOT NULL AND client_entry_id IS NOT NULL AND food_reference_id IS NOT NULL AND quantity_grams IS NOT NULL AND quantity_grams>0 AND quantity_grams<=10000 AND btrim(food_name)<>''));
ALTER TABLE food_record ADD CONSTRAINT food_record_snapshot_valid CHECK(
    (quantity_grams IS NULL OR (quantity_grams>0 AND quantity_grams<'Infinity'::numeric)) AND
    (energy_kj IS NULL OR (energy_kj>=0 AND energy_kj<'Infinity'::numeric)) AND
    (protein_grams IS NULL OR (protein_grams>=0 AND protein_grams<'Infinity'::numeric)) AND
    (carbs_grams IS NULL OR (carbs_grams>=0 AND carbs_grams<'Infinity'::numeric)) AND
    (fat_grams IS NULL OR (fat_grams>=0 AND fat_grams<'Infinity'::numeric)));
CREATE INDEX food_record_owner_date_active ON food_record(user_id,recorded_date) WHERE deleted_at IS NULL;

ALTER TABLE exercise_record ADD COLUMN recorded_date date;
ALTER TABLE exercise_record ADD COLUMN updated_at timestamptz;
ALTER TABLE exercise_record ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE exercise_record ADD COLUMN deleted_at timestamptz;
ALTER TABLE exercise_record ADD COLUMN client_entry_id uuid;
ALTER TABLE exercise_record ADD COLUMN data_quality varchar(24) NOT NULL DEFAULT 'LEGACY_UNVERIFIED';
UPDATE exercise_record SET updated_at=created_at;
ALTER TABLE exercise_record ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE exercise_record ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_client_unique UNIQUE(user_id,client_entry_id);
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_quality_valid CHECK(data_quality IN ('CURRENT','LEGACY_UNVERIFIED'));
ALTER TABLE exercise_record DROP CONSTRAINT exercise_record_source_check;
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_source_check CHECK(source IN ('MANUAL','PHOTO','IMAGE','VOICE'));
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_estimate_status_valid CHECK(estimate_status IN ('AVAILABLE','MISSING_WEIGHT','NO_APPLICABLE_REFERENCE','LEGACY_UNVERIFIED'));
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_current_complete CHECK(data_quality<>'CURRENT' OR
    (recorded_date IS NOT NULL AND client_entry_id IS NOT NULL AND exercise_type_id IS NOT NULL AND duration_minutes>0 AND duration_minutes<=1440 AND estimate_status<>'LEGACY_UNVERIFIED'));
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_available_complete CHECK(estimate_status<>'AVAILABLE' OR
    (recorded_date IS NOT NULL AND estimated_kj IS NOT NULL AND weight_kg_used IS NOT NULL AND weight_date_used IS NOT NULL AND met_used IS NOT NULL AND formula_version IS NOT NULL AND btrim(formula_version)<>'' AND weight_date_used<=recorded_date));
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_unavailable_null CHECK(estimate_status NOT IN ('MISSING_WEIGHT','NO_APPLICABLE_REFERENCE') OR estimated_kj IS NULL);
ALTER TABLE exercise_record ADD CONSTRAINT exercise_record_snapshot_valid CHECK(
    (estimated_kj IS NULL OR (estimated_kj>=0 AND estimated_kj<'Infinity'::numeric)) AND
    (weight_kg_used IS NULL OR (weight_kg_used>=20 AND weight_kg_used<=400)) AND
    (met_used IS NULL OR (met_used>0 AND met_used<'Infinity'::numeric)));
CREATE INDEX exercise_record_owner_date_active ON exercise_record(user_id,recorded_date) WHERE deleted_at IS NULL;

-- Privacy envelope for durable creation proof: no raw prompts, health answers or arbitrary context.
CREATE FUNCTION receipt_result_is_minimal(value jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
SELECT coalesce(CASE WHEN value IS NULL THEN true
    WHEN jsonb_typeof(value)<>'object' OR value-'records'<>'{}'::jsonb OR jsonb_typeof(value->'records')<>'array' THEN false
    ELSE jsonb_array_length(value->'records') BETWEEN 1 AND 50 AND NOT EXISTS(
        SELECT 1 FROM jsonb_array_elements(value->'records') r WHERE CASE WHEN jsonb_typeof(r)<>'object' THEN true ELSE
            r-ARRAY['id','clientEntryId','recordDate','versionAtCreation']<>'{}'::jsonb
            OR NOT (r ?& ARRAY['id','clientEntryId','recordDate','versionAtCreation'])
            OR jsonb_typeof(r->'id')<>'string' OR (r->>'id')!~'^[1-9][0-9]*$'
            OR jsonb_typeof(r->'clientEntryId')<>'string' OR (r->>'clientEntryId')!~'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            OR jsonb_typeof(r->'recordDate')<>'string' OR (r->>'recordDate')!~'^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
            OR CASE WHEN jsonb_typeof(r->'versionAtCreation')='number' THEN (r->>'versionAtCreation')::numeric<0 OR mod((r->>'versionAtCreation')::numeric,1)<>0 ELSE true END
        END)
    END,false)
$$;
CREATE TABLE submission_receipt (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    submission_id uuid NOT NULL,
    operation varchar(20) NOT NULL CHECK(operation IN ('FOOD_BATCH','EXERCISE_CREATE')),
    request_sha256 varchar(64) NOT NULL CHECK(request_sha256~'^[a-f0-9]{64}$' AND request_sha256<>repeat('0',64)),
    result_json jsonb,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    committed_at timestamptz,
    CONSTRAINT submission_receipt_user_identity UNIQUE(user_id,submission_id),
    CONSTRAINT submission_receipt_minimal_result CHECK(receipt_result_is_minimal(result_json)),
    CONSTRAINT submission_receipt_proof_pair CHECK((result_json IS NULL)=(committed_at IS NULL))
);
CREATE FUNCTION require_committed_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
    IF EXISTS(SELECT 1 FROM submission_receipt WHERE id=NEW.id AND committed_at IS NULL) THEN
        RAISE EXCEPTION 'A receipt must commit atomically with its final record proof' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER submission_receipt_commit_required AFTER INSERT OR UPDATE ON submission_receipt
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_committed_receipt();
CREATE FUNCTION protect_receipt_history() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
    IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Creation receipt identity must be retained' USING ERRCODE='23514'; END IF;
    IF OLD.committed_at IS NOT NULL OR
       (to_jsonb(NEW)-ARRAY['result_json','committed_at']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['result_json','committed_at']) THEN
        RAISE EXCEPTION 'Creation proof and request identity are immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER submission_receipt_history BEFORE UPDATE OR DELETE ON submission_receipt FOR EACH ROW EXECUTE FUNCTION protect_receipt_history();
