import type {FoodDraft} from '../entry/draftTypes'
/** Display-only flattened view; grouped writes must always retain their roots and original recipe grams. */
export function flattenFoodLeaves(draft:FoodDraft){return draft.entries.flatMap(root=>root.entryKind==='SINGLE'?[{rootClientEntryId:root.clientEntryId,mealType:root.mealType,leaf:root}]:root.ingredients.map(leaf=>({rootClientEntryId:root.clientEntryId,mealType:root.mealType,leaf})))}
