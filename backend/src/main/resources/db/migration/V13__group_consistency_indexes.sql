-- UX-D04: validate the final transaction state; never repair, regroup or recalculate existing data.
CREATE FUNCTION validate_food_group(group_id bigint) RETURNS void LANGUAGE plpgsql AS $$
DECLARE parent food_dish_record; n integer;
BEGIN
 IF group_id IS NULL THEN RETURN; END IF;SELECT * INTO parent FROM food_dish_record WHERE id=group_id;
 IF parent.id IS NULL THEN RAISE EXCEPTION 'Missing food group' USING ERRCODE='23514'; END IF;
 SELECT count(*) INTO n FROM food_record WHERE dish_id=group_id AND deleted_at IS NULL;
 IF (parent.deleted_at IS NULL AND (n<1 OR n>20)) OR (parent.deleted_at IS NOT NULL AND n<>0) OR EXISTS(
  SELECT 1 FROM food_record WHERE dish_id=group_id AND deleted_at IS NULL AND (user_id<>parent.user_id OR recorded_date<>parent.recorded_date OR meal_type<>parent.meal_type OR quantity_grams<>round(ingredient_input_grams*parent.portion_numerator/parent.portion_denominator,12))) THEN
  RAISE EXCEPTION 'Food group owner, date, meal, portion or lifecycle is inconsistent' USING ERRCODE='23514';
 END IF;
END $$;
CREATE FUNCTION enforce_food_group() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_TABLE_NAME='food_dish_record' THEN PERFORM validate_food_group(NEW.id); ELSE PERFORM validate_food_group(NEW.dish_id); END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER dish_consistency AFTER INSERT OR UPDATE ON food_dish_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_food_group();
CREATE CONSTRAINT TRIGGER ingredient_consistency AFTER INSERT OR UPDATE ON food_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_food_group();
CREATE FUNCTION validate_workout_group(group_id bigint) RETURNS void LANGUAGE plpgsql AS $$
DECLARE parent workout_record; n integer; minutes numeric; movements integer;
BEGIN
 IF group_id IS NULL THEN RETURN; END IF;SELECT * INTO parent FROM workout_record WHERE id=group_id;
 IF parent.id IS NULL THEN RAISE EXCEPTION 'Missing workout group' USING ERRCODE='23514'; END IF;
 SELECT count(*),sum(duration_minutes) INTO n,minutes FROM exercise_record WHERE workout_id=group_id AND deleted_at IS NULL;
 SELECT count(*) INTO movements FROM workout_movement_log m JOIN exercise_record e ON e.id=m.exercise_record_id WHERE e.workout_id=group_id AND e.deleted_at IS NULL;
 IF (parent.deleted_at IS NULL AND (n<1 OR n>12 OR minutes>1440 OR movements>100)) OR (parent.deleted_at IS NOT NULL AND n<>0) OR EXISTS(
  SELECT 1 FROM exercise_record WHERE workout_id=group_id AND deleted_at IS NULL AND (user_id<>parent.user_id OR recorded_date<>parent.recorded_date)) THEN
  RAISE EXCEPTION 'Workout owner, date, limits or lifecycle is inconsistent' USING ERRCODE='23514';
 END IF;
END $$;
CREATE FUNCTION enforce_workout_group() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE group_id bigint;
BEGIN
 IF TG_TABLE_NAME='workout_record' THEN group_id=NEW.id;
 ELSIF TG_TABLE_NAME='exercise_record' THEN group_id=NEW.workout_id;
 ELSE SELECT workout_id INTO group_id FROM exercise_record WHERE id=NEW.exercise_record_id; END IF;
 PERFORM validate_workout_group(group_id);RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER workout_consistency AFTER INSERT OR UPDATE ON workout_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_workout_group();
CREATE CONSTRAINT TRIGGER period_consistency AFTER INSERT OR UPDATE ON exercise_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_workout_group();
CREATE CONSTRAINT TRIGGER movement_workout_consistency AFTER INSERT OR UPDATE ON workout_movement_log DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_workout_group();
CREATE FUNCTION protect_entry_identity() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Record identities must be retained; use reviewed soft deletion' USING ERRCODE='23514'; END IF;
 IF NEW.user_id IS DISTINCT FROM OLD.user_id OR NEW.client_entry_id IS DISTINCT FROM OLD.client_entry_id OR NEW.created_at IS DISTINCT FROM OLD.created_at OR
 (to_jsonb(NEW)->'dish_id') IS DISTINCT FROM (to_jsonb(OLD)->'dish_id') OR (to_jsonb(NEW)->'workout_id') IS DISTINCT FROM (to_jsonb(OLD)->'workout_id') OR
 (to_jsonb(NEW)->'origin_dish_id') IS DISTINCT FROM (to_jsonb(OLD)->'origin_dish_id') OR (to_jsonb(NEW)->'origin_workout_id') IS DISTINCT FROM (to_jsonb(OLD)->'origin_workout_id') THEN
 RAISE EXCEPTION 'Record owner, creation and root identities are immutable' USING ERRCODE='23514'; END IF;RETURN NEW;
END $$;
CREATE TRIGGER food_identity_history BEFORE UPDATE OR DELETE ON food_record FOR EACH ROW EXECUTE FUNCTION protect_entry_identity();
CREATE TRIGGER dish_identity_history BEFORE UPDATE OR DELETE ON food_dish_record FOR EACH ROW EXECUTE FUNCTION protect_entry_identity();
CREATE TRIGGER exercise_identity_history BEFORE UPDATE OR DELETE ON exercise_record FOR EACH ROW EXECUTE FUNCTION protect_entry_identity();
CREATE TRIGGER workout_identity_history BEFORE UPDATE OR DELETE ON workout_record FOR EACH ROW EXECUTE FUNCTION protect_entry_identity();
CREATE FUNCTION enforce_entry_uuid_namespace() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE n integer;
BEGIN
 -- Same lock order as the application creation boundary, including direct transactional writers.
 PERFORM id FROM app_user WHERE id=NEW.user_id FOR UPDATE;
 SELECT count(*) INTO n FROM (
  SELECT user_id,client_entry_id FROM food_record UNION ALL SELECT user_id,client_entry_id FROM exercise_record UNION ALL
  SELECT user_id,client_entry_id FROM food_dish_record UNION ALL SELECT user_id,client_entry_id FROM workout_record
 ) identities WHERE user_id=NEW.user_id AND client_entry_id=NEW.client_entry_id;
 IF n<>1 THEN RAISE EXCEPTION 'Client entry identity is already used for this owner' USING ERRCODE='23505'; END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER food_uuid_namespace AFTER INSERT ON food_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_entry_uuid_namespace();
CREATE CONSTRAINT TRIGGER dish_uuid_namespace AFTER INSERT ON food_dish_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_entry_uuid_namespace();
CREATE CONSTRAINT TRIGGER exercise_uuid_namespace AFTER INSERT ON exercise_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_entry_uuid_namespace();
CREATE CONSTRAINT TRIGGER workout_uuid_namespace AFTER INSERT ON workout_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_entry_uuid_namespace();
-- Deferred checks must inspect the current row, not an earlier queued intermediate snapshot.
CREATE OR REPLACE FUNCTION validate_exercise_option_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE opt exercise_option; current_row exercise_record;
BEGIN
 SELECT * INTO current_row FROM exercise_record WHERE id=NEW.id;
 IF current_row.exercise_option_id IS NULL THEN RETURN NULL; END IF;
 SELECT * INTO opt FROM exercise_option WHERE id=current_row.exercise_option_id;
 IF opt.id IS NULL OR opt.review_status<>'REVIEWED' OR opt.period_kind<>current_row.period_kind OR current_row.exercise_type_id IS DISTINCT FROM opt.reference_id OR
  (opt.mapping_kind='UNAVAILABLE' AND (current_row.estimate_status<>'NO_APPLICABLE_REFERENCE' OR current_row.estimated_kj IS NOT NULL OR current_row.met_used IS NOT NULL OR current_row.weight_kg_used IS NOT NULL OR current_row.weight_date_used IS NOT NULL OR current_row.formula_version IS NOT NULL)) OR
  EXISTS(SELECT 1 FROM jsonb_object_keys(current_row.equipment_details) k WHERE NOT(opt.allowed_details ? k)) OR
  (current_row.period_kind<>'STRENGTH' AND EXISTS(SELECT 1 FROM workout_movement_log WHERE exercise_record_id=current_row.id)) THEN RAISE EXCEPTION 'Invalid option snapshot or unsupported details' USING ERRCODE='23514'; END IF;
 IF NOT opt.active AND (TG_OP='INSERT' OR OLD.exercise_option_id IS DISTINCT FROM current_row.exercise_option_id) THEN RAISE EXCEPTION 'New activity requires active option' USING ERRCODE='23514'; END IF;RETURN NULL;
END $$;
CREATE OR REPLACE FUNCTION validate_movement_log_period() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE row workout_movement_log; period exercise_record; movement movement_catalogue;
BEGIN
 SELECT * INTO row FROM workout_movement_log WHERE id=NEW.id;IF row.id IS NULL THEN RETURN NULL; END IF;
 SELECT * INTO period FROM exercise_record WHERE id=row.exercise_record_id;SELECT * INTO movement FROM movement_catalogue WHERE id=row.movement_catalogue_id;
 IF period.period_kind IS DISTINCT FROM 'STRENGTH' OR movement.review_status<>'REVIEWED' OR row.load_basis_snapshot<>movement.load_basis OR
 (row.set_count IS NOT NULL AND NOT(movement.allowed_fields ? 'setCount')) OR (row.repetitions IS NOT NULL AND NOT(movement.allowed_fields ? 'repetitions')) OR (row.load_kg IS NOT NULL AND NOT(movement.allowed_fields ? 'loadKg')) THEN
 RAISE EXCEPTION 'Movement details require compatible reviewed strength catalogue' USING ERRCODE='23514'; END IF;
 IF NOT movement.active AND (TG_OP='INSERT' OR OLD.movement_catalogue_id IS DISTINCT FROM row.movement_catalogue_id) THEN RAISE EXCEPTION 'New details require active movement' USING ERRCODE='23514'; END IF;
 IF (SELECT count(*) FROM workout_movement_log WHERE exercise_record_id=period.id)>30 THEN RAISE EXCEPTION 'Too many movements in period' USING ERRCODE='23514'; END IF;RETURN NULL;
END $$;
-- Fail migration on an existing inconsistency; never delete/rewrite rows to satisfy a new check.
DO $$ DECLARE r record; BEGIN
 IF EXISTS(SELECT 1 FROM (SELECT user_id,client_entry_id FROM food_record UNION ALL SELECT user_id,client_entry_id FROM exercise_record UNION ALL SELECT user_id,client_entry_id FROM food_dish_record UNION ALL SELECT user_id,client_entry_id FROM workout_record) identities GROUP BY user_id,client_entry_id HAVING count(*)>1) THEN RAISE EXCEPTION 'Existing cross-domain client identities require review' USING ERRCODE='23505'; END IF;
 FOR r IN SELECT id FROM food_dish_record LOOP PERFORM validate_food_group(r.id);END LOOP;
 FOR r IN SELECT id FROM workout_record LOOP PERFORM validate_workout_group(r.id);END LOOP;
END $$;
CREATE INDEX exercise_option_category_active ON exercise_option(category,display_name,id) WHERE active AND review_status='REVIEWED';
CREATE INDEX movement_body_equipment_active ON movement_catalogue(body_region,equipment_type,display_name,id) WHERE active AND review_status='REVIEWED';
