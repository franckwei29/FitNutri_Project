export type Role = "USER" | "ADMIN";
export type EnergyUnit = "KJ" | "KCAL";
export type Meal = "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK";
export type SafetyAnswer = "NO" | "YES_OR_UNSURE" | "PREFER_NOT_TO_ANSWER";
export type Sex = "MALE" | "FEMALE" | "OTHER" | "PREFER_NOT_TO_SAY";
export type Activity = "SEDENTARY" | "LIGHT" | "MODERATE" | "HIGH";
export type Goal = "LOSE_FAT" | "GAIN_MUSCLE" | "MAINTAIN" | "NOT_SURE";
export interface Versioned {
  version: number;
  updated_at: string;
}
export interface User extends Versioned {
  id: number;
  email: string;
  password_hash: string;
  display_name: string;
  role: Role;
  enabled: boolean;
  created_at: string;
}
export interface Profile extends Versioned {
  user_id: number;
  birth_year: number | null;
  adult_confirmed: boolean;
  sex: Sex | null;
  height_cm: number | null;
  activity_level: Activity | null;
  goal_type: Goal | null;
  daily_target_kj: number | null;
  energy_unit: EnergyUnit;
  health_safety_answer: SafetyAnswer | null;
  health_safety_updated_at: string | null;
  qa_consent_granted: boolean;
  qa_consent_updated_at: string | null;
  onboarding_completed: boolean;
}
export interface Weight extends Versioned {
  id: number;
  user_id: number;
  recorded_date: string;
  weight_kg: number;
  created_at: string;
}
export interface Dataset {
  id: number;
  name: string;
  release: string;
  revision: string;
  source_url: string;
  content_sha256: string;
  imported_at: string;
}
export interface FoodDataset extends Dataset {
  nutrient_policy: string;
}
export interface ActivityDataset extends Dataset {
  min_age: number;
  max_age: number;
}
export interface FoodReference {
  id: number;
  dataset_id: number;
  public_food_key: string;
  survey_id: string;
  official_name: string;
  display_name: string | null;
  description: string | null;
  energy_kj_100g: number;
  protein_g_100g: number;
  fat_g_100g: number;
  carbs_g_100g: number;
  is_active: boolean;
}
export interface ExerciseType {
  id: number;
  dataset_id: number;
  activity_key: string;
  display_name: string;
  source_activity_code: string;
  source_description: string;
  met_value: number;
  is_active: boolean;
}
export interface RecordBase extends Versioned {
  id: number;
  user_id: number;
  client_entry_id: string;
  recorded_date: string;
  created_at: string;
  deleted_at: string | null;
}
export interface Nutrients {
  energy_kj: number;
  protein_grams: number;
  fat_grams: number;
  carbs_grams: number;
}
export interface FoodRecord extends RecordBase, Nutrients {
  food_reference_id: number;
  meal_type: Meal;
  quantity_grams: number;
}
export type EstimateStatus =
  | "AVAILABLE"
  | "MISSING_WEIGHT"
  | "NO_APPLICABLE_REFERENCE";
export interface Estimate {
  estimated_kj: number | null;
  weight_kg_used: number | null;
  weight_date_used: string | null;
  met_used: number | null;
  formula_version: string | null;
  estimate_status: EstimateStatus;
}
export interface ExerciseRecord extends RecordBase, Estimate {
  exercise_type_id: number;
  duration_minutes: number;
}
export interface Database {
  schemaVersion: 1;
  users: User[];
  profiles: Profile[];
  weights: Weight[];
  foodDatasets: FoodDataset[];
  foods: FoodReference[];
  activityDatasets: ActivityDataset[];
  exerciseTypes: ExerciseType[];
  foodRecords: FoodRecord[];
  exerciseRecords: ExerciseRecord[];
}
export interface DraftFood {
  client_entry_id: string;
  food_reference_id: number | null;
  quantity_grams: string;
  meal_type: Meal | "";
  reference_confirmed: boolean;
  grams_confirmed: boolean;
}
export interface Draft {
  kind: "food" | "exercise";
  method: "manual" | "voice" | "image";
  recorded_date: string;
  foods: DraftFood[];
  exercise_type_id: number | null;
  duration_minutes: string;
  client_entry_id: string;
  editId?: number;
  editVersion?: number;
}
export type Errors = Record<string, string>;
export const mealNames: Record<Meal, string> = {
  BREAKFAST: "Breakfast",
  LUNCH: "Lunch",
  DINNER: "Dinner",
  SNACK: "Snack",
};
export const sexNames: Record<Sex, string> = {
  MALE: "Male",
  FEMALE: "Female",
  OTHER: "Other",
  PREFER_NOT_TO_SAY: "Prefer not to say",
};
export const activityNames: Record<Activity, string> = {
  SEDENTARY: "Sedentary",
  LIGHT: "Lightly active",
  MODERATE: "Moderately active",
  HIGH: "Very active",
};
export const goalNames: Record<Goal, string> = {
  LOSE_FAT: "Lose body fat",
  GAIN_MUSCLE: "Gain muscle",
  MAINTAIN: "Maintain wellbeing",
  NOT_SURE: "Not sure yet",
};
export const safetyNames: Record<SafetyAnswer, string> = {
  NO: "No",
  YES_OR_UNSURE: "Yes or unsure",
  PREFER_NOT_TO_ANSWER: "Prefer not to answer",
};
export const today = () => localDate(new Date());
export function localDate(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
export function shiftDate(date: string, days: number) {
  const d = new Date(date + "T12:00:00");
  d.setDate(d.getDate() + days);
  return localDate(d);
}
export function formatDate(date: string, short = false) {
  return new Date(date + "T12:00:00").toLocaleDateString("en-AU", {
    day: "numeric",
    month: short ? "short" : "long",
    ...(short ? {} : { year: "numeric" }),
  });
}
export function datesBetween(start: string, end: string) {
  const dates: string[] = [];
  for (let d = start; d <= end && dates.length < 3700; d = shiftDate(d, 1))
    dates.push(d);
  return dates;
}
export const round3 = (n: number) =>
  Math.round((n + Number.EPSILON) * 1000) / 1000;
export const energy = (kj: number, unit: EnergyUnit) =>
  unit === "KCAL" ? kj / 4.184 : kj;
export const energyLabel = (unit: EnergyUnit) =>
  unit === "KCAL" ? "kcal" : "kJ";
export const fmt = (n: number, digits = 0) =>
  n.toLocaleString("en-AU", { maximumFractionDigits: digits });
export const showEnergy = (kj: number, unit: EnergyUnit) =>
  `${fmt(energy(kj, unit))} ${energyLabel(unit)}`;
export const eligible = (p: Profile | undefined) =>
  !!p && p.health_safety_answer === "NO" && p.qa_consent_granted;
export function emptyProfile(user_id: number): Profile {
  return {
    user_id,
    birth_year: null,
    adult_confirmed: false,
    sex: null,
    height_cm: null,
    activity_level: null,
    goal_type: null,
    daily_target_kj: null,
    energy_unit: "KJ",
    health_safety_answer: null,
    health_safety_updated_at: null,
    qa_consent_granted: false,
    qa_consent_updated_at: null,
    onboarding_completed: false,
    updated_at: new Date().toISOString(),
    version: 0,
  };
}
export function newFood(): DraftFood {
  return {
    client_entry_id: crypto.randomUUID(),
    food_reference_id: null,
    quantity_grams: "",
    meal_type: "",
    reference_confirmed: false,
    grams_confirmed: false,
  };
}
export function newDraft(
  kind: "food" | "exercise" = "food",
  method: Draft["method"] = "manual",
): Draft {
  return {
    kind,
    method,
    recorded_date: today(),
    foods: [],
    exercise_type_id: null,
    duration_minutes: "",
    client_entry_id: crypto.randomUUID(),
  };
}
export function calculateFood(food: FoodReference, grams: number): Nutrients {
  return {
    energy_kj: round3((food.energy_kj_100g * grams) / 100),
    protein_grams: round3((food.protein_g_100g * grams) / 100),
    fat_grams: round3((food.fat_g_100g * grams) / 100),
    carbs_grams: round3((food.carbs_g_100g * grams) / 100),
  };
}
export function estimateExercise(
  type: ExerciseType,
  dataset: ActivityDataset,
  profile: Profile,
  weights: Weight[],
  date: string,
  minutes: number,
): Estimate {
  const weight = weights
    .filter((w) => w.user_id === profile.user_id && w.recorded_date <= date)
    .sort((a, b) => b.recorded_date.localeCompare(a.recorded_date))[0];
  const age =
    profile.birth_year === null
      ? null
      : Number(date.slice(0, 4)) - profile.birth_year;
  const base = {
    estimated_kj: null,
    weight_kg_used: weight?.weight_kg ?? null,
    weight_date_used: weight?.recorded_date ?? null,
    met_used: null,
    formula_version: null,
  };
  if (age === null || age < dataset.min_age || age > dataset.max_age)
    return { ...base, estimate_status: "NO_APPLICABLE_REFERENCE" };
  if (!weight) return { ...base, estimate_status: "MISSING_WEIGHT" };
  return {
    estimate_status: "AVAILABLE",
    estimated_kj: round3(
      ((type.met_value * weight.weight_kg * minutes) / 60) * 4.184,
    ),
    weight_kg_used: weight.weight_kg,
    weight_date_used: weight.recorded_date,
    met_used: type.met_value,
    formula_version: "MET_KG_H_GROSS_KJ_V1",
  };
}
export function validDate(date: string, now = today()) {
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(date) &&
    !Number.isNaN(Date.parse(date + "T12:00:00")) &&
    localDate(new Date(date + "T12:00:00")) === date &&
    date >= "1900-01-01" &&
    date <= now
  );
}
export function validateDraft(
  draft: Draft,
  db: Database,
  confirm = false,
): Errors {
  const e: Errors = {};
  if (!validDate(draft.recorded_date))
    e.date = "Choose today or a valid past date.";
  if (draft.kind === "food") {
    if (!draft.foods.length) e.foods = "Add at least one food.";
    draft.foods.forEach((item, i) => {
      if (!db.foods.some((f) => f.id === item.food_reference_id && f.is_active))
        e[`food${i}`] = "Select a reference food from the catalogue.";
      if (
        !Number.isFinite(Number(item.quantity_grams)) ||
        Number(item.quantity_grams) <= 0 ||
        Number(item.quantity_grams) > 10000
      )
        e[`grams${i}`] = "Enter a portion greater than 0 and up to 10,000 g.";
      if (!item.meal_type || !mealNames[item.meal_type])
        e[`meal${i}`] = "Choose a meal occasion for this food.";
      if (confirm && (!item.reference_confirmed || !item.grams_confirmed))
        e[`confirm${i}`] =
          "Confirm the selected food and its portion in grams.";
    });
  } else {
    if (
      !db.exerciseTypes.some(
        (t) => t.id === draft.exercise_type_id && t.is_active,
      )
    )
      e.exercise = "Select an exercise from the catalogue.";
    if (
      !Number.isFinite(Number(draft.duration_minutes)) ||
      Number(draft.duration_minutes) <= 0 ||
      Number(draft.duration_minutes) > 1440
    )
      e.duration = "Enter a duration greater than 0 and up to 1,440 minutes.";
  }
  return e;
}
export function validateProfile(
  p: Profile,
  weight: string,
  date: string,
  requireWeight = false,
): Errors {
  const e: Errors = {};
  if (!p.adult_confirmed) e.adult = "You must confirm you are 18 or older.";
  if (
    !p.birth_year ||
    !Number.isInteger(p.birth_year) ||
    p.birth_year < 1900 ||
    p.birth_year > Number(today().slice(0, 4)) - 18
  )
    e.birth = "Enter a birth year consistent with being 18 or older.";
  if (!p.sex || !sexNames[p.sex]) e.sex = "Choose an option.";
  if (!p.height_cm || p.height_cm <= 0 || p.height_cm > 300)
    e.height = "Enter a height greater than 0 and up to 300 cm.";
  if (!p.activity_level || !activityNames[p.activity_level])
    e.activity = "Choose your activity level.";
  if (
    p.daily_target_kj !== null &&
    (!Number.isFinite(p.daily_target_kj) ||
      p.daily_target_kj <= 0 ||
      p.daily_target_kj > 100000)
  )
    e.target = "Enter a positive reference goal, or leave it empty.";
  if (!p.health_safety_answer || !safetyNames[p.health_safety_answer])
    e.safety = "Choose an answer; you can prefer not to answer.";
  if (
    (requireWeight || weight !== "") &&
    (!Number.isFinite(Number(weight)) ||
      Number(weight) <= 0 ||
      Number(weight) > 600)
  )
    e.weight = "Enter a weight greater than 0 and up to 600 kg.";
  if ((requireWeight || weight !== "") && !validDate(date))
    e.weightDate = "Choose today or a valid past date.";
  return e;
}
export function recordsFor(
  db: Database,
  userId: number,
  start: string,
  end: string,
) {
  return {
    foods: db.foodRecords.filter(
      (r) =>
        r.user_id === userId &&
        !r.deleted_at &&
        r.recorded_date >= start &&
        r.recorded_date <= end,
    ),
    exercises: db.exerciseRecords.filter(
      (r) =>
        r.user_id === userId &&
        !r.deleted_at &&
        r.recorded_date >= start &&
        r.recorded_date <= end,
    ),
  };
}
export function aggregate(
  db: Database,
  userId: number,
  start: string,
  end: string,
) {
  const { foods, exercises } = recordsFor(db, userId, start, end);
  return {
    foods,
    exercises,
    intake: foods.reduce((s, r) => s + r.energy_kj, 0),
    expenditure: exercises.reduce((s, r) => s + (r.estimated_kj ?? 0), 0),
    minutes: exercises.reduce((s, r) => s + r.duration_minutes, 0),
    protein: foods.reduce((s, r) => s + r.protein_grams, 0),
    carbs: foods.reduce((s, r) => s + r.carbs_grams, 0),
    fat: foods.reduce((s, r) => s + r.fat_grams, 0),
    missingEstimates: exercises.filter((r) => r.estimated_kj === null).length,
    foodDays: new Set(foods.map((r) => r.recorded_date)).size,
    exerciseDays: new Set(exercises.map((r) => r.recorded_date)).size,
    daysLogged: new Set([...foods, ...exercises].map((r) => r.recorded_date))
      .size,
  };
}
export function trends(
  db: Database,
  userId: number,
  start: string,
  end: string,
  unit: EnergyUnit,
) {
  return datesBetween(start, end).map((date) => {
    const a = aggregate(db, userId, date, date);
    return {
      date,
      label: formatDate(date, true),
      intake: a.foods.length ? round3(energy(a.intake, unit)) : null,
      expenditure: a.exercises.some((r) => r.estimated_kj !== null)
        ? round3(energy(a.expenditure, unit))
        : null,
      partial: a.missingEstimates > 0,
    };
  });
}
export function commitDraft(
  db: Database,
  userId: number,
  draft: Draft,
): Database {
  const user = db.users.find((u) => u.id === userId);
  if (!user || !user.enabled || user.role !== "USER")
    throw Error("This account cannot save personal records.");
  const p = db.profiles.find((p) => p.user_id === userId);
  if (!p?.onboarding_completed) throw Error("Complete your profile first.");
  if (Object.keys(validateDraft(draft, db, true)).length)
    throw Error("Check the required fields before saving.");
  const next = structuredClone(db);
  const now = new Date().toISOString();
  const rows = draft.kind === "food" ? next.foodRecords : next.exerciseRecords;
  const old =
    draft.editId === undefined
      ? undefined
      : rows.find(
          (r) => r.id === draft.editId && r.user_id === userId && !r.deleted_at,
        );
  if (draft.editId !== undefined && (!old || old.version !== draft.editVersion))
    throw Error(
      "This record has changed. Reload it from History before editing.",
    );
  if (old && draft.kind === "food" && draft.foods.length !== 1)
    throw Error("Edit one saved food at a time.");
  const base = (key: string) => ({
    id: old?.id ?? Math.max(0, ...rows.map((r) => r.id)) + 1,
    user_id: userId,
    client_entry_id: old?.client_entry_id ?? key,
    recorded_date: draft.recorded_date,
    created_at: old?.created_at ?? now,
    updated_at: now,
    version: old ? old.version + 1 : 0,
    deleted_at: null,
  });
  if (draft.kind === "food")
    for (const item of draft.foods) {
      if (
        !old &&
        next.foodRecords.some(
          (r) =>
            r.user_id === userId && r.client_entry_id === item.client_entry_id,
        )
      )
        continue;
      const food = next.foods.find((f) => f.id === item.food_reference_id)!;
      const row: FoodRecord = {
        ...base(item.client_entry_id),
        food_reference_id: food.id,
        quantity_grams: Number(item.quantity_grams),
        meal_type: item.meal_type as Meal,
        ...calculateFood(food, Number(item.quantity_grams)),
      };
      if (old)
        next.foodRecords[next.foodRecords.findIndex((r) => r.id === old.id)] =
          row;
      else next.foodRecords.push(row);
    }
  else if (
    old ||
    !next.exerciseRecords.some(
      (r) =>
        r.user_id === userId && r.client_entry_id === draft.client_entry_id,
    )
  ) {
    const type = next.exerciseTypes.find(
      (t) => t.id === draft.exercise_type_id,
    )!;
    const row: ExerciseRecord = {
      ...base(draft.client_entry_id),
      exercise_type_id: type.id,
      duration_minutes: Number(draft.duration_minutes),
      ...estimateExercise(
        type,
        next.activityDatasets.find((ds) => ds.id === type.dataset_id)!,
        p,
        next.weights,
        draft.recorded_date,
        Number(draft.duration_minutes),
      ),
    };
    if (old)
      next.exerciseRecords[
        next.exerciseRecords.findIndex((r) => r.id === old.id)
      ] = row;
    else next.exerciseRecords.push(row);
  }
  return next;
}
export function editDraft(
  record: FoodRecord | ExerciseRecord,
  kind: "food" | "exercise",
): Draft {
  const d = newDraft(kind);
  d.editId = record.id;
  d.editVersion = record.version;
  d.recorded_date = record.recorded_date;
  d.client_entry_id = record.client_entry_id;
  if ("food_reference_id" in record)
    d.foods = [
      {
        client_entry_id: record.client_entry_id,
        food_reference_id: record.food_reference_id,
        quantity_grams: String(record.quantity_grams),
        meal_type: record.meal_type,
        reference_confirmed: true,
        grams_confirmed: true,
      },
    ];
  else {
    d.exercise_type_id = record.exercise_type_id;
    d.duration_minutes = String(record.duration_minutes);
  }
  return d;
}
