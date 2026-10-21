import {legacyExerciseDraft} from '../fixtures/exercise'
import {describe,expect,it} from 'vitest'
import {draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import type {ExerciseCreateRequest} from '../../src/api/contracts'
import {exerciseEstimate,exerciseType} from '../fixtures/exercise'
function fixture(){const draft=legacyExerciseDraft('1','12.5'),payload:ExerciseCreateRequest={submissionId:'27d4db90-dc34-4010-a2dc-eaf902c74181',clientEntryId:draft.clientEntryId,recordDate:draft.recordDate,zone:'Australia/Sydney',source:'MANUAL',exerciseTypeId:exerciseType.id,durationMinutes:12.5,calculationFingerprint:'a'.repeat(64)},state=draftReducer(emptyDraftState('1'),{type:'START_NEW',ownerId:'1',draft}),estimate=exerciseEstimate({durationMinutes:12.5});return {draft,payload,state,estimate}}
describe('F10 reviewed basis is metadata, not writable record data',()=>{
 it('requires matching physical inputs/fingerprint, copies/freezes reviewed basis and keeps the wire payload unchanged',()=>{
  const {state,payload,estimate}=fixture(),action={type:'BEGIN_SUBMIT' as const,ownerId:'1',ownerEpoch:4,expectedRevision:0,payload}
  expect(draftReducer(state,{...action,reviewedEstimate:{...estimate,calculationFingerprint:'wrong'}})).toBe(state)
  const saving=draftReducer(state,{...action,reviewedEstimate:estimate});expect(saving.submission.status).toBe('saving');if(saving.submission.status!=='saving'||saving.submission.apiVersion===2)throw Error('Fixture');estimate.weightKgUsed=99
  expect(saving.submission.reviewedEstimate?.weightKgUsed).toBe(71.5);expect(Object.isFrozen(saving.submission.reviewedEstimate?.exerciseType.dataset)).toBe(true);expect(saving.submission.frozenPayload).toEqual(payload);expect(saving.submission.frozenPayload).not.toHaveProperty('reviewedEstimate')
  const unknown=draftReducer(saving,{type:'SUBMIT_UNKNOWN',ownerId:'1',ownerEpoch:4,submissionId:payload.submissionId});if(unknown.submission.status!=='unknown'||unknown.submission.apiVersion===2)throw Error('Fixture');expect(unknown.submission.reviewedEstimate).toBe(saving.submission.reviewedEstimate)
 })
 it.each([{date:'2026-10-15',id:exerciseType.id,minutes:12.5,bound:false},{date:'2026-10-16',id:'11',minutes:12.5,bound:false},{date:'2026-10-16',id:exerciseType.id,minutes:10,bound:false},{date:'2026-10-16',id:exerciseType.id,minutes:12.5,bound:true}])('keeps only a changed preview for this frozen create intent: $date/$id/$minutes',({date,id,minutes,bound})=>{
  const {state,payload,estimate}=fixture();let saving=draftReducer(state,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:4,expectedRevision:0,payload,reviewedEstimate:estimate})
  const preview={...estimate,exerciseType:{...exerciseType,id},recordDate:date,durationMinutes:minutes,calculationFingerprint:'b'.repeat(64)}
  saving=draftReducer(saving,{type:'SUBMIT_REJECTED',ownerId:'1',ownerEpoch:4,submissionId:payload.submissionId,error:{status:409,code:'CALCULATION_CHANGED',message:'Changed',path:'/api/records/exercise',requestId:'synthetic',fieldErrors:[],retryable:false},changedPreview:preview});if(saving.submission.status!=='rejected')throw Error('Fixture');expect(!!saving.submission.changedPreview).toBe(bound)
 })
})
