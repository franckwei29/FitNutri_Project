import type {Draft,FoodDraft,DraftFood,ExerciseDraft,DraftPeriod,FoodRootDraft} from './draftTypes'
/** Compatibility views are derived from the single domain state; they never own mutable arrays. */
export function standaloneFoods(draft:FoodDraft):DraftFood[]{return draft.entries.filter((entry):entry is DraftFood=>entry.entryKind==='SINGLE')}
export function isStandaloneFoodDraft(draft:FoodDraft):boolean{return draft.entries.every(entry=>entry.entryKind==='SINGLE')}
export function exercisePeriods(draft:ExerciseDraft):DraftPeriod[]{return draft.entryKind==='QUICK'?[draft.period]:draft.periods}
export function legacyExerciseId(draft:ExerciseDraft):string|null{return draft.entryKind==='QUICK'&&draft.period.selection.kind==='REFERENCE'?draft.period.selection.referenceId:null}
export function exerciseDurationInput(draft:ExerciseDraft):string{return draft.entryKind==='QUICK'?draft.period.durationMinutesInput:''}
export function foodLeafCount(entries:FoodRootDraft[]):number{return entries.reduce((count,entry)=>count+(entry.entryKind==='SINGLE'?1:entry.ingredients.length),0)}
export function foodIdentities(entries:FoodRootDraft[]):string[]{return entries.flatMap(entry=>entry.entryKind==='SINGLE'?[entry.rowKey,entry.clientEntryId]:[entry.rowKey,entry.clientEntryId,...entry.ingredients.flatMap(i=>[i.rowKey,i.clientEntryId])])}
export function hasGroupedEntry(draft:Draft):boolean{return draft.kind==='food'?!isStandaloneFoodDraft(draft):draft.entryKind==='WORKOUT'||draft.period.selection.kind==='OPTION'}
/** A bounded legacy adapter may only operate on genuinely standalone rows; it cannot flatten a recipe. */
export function requireStandaloneFoods(draft:FoodDraft):DraftFood[]{const entries=draft.entries;if(!entries.every((entry):entry is DraftFood=>entry.entryKind==='SINGLE'))throw new Error('Use grouped review for a dish.');return entries}
/** A selected or typed unadded item must be resolved explicitly before Review. */
export function composerHasInput(root:FoodRootDraft|null):boolean {if(!root)return false;if(root.entryKind==='SINGLE')return !!root.referenceId||!!root.quantityGramsInput.trim()||!!root.suggestion;return Object.values(root.ingredientQueries??{}).some(q=>q.trim())||!!root.nameInput.trim()||root.ingredients.some(i=>!!i.referenceId||!!i.quantityGramsInput.trim())||root.portion.kind==='FRACTION'&&(!!root.portion.numeratorInput.trim()||!!root.portion.denominatorInput.trim())||root.portion.kind==='PERCENT'&&!!root.portion.percentInput.trim()||root.portion.kind==='SERVINGS'&&(!!root.portion.totalServingsInput.trim()||!!root.portion.eatenServingsInput.trim())}

/** Explicit legacy adapter for an already selected reference-based Quick draft. New option/Workout inputs use V2. */
export function usesExerciseV2(draft:ExerciseDraft):boolean{return draft.entryKind==='WORKOUT'||draft.period.selection.kind==='OPTION'||draft.period.selection.referenceId===null}
