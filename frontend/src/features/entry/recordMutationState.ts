import {isStandaloneFoodDraft,standaloneFoods,legacyExerciseId,exerciseDurationInput} from './draftSelectors'
import type {DraftAction,DraftState} from './draftTypes'
import type {RecordMutationAction,RecordMutationIntent} from './recordMutationTypes'
import {mutationExpectedVersion} from './recordMutationTypes'
import {isCorrectionDirty,matchesCorrectionIntent} from './correctionIntent'
import {parseQuantity} from '../../domain/inputRules'
import {DEFAULT_ZONE} from '../../domain/dates'
export function isRecordMutationAction(action:DraftAction):action is RecordMutationAction{return action.type==='BEGIN_RECORD_MUTATION'||action.type==='RESET_RECORD_MUTATION'||action.type.startsWith('RECORD_MUTATION_')}
export function pendingDraftOperation(state:DraftState):'saving'|'unknown'|null{const create=state.submission.status,record=state.recordMutation.status;return create==='saving'||record==='saving'?'saving':create==='unknown'||record==='unknown'?'unknown':null}
function immutable<T>(value:T):T{const copy=structuredClone(value);function freeze(v:unknown){if(v&&typeof v==='object'){Object.values(v).forEach(freeze);Object.freeze(v)}}freeze(copy);return copy}
function matchesDraft(state:DraftState,intent:RecordMutationIntent,revision?:number){
  if(intent.recordId!==intent.original.id||!Number.isSafeInteger(mutationExpectedVersion(intent))||mutationExpectedVersion(intent)<0||mutationExpectedVersion(intent)!==intent.original.version||(intent.kind==='food')!==('nutrients' in intent.original))return false
  const draft=state.draft
  if(intent.operation==='DELETE')return !draft
  const target=draft?.edit
  if(!draft||!target||!target.original||revision!==draft.revision||!isCorrectionDirty(draft)||draft.kind!==intent.kind||target.recordId!==intent.recordId||target.expectedVersion!==intent.body.expectedVersion||draft.source!==intent.original.source||target.original.clientEntryId!==intent.original.clientEntryId||draft.recordDate!==intent.body.recordDate||intent.body.zone!==DEFAULT_ZONE)return false
  if(draft.kind==='food'&&isStandaloneFoodDraft(draft)&&intent.kind==='food'){
    const row=standaloneFoods(draft)[0],grams=row&&parseQuantity(row.quantityGramsInput,'grams')
    return standaloneFoods(draft).length===1&&!!grams?.ok&&row.clientEntryId===intent.original.clientEntryId&&row.referenceId===row.selectedReference?.referenceId&&row.referenceId===intent.body.referenceId&&grams.value===intent.body.quantityGrams&&row.mealType===intent.body.mealType
  }
  if(draft.kind==='exercise'&&intent.kind==='exercise'){const minutes=parseQuantity(exerciseDurationInput(draft),'minutes');return minutes.ok&&draft.clientEntryId===intent.original.clientEntryId&&legacyExerciseId(draft)===intent.body.exerciseTypeId&&minutes.value===intent.body.durationMinutes&&!!intent.body.calculationFingerprint}
  return false
}
export function reduceRecordMutation(state:DraftState,action:RecordMutationAction):DraftState{
  const current=state.recordMutation;if(current.apiVersion===2)return state;const frozen=current.status==='saving'||current.status==='unknown',createFrozen=state.submission.status==='saving'||state.submission.status==='unknown'
  if(action.type==='RESET_RECORD_MUTATION')return frozen||createFrozen?state:{...state,recordMutation:{status:'idle'}}
  if(action.type==='BEGIN_RECORD_MUTATION'){
    if(frozen||createFrozen||!matchesDraft(state,action.intent,action.expectedRevision)||!/^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(action.mutationId))return state
    return {...state,recordMutation:{status:'saving',intent:immutable(action.intent),ownerEpoch:action.ownerEpoch,mutationId:action.mutationId}}
  }
  if(current.status==='idle'||current.ownerEpoch!==action.ownerEpoch||current.mutationId!==action.mutationId)return state
  if(action.type==='RECORD_MUTATION_REFRESH')return current.status==='saved'?{...state,recordMutation:{...current,refreshFailed:action.failed}}:state
  if(!frozen)return state
  if(action.type==='RECORD_MUTATION_RESUME')return current.status==='unknown'?{...state,recordMutation:{...current,status:'saving'}}:state
  if(action.type==='RECORD_MUTATION_UNKNOWN')return {...state,recordMutation:{...current,status:'unknown'}}
  if(action.type==='RECORD_MUTATION_REJECTED'){
    const observed=action.observedRecord
    if(observed&&(observed.id!==current.intent.recordId||observed.clientEntryId!==current.intent.original.clientEntryId||observed.source!==current.intent.original.source||observed.version<=mutationExpectedVersion(current.intent)))return {...state,recordMutation:{...current,status:'unknown'}}
    const preview=action.changedPreview,intent=current.intent,bound=preview&&action.error.code==='CALCULATION_CHANGED'&&intent.operation==='UPDATE'&&intent.kind==='exercise'&&preview.exerciseType.id===intent.body.exerciseTypeId&&preview.recordDate===intent.body.recordDate&&preview.durationMinutes===intent.body.durationMinutes
    return action.error.status>=400&&action.error.status<500?{...state,recordMutation:{...current,status:'rejected',error:action.error,...(observed?{observedRecord:immutable(observed)}:{}),...(bound?{changedPreview:immutable(preview)}:{})}}:{...state,recordMutation:{...current,status:'unknown'}}
  }
  if(action.type==='RECORD_MUTATION_CONFIRMED'){
    const valid=current.intent.operation==='DELETE'?(action.result==='DELETED'||action.result==='UNAVAILABLE'):(action.result==='UPDATED'||action.result==='MATCHES')&&matchesCorrectionIntent(current.intent,action.record)&&(action.result==='MATCHES'||action.record.version<=current.intent.body.expectedVersion+1)
    if(!valid)return {...state,recordMutation:{...current,status:'unknown'}}
    const result=action.result==='UPDATED'||action.result==='MATCHES'?{result:action.result,record:immutable(action.record)}:{result:action.result}
    return {...state,draft:current.intent.operation==='UPDATE'?null:state.draft,recordMutation:{...current,...result,status:'saved',refreshFailed:false}}
  }
  return state
}
