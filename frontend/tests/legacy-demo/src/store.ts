import {
  aggregate,
  commitDraft,
  eligible,
  emptyProfile,
  showEnergy,
  shiftDate,
  today,
  validateProfile,
} from "./domain.ts";
import type { Database, Draft, Profile } from "./domain.ts";
import { createSeed, hashPassword, verifyPassword } from "./seed.ts";
const KEY = "fitnutri-demo:v1",
  SESSION = "fitnutri-demo:session";
let db: Database;
let session: number | null = Number(sessionStorage.getItem(SESSION)) || null;
let failure: "none" | "save" | "processing" | "timeout" = "none";
const listeners = new Set<() => void>();
let revision = 0;
export const subscribe = (fn: () => void) => {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
};
export const snapshot = () => revision;
function emit() {
  revision++;
  listeners.forEach((fn) => fn());
}
export const database = () => db;
export function currentUser() {
  return db?.users.find((u) => u.id === session && u.enabled);
}
export function currentProfile() {
  return db?.profiles.find((p) => p.user_id === currentUser()?.id);
}
function persist(next: Database) {
  localStorage.setItem(KEY, JSON.stringify(next));
  db = next;
  emit();
}
export let storageNotice = "";
export async function initialize() {
  if (db) return;
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (
        parsed.schemaVersion === 1 &&
        [
          "users",
          "profiles",
          "weights",
          "foods",
          "foodDatasets",
          "activityDatasets",
          "exerciseTypes",
          "foodRecords",
          "exerciseRecords",
        ].every((k) => Array.isArray(parsed[k]))
      )
        db = parsed;
      else
        storageNotice =
          "The previous demo data format could not be loaded. A fresh example has been prepared.";
    }
  } catch {
    storageNotice =
      "Saved demo data could not be read. A fresh example has been prepared.";
  }
  if (!db) {
    db = await createSeed();
    persist(db);
  }
  emit();
}
let init: Promise<void> | undefined;
export const ready = () => (init ??= initialize());
export function requireUser() {
  const u = currentUser();
  if (!u) throw Error("Please sign in to continue.");
  return u;
}
export const getFailure = () => failure;
export function setFailure(mode: typeof failure) {
  failure = mode;
  emit();
}
export async function delay(kind: "save" | "processing" | "read" = "read") {
  const chosen = failure;
  if (
    (kind === "save" && chosen === "save") ||
    (kind === "processing" && (chosen === "processing" || chosen === "timeout"))
  ) {
    failure = "none";
    emit();
    await new Promise((r) => setTimeout(r, chosen === "timeout" ? 1800 : 450));
    throw Error(
      chosen === "timeout"
        ? "The demo request timed out. Please retry."
        : kind === "save"
          ? "Could not save this change. Your existing data is unchanged. Please retry."
          : "Processing failed. Retry or switch to manual entry.",
    );
  }
  await new Promise((r) => setTimeout(r, kind === "processing" ? 900 : 220));
}
export async function login(email: string, password: string) {
  await delay();
  const u = db.users.find(
    (u) => u.email.toLowerCase() === email.trim().toLowerCase(),
  );
  if (!u || !(await verifyPassword(password, u.password_hash)))
    throw Error("Incorrect email or password. Please try again.");
  if (!u.enabled)
    throw Error("This account is deactivated. Contact your administrator.");
  session = u.id;
  sessionStorage.setItem(SESSION, String(u.id));
  emit();
  return u;
}
export function logout() {
  session = null;
  sessionStorage.removeItem(SESSION);
  emit();
}
export async function register(name: string, email: string, password: string) {
  if (!name.trim()) throw Error("Enter your display name.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
    throw Error("Enter a valid email address.");
  if (password.length < 8)
    throw Error("Use at least 8 characters for your password.");
  const normalized = email.trim().toLowerCase();
  const hash = await hashPassword(password);
  await delay("save");
  if (db.users.some((u) => u.email.toLowerCase() === normalized))
    throw Error("This email address is already registered. Sign in instead.");
  const next = structuredClone(db),
    id = Math.max(...db.users.map((u) => u.id)) + 1,
    now = new Date().toISOString();
  next.users.push({
    id,
    email: normalized,
    password_hash: hash,
    display_name: name.trim(),
    role: "USER",
    enabled: true,
    created_at: now,
    updated_at: now,
    version: 0,
  });
  next.profiles.push(emptyProfile(id));
  persist(next);
  return id;
}
export async function saveProfile(
  profile: Profile,
  name: string,
  weight: string,
  weightDate: string,
) {
  const u = requireUser();
  if (u.role !== "USER" || profile.user_id !== u.id)
    throw Error("You can only update your own profile.");
  if (!name.trim()) throw Error("Enter a display name.");
  const original = currentProfile()!;
  const errors = validateProfile(
    profile,
    weight,
    weightDate,
    !original.onboarding_completed,
  );
  if (Object.keys(errors).length) throw Error(Object.values(errors)[0]);
  await delay("save");
  if (currentUser()?.id !== u.id)
    throw Error("Your session changed. Sign in again.");
  if (currentProfile()!.version !== profile.version)
    throw Error("Your profile has changed. Reload this page before saving.");
  const next = structuredClone(db),
    now = new Date().toISOString(),
    p = next.profiles.find((p) => p.user_id === u.id)!;
  Object.assign(p, profile, {
    onboarding_completed: true,
    version: p.version + 1,
    updated_at: now,
    health_safety_updated_at:
      p.health_safety_answer !== profile.health_safety_answer ||
      !p.health_safety_updated_at
        ? now
        : p.health_safety_updated_at,
    qa_consent_updated_at:
      p.qa_consent_granted !== profile.qa_consent_granted ||
      !p.qa_consent_updated_at
        ? now
        : p.qa_consent_updated_at,
  });
  const user = next.users.find((v) => v.id === u.id)!;
  user.display_name = name.trim();
  user.updated_at = now;
  user.version++;
  if (weight !== "") {
    const previous = next.weights.find(
      (w) => w.user_id === u.id && w.recorded_date === weightDate,
    );
    if (previous) {
      previous.weight_kg = Number(weight);
      previous.updated_at = now;
      previous.version++;
    } else
      next.weights.push({
        id: Math.max(0, ...next.weights.map((w) => w.id)) + 1,
        user_id: u.id,
        recorded_date: weightDate,
        weight_kg: Number(weight),
        created_at: now,
        updated_at: now,
        version: 0,
      });
  }
  persist(next);
}
export async function saveDraft(draft: Draft) {
  const uid = requireUser().id;
  await delay("save");
  if (currentUser()?.id !== uid)
    throw Error("Your session changed. Please sign in again.");
  persist(commitDraft(db, uid, draft));
}
export async function deleteRecord(
  kind: "food" | "exercise",
  id: number,
  version: number,
) {
  const uid = requireUser().id;
  await delay("save");
  if (currentUser()?.id !== uid) throw Error("Your session changed.");
  const next = structuredClone(db);
  const rows = kind === "food" ? next.foodRecords : next.exerciseRecords;
  const row = rows.find(
    (r) => r.id === id && r.user_id === uid && !r.deleted_at,
  );
  if (!row || row.version !== version)
    throw Error("This record has changed. Reload History before deleting.");
  row.deleted_at = new Date().toISOString();
  row.updated_at = row.deleted_at;
  row.version++;
  persist(next);
}
export async function setAccountEnabled(
  id: number,
  enabled: boolean,
  version: number,
) {
  const u = requireUser();
  if (u.role !== "ADMIN") throw Error("Administrator access is required.");
  await delay("save");
  if (currentUser()?.id !== u.id) throw Error("Your session changed.");
  const next = structuredClone(db);
  const target = next.users.find((v) => v.id === id && v.role === "USER");
  if (!target || target.version !== version)
    throw Error("This account has changed. Reload the account list.");
  target.enabled = enabled;
  target.version++;
  target.updated_at = new Date().toISOString();
  persist(next);
}
export async function resetDemo() {
  const next = await createSeed();
  persist(next);
  logout();
  failure = "none";
  emit();
}
export async function answerQuestion(question: string) {
  const u = requireUser();
  const p = currentProfile();
  if (!eligible(p))
    throw Error(
      "General Health Q&A is unavailable. Recording and summaries remain available.",
    );
  await delay("processing");
  if (currentUser()?.id !== u.id || !eligible(currentProfile()))
    throw Error("Q&A eligibility changed. This request was stopped.");
  if (
    /diabet|cancer|disease|symptom|medicat|medicine|treat|diagnos|pain|pregnan|insulin|emergency|dose|depress|blood pressure|cholesterol|asthma|headache|sick|illness|dizz|bleed|faint|suicid|heart attack|疾病|症状|药|治疗/i.test(
      question,
    )
  )
    return "That question is outside the scope of general wellbeing information. I cannot assess symptoms, diagnose conditions or recommend treatment. Please speak with a qualified health professional.";
  if (/record|today|intake|summary|logged/i.test(question)) {
    const a = aggregate(db, u.id, today(), today());
    return a.daysLogged
      ? `Your confirmed records today contain ${showEnergy(a.intake, p!.energy_unit)} of food intake and ${a.minutes} minutes of activity. ${a.missingEstimates ? "Some activity estimates are unavailable. " : ""}These figures describe what you logged; they do not measure your full day’s energy balance.`
      : "You have no confirmed records for today yet. Once you confirm an entry, it will appear in your overview and factual summary.";
  }
  if (/carb|protein|nutrient/i.test(question))
    return "Carbohydrates provide energy for everyday activity. Protein supports the maintenance and repair of body tissues. Both are part of a varied diet. The amounts shown in your food log come from the food reference and portion you confirm.";
  if (/kcal|kilojoule|energy unit/i.test(question))
    return "Kilojoules (kJ) and kilocalories (kcal) are two units of energy. One kcal equals 4.184 kJ. You can change your display preference in Profile without changing your underlying records.";
  return "This demo supports examples about food nutrients, kJ and kcal, and your confirmed records today. Try “What have I recorded today?” or “What is the difference between carbohydrates and protein?”";
}
export function parseTranscript(
  text: string,
  kind: "food" | "exercise",
): Partial<Draft> {
  const lower = text.toLowerCase();
  const explicit = text.match(/\b\d{4}-\d{2}-\d{2}\b/)?.[0];
  const date =
    explicit ?? (/yesterday/.test(lower) ? shiftDate(today(), -1) : today());
  if (kind === "exercise") {
    const type = db.exerciseTypes.find(
      (t) =>
        lower.includes(t.display_name.toLowerCase()) ||
        (t.activity_key === "RUNNING" && /\bran\b|\brun\b/.test(lower)) ||
        (t.activity_key === "WALKING" && /\bwalk(?:ed)?\b/.test(lower)),
    );
    const minutes = lower.match(/(\d+(?:\.\d+)?)\s*(?:minutes?|mins?)/)?.[1];
    return {
      recorded_date: date,
      exercise_type_id: type?.id ?? null,
      duration_minutes: minutes ?? "",
    };
  }
  const aliases: Record<number, string[]> = {
    1: ["oat"],
    2: ["milk"],
    3: ["banana"],
    4: ["chicken"],
    5: ["rice"],
    6: ["salad"],
    7: ["salmon"],
    8: ["broccoli"],
    9: ["apple"],
    10: ["yoghurt", "yogurt"],
    11: ["bread"],
    12: ["egg"],
    13: ["avocado"],
    14: ["tofu"],
    15: ["almond"],
  };
  const foods = db.foods
    .filter((f) => aliases[f.id]?.some((a) => lower.includes(a)))
    .map((f) => {
      const alias = aliases[f.id].find((a) => lower.includes(a))!;
      const index = lower.indexOf(alias);
      const segmentStart = Math.max(
        lower.lastIndexOf(" and ", index),
        lower.lastIndexOf(";", index),
        lower.lastIndexOf(",", index),
        lower.lastIndexOf(". ", index),
        lower.lastIndexOf("\n", index),
      );
      const nextSeparators = [" and ", ";", ",", ". ", "\n"]
        .map((s) => lower.indexOf(s, index))
        .filter((n) => n >= 0);
      const segmentEnd = nextSeparators.length
        ? Math.min(...nextSeparators)
        : lower.length;
      const segment = lower.slice(
        segmentStart < 0 ? 0 : segmentStart,
        segmentEnd,
      );
      const grams =
        segment.match(/(\d+(?:\.\d+)?)\s*(?:grams?|g)\b/)?.[1] ?? "";
      const meal =
        [...lower.slice(0, index).matchAll(/breakfast|lunch|dinner|snack/g)]
          .at(-1)?.[0]
          ?.toUpperCase() ?? "";
      return {
        client_entry_id: crypto.randomUUID(),
        food_reference_id: f.id,
        quantity_grams: grams,
        meal_type: meal as import("./domain.ts").Meal | "",
        reference_confirmed: false,
        grams_confirmed: false,
      };
    });
  return { recorded_date: date, foods };
}
window.addEventListener("storage", (event) => {
  if (event.key === KEY && event.newValue) {
    try {
      const next = JSON.parse(event.newValue);
      if (next.schemaVersion === 1) {
        db = next;
        emit();
      }
    } catch {
      /* Keep the last valid local state. */
    }
  }
});
