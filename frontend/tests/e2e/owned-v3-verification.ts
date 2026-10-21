import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';
import { assertE2EEnvironment } from './owned-db-verification.mjs';
export interface GroupFacts {
    dishes: {
        id: string;
        version: number;
        deleted: boolean;
    }[];
    workouts: {
        id: string;
        version: number;
        deleted: boolean;
    }[];
    periods: {
        id: string;
        workoutId: string | null;
        durationMinutes: number;
        estimatedKj: number | null;
        equipmentDetails: Record<string, number>;
        deleted: boolean;
    }[];
    movements: {
        periodId: string;
        name: string;
        sets: number | null;
        reps: number | null;
        loadKg: number | null;
        basis: string;
    }[];
    movementAuthorityColumns: number;
    ledger: {
        foodEnergyKj: number | null; proteinGrams:number|null;fatGrams:number|null;carbsGrams:number|null;
        exerciseEnergyKj: number | null;
        exerciseMinutes: number | null;
        foodComponents: number;
        exercisePeriods: number;
        foodRoots: number;
        exerciseRoots: number;
    };
}
/** Fixed SELECT-only SQL,restricted to a generated synthetic owner in an explicit disposable database. */
export function inspectGroups(email: string): GroupFacts {
    assert.match(email, /^e2e-[a-z0-9-]+@example\.test$/);
    const { container, user, database } = assertE2EEnvironment();
    const sql = `WITH owner AS (SELECT id FROM app_user WHERE email=:'case_email') SELECT json_build_object(
'dishes',(SELECT coalesce(json_agg(json_build_object('id',id::text,'version',version,'deleted',deleted_at IS NOT NULL) ORDER BY id),'[]'::json) FROM food_dish_record WHERE user_id=(SELECT id FROM owner)),
'workouts',(SELECT coalesce(json_agg(json_build_object('id',id::text,'version',version,'deleted',deleted_at IS NOT NULL) ORDER BY id),'[]'::json) FROM workout_record WHERE user_id=(SELECT id FROM owner)),
'periods',(SELECT coalesce(json_agg(json_build_object('id',id::text,'workoutId',workout_id::text,'durationMinutes',duration_minutes,'estimatedKj',estimated_kj,'equipmentDetails',equipment_details,'deleted',deleted_at IS NOT NULL) ORDER BY id),'[]'::json) FROM exercise_record WHERE user_id=(SELECT id FROM owner)),
'movements',(SELECT coalesce(json_agg(json_build_object('periodId',m.exercise_record_id::text,'name',m.name_snapshot,'sets',m.set_count,'reps',m.repetitions,'loadKg',m.load_kg,'basis',m.load_basis_snapshot) ORDER BY m.id),'[]'::json) FROM workout_movement_log m JOIN exercise_record r ON r.id=m.exercise_record_id WHERE r.user_id=(SELECT id FROM owner)),
'movementAuthorityColumns',(SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='workout_movement_log' AND (column_name LIKE '%duration%' OR column_name LIKE '%energy%')),
'ledger',json_build_object('foodEnergyKj',(SELECT sum(energy_kj) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'proteinGrams',(SELECT sum(protein_grams) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'fatGrams',(SELECT sum(fat_grams) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'carbsGrams',(SELECT sum(carbs_grams) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'exerciseEnergyKj',(SELECT sum(estimated_kj) FROM exercise_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'exerciseMinutes',(SELECT sum(duration_minutes) FROM exercise_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'foodComponents',(SELECT count(*) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'exercisePeriods',(SELECT count(*) FROM exercise_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'foodRoots',(SELECT count(*) FROM food_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL AND dish_id IS NULL)+(SELECT count(*) FROM food_dish_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL),'exerciseRoots',(SELECT count(*) FROM exercise_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL AND workout_id IS NULL)+(SELECT count(*) FROM workout_record WHERE user_id=(SELECT id FROM owner) AND deleted_at IS NULL))
);`;
    return JSON.parse(execFileSync('docker', ['exec', '-i', container, 'psql', '-XAt', '-v', 'ON_ERROR_STOP=1', '-U', user, '-d', database, '-v', `case_email=${email}`], { input: sql, encoding: 'utf8', timeout: 15000 }).trim()) as GroupFacts;
}
