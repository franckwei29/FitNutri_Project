import type { EnergyUnit, EntrySource, MealType, SafetyAnswer } from '../api/contracts'
export const mealLabels: Record<MealType, string> = { BREAKFAST: 'Breakfast', LUNCH: 'Lunch', DINNER: 'Dinner', SNACK: 'Snack' }
export const sourceLabels: Record<EntrySource, string> = { MANUAL: 'Manual', VOICE: 'Voice', IMAGE: 'Image' }
export const energyLabels: Record<EnergyUnit, string> = { KJ: 'kJ', KCAL: 'kcal' }
export const safetyLabels: Record<SafetyAnswer, string> = { NO: 'No', YES_OR_UNSURE: 'Yes or unsure', PREFER_NOT_TO_ANSWER: 'Prefer not to answer' }
