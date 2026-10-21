import {
  calculateFood,
  emptyProfile,
  estimateExercise,
  shiftDate,
  today,
} from "./domain.ts";
import type { Database, FoodReference, User } from "./domain.ts";
export const demoAccounts = [
  { id: 1, label: "Jessica · returning user", email: "jessica@example.com" },
  { id: 2, label: "Alex · new user", email: "alex@example.com" },
  { id: 3, label: "Marcus · Q&A unavailable", email: "marcus@example.com" },
  { id: 4, label: "Priya · deactivated", email: "priya@example.com" },
  { id: 5, label: "Administrator", email: "admin@example.com" },
];
export const DEMO_PASSWORD = "FitNutri123!";
export async function hashPassword(password: string, salt?: string) {
  const s =
    salt ??
    Array.from(crypto.getRandomValues(new Uint8Array(16)))
      .map((v) => v.toString(16).padStart(2, "0"))
      .join("");
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveBits"],
  );
  const result = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      hash: "SHA-256",
      salt: new TextEncoder().encode(s),
      iterations: 120000,
    },
    key,
    256,
  );
  return `pbkdf2$${s}$${Array.from(new Uint8Array(result))
    .map((v) => v.toString(16).padStart(2, "0"))
    .join("")}`;
}
export async function verifyPassword(password: string, hash: string) {
  return (await hashPassword(password, hash.split("$")[1])) === hash;
}
export async function createSeed(): Promise<Database> {
  const now = new Date().toISOString(),
    t = today();
  const users: User[] = await Promise.all(
    demoAccounts.map(async (a, i) => ({
      id: a.id,
      email: a.email,
      password_hash: await hashPassword(DEMO_PASSWORD),
      display_name: [
        "Jessica Liu",
        "Alex Morgan",
        "Marcus Webb",
        "Priya Sharma",
        "FitNutri Admin",
      ][i],
      role: i === 4 ? ("ADMIN" as const) : ("USER" as const),
      enabled: i !== 3,
      created_at: shiftDate(t, -60 + i * 6) + "T09:00:00.000Z",
      updated_at: now,
      version: 0,
    })),
  );
  const profiles = users
    .filter((u) => u.role === "USER")
    .map((u) =>
      u.id === 2
        ? emptyProfile(u.id)
        : {
            ...emptyProfile(u.id),
            birth_year: 1992,
            adult_confirmed: true,
            sex: "FEMALE" as const,
            height_cm: 168,
            activity_level: "MODERATE" as const,
            goal_type: "MAINTAIN" as const,
            daily_target_kj: 8368,
            health_safety_answer:
              u.id === 3 ? ("YES_OR_UNSURE" as const) : ("NO" as const),
            qa_consent_granted: true,
            health_safety_updated_at: now,
            qa_consent_updated_at: now,
            onboarding_completed: true,
          },
    );
  const rows: [string, number, number, number, number][] = [
    ["Rolled oats", 1550, 12.5, 8.5, 60],
    ["Milk, reduced fat", 195, 3.6, 1.5, 4.9],
    ["Banana, raw", 372, 1.1, 0.3, 20.3],
    ["Chicken breast, grilled, no skin", 692, 31, 3.6, 0],
    ["Brown rice, cooked", 456, 2.3, 0.9, 23],
    ["Mixed salad vegetables", 92, 1.2, 0.3, 2.8],
    ["Salmon, baked", 870, 22, 13, 0],
    ["Broccoli, steamed", 145, 2.5, 0.5, 4],
    ["Apple, raw, with skin", 218, 0.3, 0.2, 12.5],
    ["Greek yoghurt, plain", 410, 9, 5, 4],
    ["Wholemeal bread", 1034, 9, 3.5, 42],
    ["Egg, boiled", 650, 12.6, 10.6, 1.1],
    ["Avocado, raw", 670, 2, 14.7, 1.8],
    ["Tofu, firm", 600, 15, 8, 2.5],
    ["Almonds, raw", 2500, 21, 52, 7],
  ];
  const foods: FoodReference[] = rows.map((r, i) => ({
    id: i + 1,
    dataset_id: 1,
    public_food_key: `DEMO-F${String(i + 1).padStart(4, "0")}`,
    survey_id: `DEMO-S${i + 1}`,
    official_name: r[0],
    display_name: r[0],
    description:
      "Illustrative food reference for interface testing; not an official AUSNUT value.",
    energy_kj_100g: r[1],
    protein_g_100g: r[2],
    fat_g_100g: r[3],
    carbs_g_100g: r[4],
    is_active: true,
  }));
  const exercises: [string, number][] = [
    ["Walking", 3.5],
    ["Running", 8],
    ["Cycling", 6.8],
    ["Swimming", 6],
    ["Yoga", 2.5],
    ["Badminton", 5.5],
    ["Strength training", 3.5],
    ["Dancing", 4.8],
  ];
  const ds = {
    id: 1,
    release: "demo",
    revision: "demo-v1",
    source_url: "",
    content_sha256: "0".repeat(64),
    imported_at: now,
  };
  const db: Database = {
    schemaVersion: 1,
    users,
    profiles,
    foods,
    foodDatasets: [
      {
        ...ds,
        name: "Demo food catalogue · AUSNUT-style fields",
        nutrient_policy: "MVP_CORE_V1",
      },
    ],
    activityDatasets: [
      {
        ...ds,
        name: "Demo activity catalogue · Compendium-style fields",
        min_age: 19,
        max_age: 59,
      },
    ],
    exerciseTypes: exercises.map((r, i) => ({
      id: i + 1,
      dataset_id: 1,
      activity_key: r[0].toUpperCase().replaceAll(" ", "_"),
      display_name: r[0],
      source_activity_code: `DEMO-A${i + 1}`,
      source_description:
        "Illustrative general activity coefficient; not a verified reference import.",
      met_value: r[1],
      is_active: true,
    })),
    weights: [],
    foodRecords: [],
    exerciseRecords: [],
  };
  for (const uid of [1, 3, 4])
    for (const [offset, kg] of [
      [-35, 64],
      [-21, 63.5],
      [-7, 62.8],
      [0, 62.4],
    ])
      db.weights.push({
        id: db.weights.length + 1,
        user_id: uid,
        recorded_date: shiftDate(t, offset),
        weight_kg: kg,
        created_at: now,
        updated_at: now,
        version: 0,
      });
  for (const uid of [1, 3, 4])
    for (let day = -13; day <= 0; day++) {
      if (day === -9 || day === -3) continue;
      const date = shiftDate(t, day),
        factor = 1 + ((day + 13) % 4) * 0.06;
      const entries: [
        number,
        number,
        "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK",
      ][] = [
        [1, 60, "BREAKFAST"],
        [2, 180, "BREAKFAST"],
        [3, 118, "BREAKFAST"],
        [4, 150, "LUNCH"],
        [5, 200, "LUNCH"],
        [6, 120, "LUNCH"],
        [7, 180, "DINNER"],
        [8, 160, "DINNER"],
        [10, 150, "SNACK"],
      ];
      for (const [foodId, grams, meal] of day === 0
        ? entries.slice(0, 6)
        : entries) {
        const q = Math.round(grams * factor);
        db.foodRecords.push({
          id: db.foodRecords.length + 1,
          user_id: uid,
          client_entry_id: crypto.randomUUID(),
          recorded_date: date,
          food_reference_id: foodId,
          quantity_grams: q,
          meal_type: meal,
          ...calculateFood(foods[foodId - 1], q),
          created_at: now,
          updated_at: now,
          version: 0,
          deleted_at: null,
        });
      }
      const type = db.exerciseTypes[(day + 14) % 3],
        duration = day === 0 ? 35 : 25 + ((day + 13) % 5) * 10;
      db.exerciseRecords.push({
        id: db.exerciseRecords.length + 1,
        user_id: uid,
        client_entry_id: crypto.randomUUID(),
        recorded_date: date,
        exercise_type_id: type.id,
        duration_minutes: duration,
        ...estimateExercise(
          type,
          db.activityDatasets[0],
          profiles.find((p) => p.user_id === uid)!,
          db.weights,
          date,
          duration,
        ),
        created_at: now,
        updated_at: now,
        version: 0,
        deleted_at: null,
      });
    }
  return db;
}
