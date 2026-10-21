import {rootCorrectionDirty} from './rootCorrectionIntent'
import {standaloneFoods,legacyExerciseId,exerciseDurationInput,hasGroupedEntry} from './draftSelectors'
import type {ExerciseEstimate,ExerciseRecord,ExerciseUpdateRequest,FoodRecord,FoodUpdateRequest,MealType} from '../../api/contracts'
import type {Draft,FoodDraft} from './draftTypes'
import {validateDraft} from './draftValidation'
import {DEFAULT_ZONE,todayInZone} from '../../domain/dates'
import {parseQuantity} from '../../domain/inputRules'
import {savedRecordLocation} from '../history/historySearch'
export type CorrectionIntent={kind:'food';recordId:string;original:FoodRecord;body:FoodUpdateRequest}|{kind:'exercise';recordId:string;original:ExerciseRecord;body:ExerciseUpdateRequest;reviewedEstimate:ExerciseEstimate}
function freeze(value:unknown){if(value&&typeof value==='object'){Object.values(value).forEach(freeze);Object.freeze(value)}}
export function loadEditDraft(ownerId:string,kind:'food'|'exercise',record:FoodRecord|ExerciseRecord):Draft{
  if((kind==='food')!==('nutrients' in record))throw new Error('Load the original record identity before editing.')
  const original=structuredClone(record);freeze(original)
  const base={ownerId,source:original.source,recordDate:original.recordDate,revision:0,returnTo:savedRecordLocation(kind,original.id,original.recordDate),edit:{recordId:original.id,expectedVersion:original.version,originalRecordDate:original.recordDate,original}}
  if('nutrients' in original)return {...base,kind:'food',defaultMealType:original.mealType,composer:null,selectionTarget:null,composerQuery:'',entries:[{entryKind:'SINGLE',rowKey:crypto.randomUUID(),clientEntryId:original.clientEntryId,referenceId:original.reference?.referenceId??null,selectedReference:original.reference?structuredClone(original.reference):null,quantityGramsInput:String(original.quantityGrams),mealType:original.mealType}]}
  return {...base,kind:'exercise',entryKind:'QUICK',clientEntryId:original.clientEntryId,period:{clientEntryId:original.clientEntryId,periodKind:'ACTIVITY',selection:{kind:'REFERENCE',referenceId:original.exerciseType?.id??null},durationMinutesInput:String(original.durationMinutes),equipmentDetails:{},movements:[]}}
}
export function foodCalculationChanged(draft:FoodDraft){const original=draft.edit?.original,row=standaloneFoods(draft)[0];return !original||!('nutrients' in original)||standaloneFoods(draft).length!==1||!row||row.referenceId!==(original.reference?.referenceId??null)||!parseQuantity(row.quantityGramsInput,'grams').ok||Number(row.quantityGramsInput)!==original.quantityGrams}
export function isCorrectionDirty(draft:Draft){
  if(draft.edit?.root)return rootCorrectionDirty(draft)
  const original=draft.edit?.original;if(!original)return false
  if(draft.kind==='food'&&'nutrients' in original)return foodCalculationChanged(draft)||draft.recordDate!==original.recordDate||standaloneFoods(draft)[0]?.mealType!==original.mealType
  if(draft.kind==='exercise'&&!('nutrients' in original))return draft.recordDate!==original.recordDate||legacyExerciseId(draft)!==(original.exerciseType?.id??null)||!parseQuantity(exerciseDurationInput(draft),'minutes').ok||Number(exerciseDurationInput(draft))!==original.durationMinutes
  return true
}
export function prepareCorrection(draft:Draft,ownerId:string,estimate?:ExerciseEstimate):CorrectionIntent{
  if(hasGroupedEntry(draft))throw new Error('Grouped or option-based entries require whole-root correction.');
  const target=draft.edit,original=target?.original
  if(!target||!original||target.recordId!==original.id||target.expectedVersion!==original.version||draft.source!==original.source)throw new Error('Load and retain the original record identity, source and version.')
  const errors=validateDraft(draft,ownerId,todayInZone());if(errors.length)throw new Error(errors[0].message)
  if(!isCorrectionDirty(draft))throw new Error('No changes to confirm.')
  const common={expectedVersion:target.expectedVersion,recordDate:draft.recordDate,zone:DEFAULT_ZONE}
  if(draft.kind==='food'&&'nutrients' in original){const row=standaloneFoods(draft)[0];if(row.clientEntryId!==original.clientEntryId)throw new Error('Retain the original record identity.');return {kind:'food',recordId:target.recordId,original,body:{...common,referenceId:row.referenceId!,quantityGrams:Number(row.quantityGramsInput),mealType:row.mealType as MealType}}}
  if(draft.kind==='exercise'&&!('nutrients' in original)){
    const minutes=parseQuantity(exerciseDurationInput(draft),'minutes'),ownOriginal=estimate?.exerciseType.id===original.exerciseType?.id
    if(draft.clientEntryId!==original.clientEntryId)throw new Error('Retain the original record identity.')
    if(!estimate||estimate.exerciseType.id!==legacyExerciseId(draft)||(!ownOriginal&&!estimate.exerciseType.active)||estimate.recordDate!==draft.recordDate||!minutes.ok||estimate.durationMinutes!==minutes.value||!estimate.calculationFingerprint)throw new Error('Load and review the current activity estimate before saving changes.')
    return {kind:'exercise',recordId:target.recordId,original,reviewedEstimate:structuredClone(estimate),body:{...common,exerciseTypeId:legacyExerciseId(draft)!,durationMinutes:minutes.value,calculationFingerprint:estimate.calculationFingerprint}}
  }
  throw new Error('Load the original record identity before editing.')
}
/** State comparison only. A later matching value does not identify which request wrote it. */
export function matchesCorrectionIntent(intent:CorrectionIntent,record:FoodRecord|ExerciseRecord){
  if(record.id!==intent.recordId||record.clientEntryId!==intent.original.clientEntryId||record.source!==intent.original.source||record.version<intent.body.expectedVersion||record.recordDate!==intent.body.recordDate)return false
  if(intent.kind==='food'&&'nutrients' in record)return record.reference?.referenceId===intent.body.referenceId&&record.quantityGrams===intent.body.quantityGrams&&record.mealType===intent.body.mealType
  return intent.kind==='exercise'&&!('nutrients' in record)&&record.exerciseType?.id===intent.body.exerciseTypeId&&record.durationMinutes===intent.body.durationMinutes
}
