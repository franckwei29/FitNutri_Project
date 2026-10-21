-- Expand only. Conflicts fail before schema/data changes; never delete or merge legacy rows.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM app_user GROUP BY lower(btrim(email)) HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Canonical email collisions require a reviewed resolution before V2';
    END IF;
    IF EXISTS (SELECT 1 FROM weight_entry GROUP BY user_id,entry_date HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Duplicate dated weights require a reviewed resolution before V2';
    END IF;
    IF EXISTS (SELECT 1 FROM weight_entry WHERE weight_kg::text IN ('NaN','Infinity','-Infinity')
        OR weight_kg<20 OR weight_kg>400 OR weight_kg::numeric<>round(weight_kg::numeric,2)) THEN
        RAISE EXCEPTION 'Invalid or excessive-precision legacy weights require review before V2';
    END IF;
    IF EXISTS (SELECT 1 FROM user_profile WHERE energy_goal_kcal IS NOT NULL AND
        (energy_goal_kcal::text IN ('NaN','Infinity','-Infinity') OR energy_goal_kcal<=0 OR energy_goal_kcal::numeric*4.184>999999999.999)) THEN
        RAISE EXCEPTION 'Invalid legacy energy goal requires review before conversion';
    END IF;
END $$;

ALTER TABLE app_user ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN updated_at timestamptz;
UPDATE app_user SET updated_at=created_at;
ALTER TABLE app_user ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_user ALTER COLUMN updated_at SET NOT NULL;
CREATE UNIQUE INDEX app_user_canonical_email_unique ON app_user(lower(btrim(email)));

ALTER TABLE user_profile ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE user_profile ADD COLUMN adult_confirmed boolean NOT NULL DEFAULT false;
ALTER TABLE user_profile ADD COLUMN energy_unit varchar(4) NOT NULL DEFAULT 'KJ';
ALTER TABLE user_profile ADD COLUMN daily_target_kj numeric(12,3);
ALTER TABLE user_profile ADD COLUMN health_safety_updated_at timestamptz;
ALTER TABLE user_profile ADD COLUMN qa_consent_updated_at timestamptz;
ALTER TABLE user_profile ADD COLUMN updated_at timestamptz;
-- Legacy adult confirmation and consent times are unknown, never inferred.
UPDATE user_profile SET daily_target_kj=round(energy_goal_kcal::numeric*4.184,3) WHERE energy_goal_kcal IS NOT NULL;
UPDATE user_profile SET health_safety_consent=false WHERE health_safety_consent IS NULL;
ALTER TABLE user_profile DROP CONSTRAINT user_profile_health_safety_answer_check;
UPDATE user_profile SET health_safety_answer=CASE health_safety_answer
    WHEN 'YES' THEN 'YES_OR_UNSURE' WHEN 'NOT_SURE' THEN 'YES_OR_UNSURE'
    WHEN 'PREFER_NOT_TO_SAY' THEN 'PREFER_NOT_TO_ANSWER' ELSE health_safety_answer END;
ALTER TABLE user_profile ADD CONSTRAINT user_profile_health_safety_answer_check
    CHECK(health_safety_answer IN ('NO','YES_OR_UNSURE','PREFER_NOT_TO_ANSWER'));
ALTER TABLE user_profile ADD CONSTRAINT user_profile_energy_unit_check CHECK(energy_unit IN ('KJ','KCAL'));
ALTER TABLE user_profile ADD CONSTRAINT user_profile_daily_target_positive CHECK(daily_target_kj IS NULL OR daily_target_kj>0);

ALTER TABLE weight_entry ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE weight_entry ADD COLUMN updated_at timestamptz;
UPDATE weight_entry SET updated_at=created_at;
ALTER TABLE weight_entry ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE weight_entry ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE weight_entry ALTER COLUMN weight_kg TYPE numeric(6,2) USING weight_kg::numeric(6,2);
ALTER TABLE weight_entry ADD CONSTRAINT weight_entry_weight_range CHECK(weight_kg>=20 AND weight_kg<=400);
ALTER TABLE weight_entry ADD CONSTRAINT weight_entry_user_date_unique UNIQUE(user_id,entry_date);
CREATE INDEX weight_entry_user_date_desc ON weight_entry(user_id,entry_date DESC);
