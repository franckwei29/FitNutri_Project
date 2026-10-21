import {standaloneFoods,legacyExerciseId,exerciseDurationInput,hasGroupedEntry} from './draftSelectors'
import type {ExerciseEstimate,MealType} from '../../api/contracts'
import type {CreatePayload,Draft} from './draftTypes'
import {validateDraft} from './draftValidation'
import {DEFAULT_ZONE,todayInZone} from '../../domain/dates'
import {parseQuantity} from '../../domain/inputRules'
/** New UUIDs are minted only on explicit confirmation, never on unknown-result retry. */
export function prepareSubmission(draft:Draft,ownerId:string,estimate?:ExerciseEstimate):CreatePayload{
  if(hasGroupedEntry(draft))throw new Error('Grouped or option-based entries require the V2 review flow.');
  if(draft.edit)throw new Error('Existing records require the correction flow.')
  const errors=validateDraft(draft,ownerId,todayInZone());if(errors.length)throw new Error(errors[0].message)
  const common={recordDate:draft.recordDate,zone:DEFAULT_ZONE,source:draft.source}
  if(draft.kind==='food')return {...common,submissionId:crypto.randomUUID(),items:standaloneFoods(draft).map(row=>({clientEntryId:row.clientEntryId,referenceId:row.referenceId!,quantityGrams:Number(row.quantityGramsInput),mealType:row.mealType as MealType}))}
  const minutes=parseQuantity(exerciseDurationInput(draft),'minutes')
  if(!estimate||estimate.exerciseType.id!==legacyExerciseId(draft)||!estimate.exerciseType.active||estimate.recordDate!==draft.recordDate||!minutes.ok||estimate.durationMinutes!==minutes.value||!estimate.calculationFingerprint)throw new Error('Load and review the current activity estimate before saving.')
  return {...common,submissionId:crypto.randomUUID(),clientEntryId:draft.clientEntryId,exerciseTypeId:legacyExerciseId(draft)!,durationMinutes:minutes.value,calculationFingerprint:estimate.calculationFingerprint}
}
