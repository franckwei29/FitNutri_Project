import { before, test } from "node:test";
import assert from "node:assert/strict";
import {
  aggregate,
  calculateFood,
  commitDraft,
  editDraft,
  eligible,
  energy,
  estimateExercise,
  newDraft,
  newFood,
  shiftDate,
  today,
  trends,
  validateDraft,
  validDate,
} from "../src/domain.ts";
import type { Database } from "../src/domain.ts";
import { createSeed, DEMO_PASSWORD, verifyPassword } from "../src/seed.ts";
let db: Database;
before(async () => {
  db = await createSeed();
});
function foodDraft(date = today()) {
  return {
    ...newDraft(),
    recorded_date: date,
    foods: [
      {
        ...newFood(),
        food_reference_id: 4,
        quantity_grams: "150",
        meal_type: "LUNCH" as const,
        reference_confirmed: true,
        grams_confirmed: true,
      },
      {
        ...newFood(),
        food_reference_id: 5,
        quantity_grams: "200",
        meal_type: "DINNER" as const,
        reference_confirmed: true,
        grams_confirmed: true,
      },
    ],
  };
}
test("portion calculation scales all four nutrients; kJ/kcal is a display conversion", () => {
  const n = calculateFood(db.foods[3], 150);
  assert.deepEqual(n, {
    energy_kj: 1038,
    protein_grams: 46.5,
    fat_grams: 5.4,
    carbs_grams: 0,
  });
  assert.equal(energy(8368, "KCAL"), 2000);
  assert.equal(energy(8368, "KJ"), 8368);
});
test("a confirmed batch can contain different meals; a past date leaves today unchanged", () => {
  const d = foodDraft(shiftDate(today(), -2)),
    before = aggregate(db, 1, today(), today());
  const saved = commitDraft(db, 1, d);
  assert.equal(aggregate(saved, 1, today(), today()).intake, before.intake);
  assert.equal(saved.foodRecords.length, db.foodRecords.length + 2);
  assert.deepEqual(
    saved.foodRecords.slice(-2).map((r) => r.meal_type),
    ["LUNCH", "DINNER"],
  );
  assert.equal(
    aggregate(saved, 1, d.recorded_date, d.recorded_date).intake,
    aggregate(db, 1, d.recorded_date, d.recorded_date).intake + 1950,
  );
});
test("duplicate save retries keep one record per item, including soft-deleted retry keys", () => {
  const draft = foodDraft(),
    once = commitDraft(db, 1, draft),
    twice = commitDraft(once, 1, draft);
  assert.equal(twice.foodRecords.length, once.foodRecords.length);
  once.foodRecords.at(-1)!.deleted_at = new Date().toISOString();
  const retried = commitDraft(once, 1, draft);
  assert.equal(retried.foodRecords.length, once.foodRecords.length);
});
test("invalid batches leave the original database unchanged and locate item fields", () => {
  const d = foodDraft();
  d.foods[1].quantity_grams = "0";
  const original = JSON.stringify(db);
  assert.equal(
    validateDraft(d, db, true).grams1,
    "Enter a portion greater than 0 and up to 10,000 g.",
  );
  assert.throws(() => commitDraft(db, 1, d));
  assert.equal(JSON.stringify(db), original);
});
test("a review cannot save an unconfirmed reference or grams", () => {
  const d = foodDraft();
  d.foods[1].grams_confirmed = false;
  assert.ok(validateDraft(d, db, true).confirm1);
  assert.throws(() => commitDraft(db, 1, d));
  d.foods[1].grams_confirmed = true;
  d.foods[1].meal_type = "" as any;
  assert.ok(validateDraft(d, db, true).meal1);
});
test("exercise uses the latest weight on or before the activity date", () => {
  const p = db.profiles[0],
    type = db.exerciseTypes[0],
    date = shiftDate(today(), -10);
  const e = estimateExercise(
    type,
    db.activityDatasets[0],
    p,
    db.weights,
    date,
    60,
  );
  assert.equal(e.weight_date_used, shiftDate(today(), -21));
  assert.equal(e.weight_kg_used, 63.5);
  assert.equal(e.estimated_kj, 929.894);
  assert.equal(e.formula_version, "MET_KG_H_GROSS_KJ_V1");
});
test("missing weights and inapplicable references are unavailable, not zero", () => {
  const p = db.profiles[0],
    type = db.exerciseTypes[0],
    ds = db.activityDatasets[0];
  const missing = estimateExercise(type, ds, p, [], today(), 30);
  assert.equal(missing.estimate_status, "MISSING_WEIGHT");
  assert.equal(missing.estimated_kj, null);
  const age = estimateExercise(
    type,
    ds,
    { ...p, birth_year: 1960 },
    db.weights,
    today(),
    30,
  );
  assert.equal(age.estimate_status, "NO_APPLICABLE_REFERENCE");
  assert.equal(age.estimated_kj, null);
  const d = {
    ...newDraft("exercise"),
    recorded_date: shiftDate(today(), -90),
    exercise_type_id: 1,
    duration_minutes: "30",
  };
  const saved = commitDraft(db, 1, d);
  assert.equal(saved.exerciseRecords.at(-1)!.estimated_kj, null);
});
test("later weight changes do not rewrite saved estimates; explicit corrections recalculate", () => {
  const changed = structuredClone(db),
    r = changed.exerciseRecords.find(
      (r) => r.user_id === 1 && r.recorded_date === today(),
    )!,
    original = r.estimated_kj;
  changed.weights.find(
    (w) => w.user_id === 1 && w.recorded_date === today(),
  )!.weight_kg = 80;
  assert.equal(r.estimated_kj, original);
  const d = editDraft(r, "exercise");
  const updated = commitDraft(changed, 1, d);
  assert.notEqual(
    updated.exerciseRecords.find((x) => x.id === r.id)!.estimated_kj,
    original,
  );
});
test("date corrections move totals across days and stale edits cannot overwrite", () => {
  const row = db.foodRecords.find(
      (r) => r.user_id === 1 && r.recorded_date === today(),
    )!,
    oldTotal = aggregate(db, 1, today(), today()).intake;
  const d = editDraft(row, "food");
  d.recorded_date = shiftDate(today(), -1);
  const saved = commitDraft(db, 1, d);
  assert.equal(
    aggregate(saved, 1, today(), today()).intake,
    oldTotal - row.energy_kj,
  );
  assert.throws(() => commitDraft(saved, 1, d), /changed/);
});
test("history respects ownership and shows missing days as null", () => {
  const gap = shiftDate(today(), -3);
  const values = trends(db, 1, gap, today(), "KJ");
  assert.equal(values[0].intake, null);
  assert.equal(values[0].expenditure, null);
  assert.equal(
    aggregate(db, 2, shiftDate(today(), -14), today()).daysLogged,
    0,
  );
  assert.throws(() => commitDraft(db, 5, foodDraft()), /cannot save/);
  const foreign = editDraft(
    db.foodRecords.find((r) => r.user_id === 3)!,
    "food",
  );
  assert.throws(() => commitDraft(db, 1, foreign), /changed/);
});
test("only No with consent enables Q&A", () => {
  const p = db.profiles[0];
  for (const answer of [
    "NO",
    "YES_OR_UNSURE",
    "PREFER_NOT_TO_ANSWER",
    null,
  ] as const)
    for (const consent of [true, false])
      assert.equal(
        eligible({
          ...p,
          health_safety_answer: answer,
          qa_consent_granted: consent,
        }),
        answer === "NO" && consent,
      );
});
test("dates reject future and impossible calendar dates", () => {
  assert.equal(validDate(today()), true);
  assert.equal(validDate(shiftDate(today(), 1)), false);
  assert.equal(validDate("2026-02-30"), false);
  assert.equal(validDate(""), false);
});
test("seeded passwords use per-account salted hashes and verify correctly", async () => {
  assert.ok(db.users.every((u) => !u.password_hash.includes(DEMO_PASSWORD)));
  assert.notEqual(db.users[0].password_hash, db.users[1].password_hash);
  assert.equal(
    await verifyPassword(DEMO_PASSWORD, db.users[0].password_hash),
    true,
  );
  assert.equal(
    await verifyPassword("incorrect", db.users[0].password_hash),
    false,
  );
});
