-- Legacy conversion is explicit and reversible through retained original columns.
-- The caller supplies an allowlisted IANA zone AND reviewed approval; no default historical zone is guessed.
DO $$ BEGIN
    IF EXISTS(SELECT 1 FROM food_record WHERE data_quality='LEGACY_UNVERIFIED' AND quantity_grams IS NULL AND
        (quantity_grams_legacy IS NULL OR quantity_grams_legacy<=0 OR quantity_grams_legacy>9999999.999 OR quantity_grams_legacy::text IN ('NaN','Infinity','-Infinity') OR round(quantity_grams_legacy::numeric,3)<=0)) THEN
        RAISE EXCEPTION 'Legacy food quantity needs reviewed resolution; no row will be deleted or clamped';
    END IF;
    IF EXISTS(SELECT 1 FROM food_record WHERE recorded_date IS NULL) OR EXISTS(SELECT 1 FROM exercise_record WHERE recorded_date IS NULL) THEN
        IF '${legacy_zone_approved}'<>'true' OR NOT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name='${legacy_zone}') THEN
            RAISE EXCEPTION 'Legacy timestamp conversion requires an explicitly approved valid IANA legacy timezone';
        END IF;
        UPDATE food_record SET recorded_date=(recorded_at AT TIME ZONE '${legacy_zone}')::date WHERE recorded_date IS NULL;
        UPDATE exercise_record SET recorded_date=(recorded_at AT TIME ZONE '${legacy_zone}')::date WHERE recorded_date IS NULL;
    END IF;
END $$;
UPDATE food_record SET client_entry_id=gen_random_uuid() WHERE client_entry_id IS NULL;
UPDATE exercise_record SET client_entry_id=gen_random_uuid() WHERE client_entry_id IS NULL;
UPDATE food_record SET
    quantity_grams=coalesce(quantity_grams,round(quantity_grams_legacy::numeric,3)),
    energy_kj=coalesce(energy_kj,CASE WHEN calories>=0 AND calories<'Infinity'::float8 AND calories::numeric*4.184<=999999999.999 THEN round(calories::numeric*4.184,3) END),
    protein_grams=coalesce(protein_grams,CASE WHEN protein_grams_legacy>=0 AND protein_grams_legacy<=999999999.999 THEN round(protein_grams_legacy::numeric,3) END),
    carbs_grams=coalesce(carbs_grams,CASE WHEN carbs_grams_legacy>=0 AND carbs_grams_legacy<=999999999.999 THEN round(carbs_grams_legacy::numeric,3) END),
    fat_grams=coalesce(fat_grams,CASE WHEN fat_grams_legacy>=0 AND fat_grams_legacy<=999999999.999 THEN round(fat_grams_legacy::numeric,3) END)
WHERE data_quality='LEGACY_UNVERIFIED';
UPDATE exercise_record SET estimated_kj=CASE WHEN estimated_calories>=0 AND estimated_calories<'Infinity'::float8 AND estimated_calories::numeric*4.184<=999999999.999 THEN round(estimated_calories::numeric*4.184,3) END
WHERE data_quality='LEGACY_UNVERIFIED' AND estimated_kj IS NULL;
-- No references, body weight, MET, formula, official source or old submission identity is inferred.
-- data_quality and estimate_status remain LEGACY_UNVERIFIED; PHOTO and recorded_at are preserved.
