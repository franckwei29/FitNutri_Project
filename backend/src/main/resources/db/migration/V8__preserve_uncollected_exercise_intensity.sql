-- Canonical preset recording collects type/duration/date, never an intensity observation.
-- Preserve every historical value and its existing allowed-value CHECK; permit honest NULL for new uncollected values.
DO $$ BEGIN
    IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.exercise_record'::regclass AND conname='exercise_record_intensity_check') THEN
        RAISE EXCEPTION 'Expected legacy intensity constraint is missing; inspect before V8';
    END IF;
END $$;
ALTER TABLE exercise_record ALTER COLUMN intensity DROP NOT NULL;
