import { test } from "node:test";
import assert from "node:assert/strict";
import { emptyProfile, newDraft, newFood, today } from "../src/domain.ts";
class MemoryStorage {
  data = new Map<string, string>();
  getItem(k: string) {
    return this.data.get(k) ?? null;
  }
  setItem(k: string, v: string) {
    this.data.set(k, v);
  }
  removeItem(k: string) {
    this.data.delete(k);
  }
}
Object.defineProperty(globalThis, "localStorage", {
  value: new MemoryStorage(),
  configurable: true,
});
Object.defineProperty(globalThis, "sessionStorage", {
  value: new MemoryStorage(),
  configurable: true,
});
Object.defineProperty(globalThis, "window", {
  value: { addEventListener() {} },
  configurable: true,
});
const api = await import("../src/store.ts");
await api.ready();
test("service integration: failure atomicity, registration, role restrictions and Q&A eligibility", async () => {
  await assert.rejects(api.login("jessica@example.com", "wrong"), /Incorrect/);
  await assert.rejects(
    api.login("priya@example.com", "FitNutri123!"),
    /deactivated/,
  );
  await api.login("JESSICA@example.com", "FitNutri123!");
  const initial = JSON.stringify(api.database());
  const draft = {
    ...newDraft(),
    foods: [
      {
        ...newFood(),
        food_reference_id: 4,
        quantity_grams: "100",
        meal_type: "LUNCH" as const,
        reference_confirmed: true,
        grams_confirmed: true,
      },
    ],
  };
  api.setFailure("save");
  await assert.rejects(api.saveDraft(draft), /Could not save/);
  assert.equal(JSON.stringify(api.database()), initial);
  await Promise.all([api.saveDraft(draft), api.saveDraft(draft)]);
  assert.equal(
    api
      .database()
      .foodRecords.filter(
        (r) => r.client_entry_id === draft.foods[0].client_entry_id,
      ).length,
    1,
  );
  await assert.rejects(api.setAccountEnabled(3, false, 0), /Administrator/);
  assert.match(
    await api.answerQuestion("What have I recorded today?"),
    /confirmed records today/,
  );
  assert.match(
    await api.answerQuestion("Can you diagnose my chest pain?"),
    /outside the scope/,
  );
  const profile = { ...api.currentProfile()!, qa_consent_granted: false };
  await api.saveProfile(profile, "Jessica Liu", "", today());
  await assert.rejects(api.answerQuestion("What is protein?"), /unavailable/);
  await api.login("admin@example.com", "FitNutri123!");
  let target = api.database().users.find((u) => u.id === 3)!;
  await api.setAccountEnabled(3, false, target.version);
  await assert.rejects(
    api.login("marcus@example.com", "FitNutri123!"),
    /deactivated/,
  );
  target = api.database().users.find((u) => u.id === 3)!;
  await api.setAccountEnabled(3, true, target.version);
  await assert.rejects(
    api.register("Duplicate", "JESSICA@EXAMPLE.COM", "password123"),
    /already registered/,
  );
  const count = api.database().users.length;
  api.setFailure("save");
  await assert.rejects(
    api.register("Failed", "failed@example.com", "password123"),
    /Could not save/,
  );
  assert.equal(api.database().users.length, count);
  const id = await api.register(
    "Test Person",
    "TEST@example.com",
    "password123",
  );
  assert.equal(api.database().users.find((u) => u.id === id)!.role, "USER");
  await api.login("test@example.com", "password123");
  assert.equal(api.currentProfile()!.onboarding_completed, false);
  await api.saveProfile(
    {
      ...api.currentProfile()!,
      adult_confirmed: true,
      birth_year: 1990,
      sex: "OTHER",
      height_cm: 175,
      activity_level: "LIGHT",
      health_safety_answer: "PREFER_NOT_TO_ANSWER",
    },
    "Test Person",
    "70",
    today(),
  );
  assert.equal(api.currentProfile()!.onboarding_completed, true);
  assert.equal(
    api.database().weights.filter((w) => w.user_id === id).length,
    1,
  );
  await api.saveProfile(
    { ...api.currentProfile()! },
    "Test Person",
    "71",
    today(),
  );
  assert.equal(
    api.database().weights.filter((w) => w.user_id === id).length,
    1,
  );
  assert.equal(
    api.database().weights.find((w) => w.user_id === id)!.weight_kg,
    71,
  );
  api.logout();
  assert.equal(api.currentUser(), undefined);
});
test("voice samples preserve per-food grams and meal context; unspecified details stay empty", () => {
  const parsed = api.parseTranscript(
    "For breakfast today I had 60 grams of oats and 150 g of milk. For lunch I had 200 g of brown rice.",
    "food",
  );
  assert.equal(parsed.foods![0].quantity_grams, "60");
  assert.equal(parsed.foods![0].meal_type, "BREAKFAST");
  assert.equal(
    parsed.foods!.find((f) => f.food_reference_id === 5)!.quantity_grams,
    "200",
  );
  assert.equal(
    parsed.foods!.find((f) => f.food_reference_id === 5)!.meal_type,
    "LUNCH",
  );
  const missing = api.parseTranscript("Yesterday I had an apple", "food");
  assert.equal(missing.foods![0].quantity_grams, "");
  assert.equal(missing.foods![0].meal_type, "");
  assert.ok(missing.recorded_date! < today());
  const exercise = api.parseTranscript(
    "Yesterday I went walking for 35 minutes.",
    "exercise",
  );
  assert.equal(exercise.exercise_type_id, 1);
  assert.equal(exercise.duration_minutes, "35");
});
