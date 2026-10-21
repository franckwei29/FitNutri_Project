import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import {describe,expect,it} from 'vitest'
import {draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import {loadEditDraft,prepareCorrection} from '../../src/features/entry/correctionIntent'
import type {RecordMutationIntent} from '../../src/features/entry/recordMutationTypes'
import {savedFood,savedExercise} from '../fixtures/records'
import {exerciseEstimate} from '../fixtures/exercise'
const mutationId='27d4db90-dc34-4010-a2dc-eaf902c74181'
function fixture(){const draft=loadEditDraft('1','food',savedFood());if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].mealType='DINNER';const state=draftReducer(emptyDraftState('1'),{type:'LOAD_EDIT',ownerId:'1',draft}),intent:RecordMutationIntent={operation:'UPDATE',...prepareCorrection(draft,'1')};return {state,intent}}
function saving(){const {state,intent}=fixture();return {intent,state:draftReducer(state,{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,expectedRevision:0,intent})}}
describe('F13 shared record mutation state',()=>{
 it('freezes an exact update intent synchronously and blocks edits, discard and duplicate dispatch',()=>{
  const {state,intent}=saving();expect(state.recordMutation.status).toBe('saving');if(state.recordMutation.status!=='saving')throw Error('Fixture')
  expect(Object.isFrozen(state.recordMutation.intent)).toBe(true);expect(Object.isFrozen(state.recordMutation.intent.original)).toBe(true);expect(state.recordMutation.intent).not.toBe(intent)
  expect(draftReducer(state,{type:'CHANGE_DATE',ownerId:'1',value:'2026-10-15'})).toBe(state);expect(draftReducer(state,{type:'DISCARD',ownerId:'1'})).toBe(state);expect(draftReducer(state,{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,expectedRevision:0,intent})).toBe(state)
 })
 it('keeps unknown payload and original version, ignoring stale epochs and mismatched acknowledgements',()=>{
  const {state}=saving();const unknown=draftReducer(state,{type:'RECORD_MUTATION_UNKNOWN',ownerId:'1',ownerEpoch:3,mutationId});expect(unknown.recordMutation.status).toBe('unknown')
  expect(draftReducer(unknown,{type:'RECORD_MUTATION_CONFIRMED',ownerId:'1',ownerEpoch:2,mutationId,result:'UPDATED',record:savedFood({version:3,mealType:'DINNER'})})).toBe(unknown)
  const bad=draftReducer(unknown,{type:'RECORD_MUTATION_CONFIRMED',ownerId:'1',ownerEpoch:3,mutationId,result:'UPDATED',record:savedFood({id:'2',version:3,mealType:'DINNER'})});expect(bad.recordMutation.status).toBe('unknown');expect(bad.draft).not.toBeNull()
 })
 it('clears only after a matching update, preserving immutable creation receipts independently',()=>{
  const {state}=saving(),record=savedFood({version:3,mealType:'DINNER'});const saved=draftReducer(state,{type:'RECORD_MUTATION_CONFIRMED',ownerId:'1',ownerEpoch:3,mutationId,result:'UPDATED',record});expect(saved.draft).toBeNull();expect(saved.recordMutation).toMatchObject({status:'saved',result:'UPDATED',record});expect(saved.submission).toEqual(state.submission)
 })
 it('records matching later state as MATCHES rather than proof that this request wrote it',()=>{
  const {state}=saving(),record=savedFood({version:5,mealType:'DINNER'});const result=draftReducer(state,{type:'RECORD_MUTATION_CONFIRMED',ownerId:'1',ownerEpoch:3,mutationId,result:'MATCHES',record});expect(result.recordMutation).toMatchObject({status:'saved',result:'MATCHES'});expect(result.draft).toBeNull()
 })
 it('retains input on known conflict and refuses an unproven unknown reset',()=>{
  const {state}=saving();const error={status:409,code:'VERSION_CONFLICT',message:'Review latest version',path:'/api/records/food/1',requestId:'synthetic',fieldErrors:[],retryable:false};const rejected=draftReducer(state,{type:'RECORD_MUTATION_REJECTED',ownerId:'1',ownerEpoch:3,mutationId,error});expect(rejected.draft).toEqual(state.draft);expect(rejected.recordMutation.status).toBe('rejected')
  const unknown=draftReducer(state,{type:'RECORD_MUTATION_UNKNOWN',ownerId:'1',ownerEpoch:3,mutationId});expect(draftReducer(unknown,{type:'RESET_RECORD_MUTATION',ownerId:'1'})).toBe(unknown)
 })
 it('requires original read context for DELETE and distinguishes invisibility from a proven DELETE response',()=>{
  const original=savedFood(),intent:RecordMutationIntent={operation:'DELETE',kind:'food',recordId:original.id,expectedVersion:2,original};const state=draftReducer(emptyDraftState('1'),{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,intent});expect(state.recordMutation.status).toBe('saving')
  const unavailable=draftReducer(state,{type:'RECORD_MUTATION_CONFIRMED',ownerId:'1',ownerEpoch:3,mutationId,result:'UNAVAILABLE'});expect(unavailable.recordMutation).toMatchObject({status:'saved',result:'UNAVAILABLE'});expect(unavailable.submission.status).toBe('idle')
  const {state:dirty}=fixture();expect(draftReducer(dirty,{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,intent})).toBe(dirty)
 })
 it('never promotes an unclassified 500 to a known rejection or clears it through another owner',()=>{
  const {state}=saving();const result=draftReducer(state,{type:'RECORD_MUTATION_REJECTED',ownerId:'1',ownerEpoch:3,mutationId,error:{status:500,code:'INTERNAL_ERROR',message:'Unknown',path:'/api/records/food/1',requestId:'synthetic',fieldErrors:[],retryable:true}});expect(result.recordMutation.status).toBe('unknown');expect(draftReducer(result,{type:'DISCARD',ownerId:'2'})).toBe(result)
 })
 it.each([{date:'2026-10-16',bound:true},{date:'2026-10-15',bound:false}])('retains only a changed exercise preview bound to the frozen inputs: $date',({date,bound})=>{
  const original=savedExercise(),draft=loadEditDraft('1','exercise',original);if(draft.kind!=='exercise'||draft.entryKind!=='QUICK')throw Error('Fixture');draft.period.durationMinutesInput='12.5'
  const estimate=exerciseEstimate({exerciseType:original.exerciseType!,durationMinutes:12.5}),intent:RecordMutationIntent={operation:'UPDATE',...prepareCorrection(draft,'1',estimate)}
  let state=draftReducer(emptyDraftState('1'),{type:'LOAD_EDIT',ownerId:'1',draft});state=draftReducer(state,{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,expectedRevision:0,intent})
  const preview={...estimate,recordDate:date,calculationFingerprint:'b'.repeat(64)};state=draftReducer(state,{type:'RECORD_MUTATION_REJECTED',ownerId:'1',ownerEpoch:3,mutationId,error:{status:409,code:'CALCULATION_CHANGED',message:'Changed basis',path:'/api/records/exercise/1',requestId:'synthetic',fieldErrors:[],retryable:false},changedPreview:preview})
  expect(state.recordMutation.status).toBe('rejected');if(state.recordMutation.status!=='rejected'||state.recordMutation.apiVersion===2)throw Error('Fixture');expect(!!state.recordMutation.changedPreview).toBe(bound)
 })
 it('permits valid manual corrected grams without a second confirmation flag',()=>{const draft=loadEditDraft('1','food',savedFood());if(draft.kind!=='food')throw new Error('Fixture');foodRows(draft)[0].quantityGramsInput='200';const state=draftReducer(emptyDraftState('1'),{type:'LOAD_EDIT',ownerId:'1',draft});const intent:RecordMutationIntent={operation:'UPDATE',...prepareCorrection(draft,'1')};expect(draftReducer(state,{type:'BEGIN_RECORD_MUTATION',ownerId:'1',ownerEpoch:3,mutationId,expectedRevision:0,intent}).recordMutation.status).toBe('saving')})

})
