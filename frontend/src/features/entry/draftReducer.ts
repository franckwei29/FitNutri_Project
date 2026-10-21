import {reduceRootMutation} from './rootMutationState'
import type { EntrySource, SubmissionReceipt,ExerciseEstimate,MovementCatalogueV2 } from '../../api/contracts'
import {DEFAULT_WORKOUT_TITLE} from './draftDefaults'
import { parseQuantity } from '../../domain/inputRules'
import type { CreatePayload, Draft, DraftAction, DraftFood, DraftState,DraftDish,DraftIngredient,DraftMovement,DraftPeriod,WorkoutDraft,FoodRootDraft } from './draftTypes'
import {standaloneFoods,isStandaloneFoodDraft,legacyExerciseId,exerciseDurationInput,foodIdentities,foodLeafCount,composerHasInput} from './draftSelectors'
import {freezeSubmissionPayload} from './immutable'
export {freezeSubmissionPayload} from './immutable'
import {reduceSubmissionV2} from './submissionV2State'
import {reduceFoodComposer} from './foodComposerReducer'
import {reduceDishDraft} from './dishDraftReducer'
import {reduceGroupedDraft} from './groupedDraftReducer'
import {isRecordMutationAction,reduceRecordMutation} from './recordMutationState'

export function emptyDraftState(ownerId: string | null): DraftState { return {ownerId,draft:null,submission:{status:'idle'},recordMutation:{status:'idle'}} }
/** Call only on an explicit start/add event, never in render or retries. */
export function createFoodRow(): DraftFood {return {entryKind:'SINGLE',rowKey:crypto.randomUUID(),clientEntryId:crypto.randomUUID(),referenceId:null,selectedReference:null,quantityGramsInput:'',mealType:''}}
export function createIngredientRow():DraftIngredient {const row=createFoodRow();return {rowKey:row.rowKey,clientEntryId:row.clientEntryId,referenceId:null,selectedReference:null,quantityGramsInput:''}}
export function createDishRow():DraftDish{return {entryKind:'DISH',rowKey:crypto.randomUUID(),clientEntryId:crypto.randomUUID(),nameInput:'',mealType:'',inputMode:'PERSONAL',portion:{kind:'PERSONAL'},ingredients:[createIngredientRow()]}}
export function createPeriodDraft(periodKind:DraftPeriod['periodKind']='ACTIVITY'):DraftPeriod{return {clientEntryId:crypto.randomUUID(),periodKind,selection:{kind:'OPTION',option:null},durationMinutesInput:'',equipmentDetails:{},movements:[]}}
export function createMovementDraft(catalogue:MovementCatalogueV2):DraftMovement{return {rowKey:crypto.randomUUID(),catalogue:structuredClone(catalogue),setCountInput:'',repetitionsInput:'',loadKgInput:''}}
export function createWorkoutDraft(ownerId:string,recordDate:string,source:EntrySource):WorkoutDraft{return {kind:'exercise',entryKind:'WORKOUT',ownerId,recordDate,source,revision:0,returnTo:'/dashboard',clientEntryId:crypto.randomUUID(),titleInput:DEFAULT_WORKOUT_TITLE,periods:[createPeriodDraft('STRENGTH')]}}
export function createDraft(ownerId:string,kind:'food'|'exercise',recordDate:string,source:EntrySource):Draft {
 const base={ownerId,recordDate,source,revision:0,returnTo:'/dashboard'};if(kind==='food')return {...base,kind,entries:[],defaultMealType:'',composer:source==='MANUAL'?createFoodRow():null,selectionTarget:null,composerQuery:''};const period=createPeriodDraft();period.selection={kind:'REFERENCE',referenceId:null};return {...base,kind,entryKind:'QUICK',clientEntryId:period.clientEntryId,period}
}

/** Structure-only reuse. Amounts and details need an explicit later Use last action. */
export function copyFoodRootForReuse(input:FoodRootDraft,originDishId?:string):FoodRootDraft {const root=structuredClone(input);if(root.entryKind==='SINGLE')return {...root,rowKey:crypto.randomUUID(),clientEntryId:crypto.randomUUID(),mealType:'',quantityGramsInput:'',suggestion:undefined};const portion:DraftDish['portion']=root.portion.kind==='PERSONAL'?{kind:'PERSONAL'}:root.portion.kind==='FRACTION'?{kind:'FRACTION',numeratorInput:'',denominatorInput:''}:root.portion.kind==='PERCENT'?{kind:'PERCENT',percentInput:''}:{kind:'SERVINGS',totalServingsInput:'',eatenServingsInput:''};return {...root,rowKey:crypto.randomUUID(),clientEntryId:crypto.randomUUID(),mealType:'',portion,originDishId,ingredientQueries:{},...reuseIngredients(root)}}
function reuseIngredients(root:DraftDish){const reuseAmounts:Record<string,string>={};const ingredients=root.ingredients.map(i=>{const rowKey=crypto.randomUUID();reuseAmounts[rowKey]=i.quantityGramsInput;return {...i,rowKey,clientEntryId:crypto.randomUUID(),quantityGramsInput:'',suggestion:undefined}});return {ingredients,reuseAmounts}}
export function copyWorkoutForReuse(input:WorkoutDraft,recordDate:string,originWorkoutId?:string):WorkoutDraft {const root=structuredClone(input);return {kind:'exercise',entryKind:'WORKOUT',ownerId:root.ownerId,recordDate,source:'MANUAL',revision:0,returnTo:'/dashboard',clientEntryId:crypto.randomUUID(),titleInput:root.titleInput,originWorkoutId,periods:root.periods.map(p=>({...p,clientEntryId:crypto.randomUUID(),durationMinutesInput:'',equipmentDetails:{},movements:p.movements.map(m=>({...m,rowKey:crypto.randomUUID(),setCountInput:'',repetitionsInput:'',loadKgInput:''}))}))}}
function freezePayload(payload:CreatePayload):CreatePayload{return freezeSubmissionPayload(payload)}
function freezeEstimate(estimate:ExerciseEstimate){return freezeSubmissionPayload(estimate)}
function estimateMatches(payload:CreatePayload,estimate:ExerciseEstimate){return !('items' in payload)&&payload.exerciseTypeId===estimate.exerciseType.id&&payload.recordDate===estimate.recordDate&&payload.durationMinutes===estimate.durationMinutes}
function matchesDraft(draft:Draft,payload:CreatePayload): boolean {
  if(draft.kind==='food'&&(composerHasInput(draft.composer??null)||draft.composerQuery?.trim()||draft.selectionTarget))return false
  if(draft.voiceInputStale||draft.recordDate!==payload.recordDate || draft.source!==payload.source || draft.edit) return false
  if(draft.kind==='food' && isStandaloneFoodDraft(draft) && 'items' in payload) return standaloneFoods(draft).length>0 && standaloneFoods(draft).length<=50 && new Set(standaloneFoods(draft).map(x=>x.clientEntryId)).size===standaloneFoods(draft).length && standaloneFoods(draft).length===payload.items.length && standaloneFoods(draft).every(row=>{
    const item=payload.items.find(x=>x.clientEntryId===row.clientEntryId),grams=parseQuantity(row.quantityGramsInput,'grams')
    return item && grams.ok && row.selectedReference?.active && row.selectedReference.referenceId===row.referenceId && item.referenceId===row.referenceId && item.quantityGrams===grams.value && item.mealType===row.mealType
  })
  if(draft.kind==='exercise' && draft.entryKind==='QUICK' && draft.period.selection.kind==='REFERENCE' && !('items' in payload)) {
    const minutes=parseQuantity(exerciseDurationInput(draft),'minutes')
    return minutes.ok && payload.clientEntryId===draft.clientEntryId && payload.exerciseTypeId===legacyExerciseId(draft) && payload.durationMinutes===minutes.value
  }
  return false
}
export function matchesReceipt(payload:CreatePayload,receipt:SubmissionReceipt): boolean {
  const ids='items' in payload ? payload.items.map(x=>x.clientEntryId) : [payload.clientEntryId]
  return receipt.submissionId===payload.submissionId && receipt.kind===('items' in payload?'FOOD_BATCH':'EXERCISE_CREATE')
    && receipt.records.length===ids.length && new Set(receipt.records.map(x=>x.clientEntryId)).size===ids.length
    && new Set(receipt.records.map(x=>x.id)).size===ids.length
    && receipt.records.every(x=>ids.includes(x.clientEntryId) && x.recordDate===payload.recordDate && x.versionAtCreation===0)
}
function resetDraftMemory(state:DraftState):DraftState{return {...emptyDraftState(state.ownerId),...(state.preferredExerciseMode?{preferredExerciseMode:state.preferredExerciseMode}:{}),...(state.preferredMealType!==undefined?{preferredMealType:state.preferredMealType}:{}),...(state.noticeSequence!==undefined?{noticeSequence:state.noticeSequence}:{})}}
export function draftReducer(state:DraftState,action:DraftAction):DraftState{
 let next=reduceOwnedDraft(state,action)
 const rejected=state.recordMutation,originalEdit=state.draft?.edit,currentEdit=next.draft?.edit
 // Editing a rejected snapshot does not resolve its stale/deleted root. Only an
 // explicit discard/reload can replace this expected version and its rejection.
 if(next!==state&&next.recordMutation!==rejected&&rejected.apiVersion===2&&rejected.status==='rejected'&&['VERSION_CONFLICT','NOT_FOUND'].includes(rejected.error.code)&&originalEdit?.root&&currentEdit?.root&&originalEdit.recordId===currentEdit.recordId&&originalEdit.root.kind===currentEdit.root.kind&&originalEdit.expectedVersion===currentEdit.expectedVersion)next={...next,recordMutation:rejected}
 return next!==state&&next.undoRemoval&&next.undoRemoval!==state.undoRemoval&&'undoToken' in action&&action.undoToken!==undefined?{...next,undoRemoval:{...next.undoRemoval,token:action.undoToken}}:next
}
function reduceOwnedDraft(state:DraftState,action:DraftAction): DraftState {
  if(action.ownerId!==state.ownerId) return state
  if(action.type==='ABANDON_UNKNOWN'){if(state.submission.status==='saving'||state.recordMutation.status==='saving')return state;const pending=action.kind==='SUBMISSION'?state.submission:state.recordMutation;if(pending.status!=='unknown'||pending.ownerEpoch!==action.ownerEpoch||('submissionId' in pending?pending.submissionId:pending.mutationId)!==action.identity)return state;return resetDraftMemory(state)}
  if(action.type==='EXPIRE_UNDO')return state.undoRemoval?.token===action.token?{...state,undoRemoval:undefined}:state
  if(action.type==='END_UNDO_WINDOW')return state.undoRemoval?{...state,undoRemoval:undefined}:state
  if(action.type==='CLEAR_LAST_SAVED')return state.lastSaved?.receipt.submissionId===action.submissionId?{...state,lastSaved:undefined}:state
  const v2=reduceSubmissionV2(state,action);if(v2)return v2
  const rooted=reduceRootMutation(state,action);if(rooted)return rooted
  if(isRecordMutationAction(action))return reduceRecordMutation(state,action)
  const submission=state.submission
  const frozen=submission.status==='saving' || submission.status==='unknown'||state.recordMutation.status==='saving'||state.recordMutation.status==='unknown'
  if(action.type==='SUBMIT_UNKNOWN' || action.type==='SUBMIT_REJECTED' || action.type==='SUBMIT_CONFIRMED') {
    if((submission.status!=='saving'&&submission.status!=='unknown') || submission.apiVersion===2 || submission.ownerEpoch!==action.ownerEpoch) return state
    if(action.type==='SUBMIT_CONFIRMED') {
      if(!matchesReceipt(submission.frozenPayload,action.receipt)) return {...state,submission:{...submission,status:'unknown'}}
      return {...state,draft:null,submission:{status:'saved',receipt:freezeSubmissionPayload(action.receipt),refreshFailed:false}}
    }
    if(action.submissionId!==submission.submissionId) return state
    if(action.type==='SUBMIT_REJECTED' && (action.error.status<400 || action.error.status>=500)) return {...state,submission:{...submission,status:'unknown'}}
    if(action.type==='SUBMIT_UNKNOWN')return {...state,submission:{...submission,status:'unknown'}}
    const preview=action.changedPreview,bound=preview&&action.error.code==='CALCULATION_CHANGED'&&estimateMatches(submission.frozenPayload,preview)
    return {...state,submission:{status:'rejected',error:action.error,...(submission.reviewedEstimate?{reviewedEstimate:submission.reviewedEstimate}:{}),...(bound?{changedPreview:freezeEstimate(preview)}:{})}}
  }
  if(action.type==='REFRESH_FAILED') return submission.status==='saved'?{...state,submission:{...submission,refreshFailed:true}}:state
  if(action.type==='REFRESH_COMPLETED') return submission.status==='saved'?{...state,submission:{...submission,refreshFailed:false}}:state
  if(frozen) return state
  if(action.type==='DISCARD') return resetDraftMemory(state)
  if(action.type==='START_NEW' || action.type==='LOAD_EDIT') {
    if(action.draft.ownerId!==state.ownerId || (state.draft && (action.type==='LOAD_EDIT' || !action.replaceConfirmed))) return state
    if(action.type==='LOAD_EDIT' && (!action.draft.edit || (action.draft.kind==='food' && !action.draft.edit.root && (!isStandaloneFoodDraft(action.draft)||standaloneFoods(action.draft).length!==1)))) return state
    return {...state,...(action.type==='START_NEW'&&action.draft.kind==='exercise'?{preferredExerciseMode:action.draft.entryKind}:{}),draft:structuredClone(action.draft),submission:{status:'idle'},recordMutation:{status:'idle'},undoRemoval:undefined}
  }
  const draft=state.draft
  if(!draft) return state
  const composed=reduceFoodComposer(state,action);if(composed)return composed
  const dished=reduceDishDraft(state,action);if(dished)return dished
  const grouped=reduceGroupedDraft(state,action);if(grouped)return grouped
  if(action.type==='IMPORT_IMAGE_CANDIDATES') {
    if(draft.kind!=='food'||!isStandaloneFoodDraft(draft)||draft.source!=='IMAGE'||draft.edit||action.expectedRevision!==draft.revision||action.rows.length<1||action.rows.length>50||new Set(action.rows.flatMap(r=>[r.rowKey,r.clientEntryId])).size!==action.rows.length*2||action.rows.some(r=>r.referenceId!==null||r.selectedReference!==null||r.mealType!==''||r.suggestion?.origin!=='IMAGE'))return state
    return {...state,draft:{...draft,revision:draft.revision+1,entries:structuredClone(action.rows)},submission:{status:'idle'},recordMutation:{status:'idle'}}
  }
  if(action.type==='MARK_VOICE_INPUT_STALE') {
    if(draft.source!=='VOICE'||draft.edit||!!draft.voiceInputStale===action.value)return state
    return {...state,draft:{...draft,voiceInputStale:action.value,revision:draft.revision+1}}
  }
  if(action.type==='IMPORT_VOICE_CANDIDATES') {
    const pending=action.pending
    if(draft.source!=='VOICE'||draft.edit||action.expectedRevision!==draft.revision||pending.kind!==draft.kind)return state
    if(pending.kind==='food'&&draft.kind==='food'&&isStandaloneFoodDraft(draft)){
      const rows=pending.foods
      if(rows.length<1||rows.length>50||new Set(rows.flatMap(r=>[r.rowKey,r.clientEntryId])).size!==rows.length*2||rows.some(r=>r.referenceId!==null||r.selectedReference!==null||r.suggestion?.origin!=='VOICE'||!['','BREAKFAST','LUNCH','DINNER','SNACK'].includes(r.mealType)))return state
      return {...state,draft:{...draft,voiceInputStale:false,recordDate:pending.recordDate,entries:structuredClone(rows),revision:draft.revision+1},submission:{status:'idle'},recordMutation:{status:'idle'}}
    }
    if(pending.kind==='exercise'&&draft.kind==='exercise'&&draft.entryKind==='QUICK')return {...state,draft:{...draft,voiceInputStale:false,recordDate:pending.recordDate,period:{clientEntryId:draft.period.clientEntryId,periodKind:'ACTIVITY',selection:{kind:'OPTION',option:null},durationMinutesInput:pending.durationMinutesInput,equipmentDetails:{},movements:[]},suggestion:structuredClone(pending.suggestion),revision:draft.revision+1},submission:{status:'idle'},recordMutation:{status:'idle'}}
    return state
  }
  if(action.type==='USE_MANUAL_ENTRY') {
    if(draft.edit||action.expectedRevision!==draft.revision||!(draft.source==='VOICE'||draft.kind==='food'&&draft.source==='IMAGE')||(draft.kind==='food'?standaloneFoods(draft).some(row=>row.suggestion):!!draft.suggestion))return state
    return {...state,draft:draft.kind==='food'?{...draft,source:'MANUAL',composer:draft.composer??action.composer??null,voiceInputStale:false,revision:draft.revision+1}:{...draft,source:'MANUAL',voiceInputStale:false,revision:draft.revision+1},submission:{status:'idle'}}
  }
  if(action.type==='BEGIN_SUBMIT') {
    if(action.expectedRevision!==draft.revision || !matchesDraft(draft,action.payload)) return state
    if(action.reviewedEstimate&&(!estimateMatches(action.payload,action.reviewedEstimate)||!('calculationFingerprint' in action.payload)||action.payload.calculationFingerprint!==action.reviewedEstimate.calculationFingerprint))return state
    return {...state,submission:{status:'saving',submissionId:action.payload.submissionId,ownerEpoch:action.ownerEpoch,frozenPayload:freezePayload(action.payload),...(action.reviewedEstimate?{reviewedEstimate:freezeEstimate(action.reviewedEstimate)}:{})}}
  }
  let next:Draft=draft
  if(action.type==='CHANGE_DATE') next={...draft,recordDate:action.value}
  else if(draft.kind==='exercise') {
    if(action.type==='SET_EXERCISE_TYPE'&&draft.entryKind==='QUICK') next={...draft,period:{...draft.period,selection:{kind:'REFERENCE',referenceId:action.value},equipmentDetails:{},movements:[]}}
    if(action.type==='SET_DURATION'&&draft.entryKind==='QUICK') next={...draft,period:{...draft.period,durationMinutesInput:action.value}}
  } else {
    if(action.type==='ADD_FOOD') {
      if(draft.entries.length>=20 || foodLeafCount(draft.entries)>=50 || draft.edit || foodIdentities(draft.entries).some(id=>id===action.row.rowKey||id===action.row.clientEntryId))return state
      next={...draft,entries:[...draft.entries,structuredClone(action.row)]}
    }else if(action.type==='REMOVE_FOOD'){const index=draft.entries.findIndex(x=>x.rowKey===action.rowKey);if(index<0)return state;return {...state,draft:{...draft,entries:draft.entries.filter(x=>x.rowKey!==action.rowKey),revision:draft.revision+1},undoRemoval:{kind:'FOOD_ROOT',index,root:structuredClone(draft.entries[index])},submission:{status:'idle'}}}
    else if('rowKey' in action){
      const update=(row:DraftFood|DraftIngredient)=>{if(row.rowKey!==action.rowKey)return row;switch(action.type){case 'SELECT_REFERENCE':return {...row,referenceId:action.reference.referenceId,selectedReference:structuredClone(action.reference),selectionKind:action.metadata?.selectionKind??'USER_SELECTED',searchMetadataRevision:action.metadata?.metadataRevision??undefined,selectionShortName:action.metadata?.shortName};case 'CLEAR_REFERENCE':return {...row,referenceId:null,selectedReference:null,selectionKind:undefined,searchMetadataRevision:undefined,selectionShortName:undefined};case 'CHANGE_GRAMS':return action.onlyIfEmpty&&row.quantityGramsInput.trim()!==''?row:{...row,quantityGramsInput:action.value};default:return row}}
      let dirty=false;const entries=draft.entries.map(root=>{if(action.type==='CHANGE_MEAL'&&root.rowKey===action.rowKey){dirty=true;return {...root,mealType:action.value}}if(root.entryKind==='SINGLE'){const row=update(root);if(row!==root)dirty=true;return row as DraftFood}const ingredients=root.ingredients.map(row=>{const result=update(row);if(result!==row)dirty=true;return result});return ingredients.some((row,i)=>row!==root.ingredients[i])?{...root,ingredients}:root});if(dirty)next={...draft,entries}
    }
  }
  return next===draft?state:{...state,draft:{...next,revision:draft.revision+1},submission:{status:'idle'}}
}
