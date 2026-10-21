import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import { describe, expect, it } from 'vitest'
import { createDraft, createFoodRow, draftReducer, emptyDraftState } from '../../src/features/entry/draftReducer'
import { validateDraft } from '../../src/features/entry/draftValidation'
import type { DraftState, FoodDraft } from '../../src/features/entry/draftTypes'
import type { FoodBatchRequest, ReferenceInfo, SubmissionReceipt } from '../../src/api/contracts'

const reference: ReferenceInfo={referenceId:'101',publicFoodKey:'synthetic',displayName:'Synthetic',active:true,dataset:{id:'1',name:'Synthetic fixture',release:'test',revision:'1',sourceUrl:null,nutrientPolicy:'synthetic',verified:true}}
function setup(): DraftState {
  const draft=createDraft('1','food','2026-10-16','MANUAL') as FoodDraft
  draft.entries[0]={...createFoodRow(),referenceId:'101',selectedReference:reference,quantityGramsInput:'150',mealType:'LUNCH'}
  draft.composer=null;return draftReducer(emptyDraftState('1'),{type:'START_NEW',ownerId:'1',draft})
}
function food(state: DraftState): FoodDraft { if(state.draft?.kind!=='food') throw new Error('Expected food test fixture');return state.draft }
function payload(state: DraftState): FoodBatchRequest {
  return {submissionId:'0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd',recordDate:'2026-10-16',zone:'Australia/Sydney',source:'MANUAL',items:foodRows(food(state)).map(x=>({clientEntryId:x.clientEntryId,referenceId:'101',quantityGrams:150,mealType:'LUNCH'}))}
}
function saving(state: DraftState) { return draftReducer(state,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:4,expectedRevision:food(state).revision,payload:payload(state)}) }

describe('F05 shared draft invariants',()=>{
  it('keeps row identity separate from stable entry identity and preserves other rows when removing',()=>{
    let state=setup();const first=foodRows(food(state))[0];const second=createFoodRow()
    expect(first.rowKey).not.toBe(first.clientEntryId)
    state=draftReducer(state,{type:'ADD_FOOD',ownerId:'1',row:second})
    state=draftReducer(state,{type:'REMOVE_FOOD',ownerId:'1',rowKey:first.rowKey})
    expect(foodRows(food(state))[0].clientEntryId).toBe(second.clientEntryId)
    expect(food(state).revision).toBe(2)
  })
  it('changing grams/reference invalidates confirmation without changing client identity',()=>{
    const state=setup(), first=foodRows(food(state))[0]
    const changed=draftReducer(state,{type:'CHANGE_GRAMS',ownerId:'1',rowKey:first.rowKey,value:'200'})
    expect(foodRows(food(changed))[0]).toMatchObject({quantityGramsInput:'200'});expect(foodRows(food(changed))[0]).not.toHaveProperty('gramsConfirmed')
    expect(foodRows(food(changed))[0].clientEntryId).toBe(first.clientEntryId)
    const replaced=draftReducer(state,{type:'SELECT_REFERENCE',ownerId:'1',rowKey:first.rowKey,reference:{...reference,referenceId:'102'}})
    expect(foodRows(food(replaced))[0]).toMatchObject({referenceId:'102'});expect(foodRows(food(replaced))[0]).not.toHaveProperty('referenceConfirmed')
  })
  it('rejects cross-owner commands and requires explicit replacement of an existing draft',()=>{
    const state=setup()
    expect(draftReducer(state,{type:'DISCARD',ownerId:'2'})).toBe(state)
    expect(draftReducer(state,{type:'START_NEW',ownerId:'1',draft:createDraft('1','exercise','2026-10-16','MANUAL')})).toBe(state)
    expect(draftReducer(state,{type:'START_NEW',ownerId:'1',draft:createDraft('1','exercise','2026-10-16','MANUAL'),replaceConfirmed:true}).draft?.kind).toBe('exercise')
  })
  it('freezes pending/unknown payloads and does not accept changed fields, discard or a new submission',()=>{
    const initial=setup(), state=saving(initial)
    expect(state.submission.status).toBe('saving')
    if(state.submission.status!=='saving'||state.submission.apiVersion===2) throw new Error('Expected saving')
    expect(Object.isFrozen(state.submission.frozenPayload)).toBe(true)
    const unknown=draftReducer(state,{type:'SUBMIT_UNKNOWN',ownerId:'1',ownerEpoch:4,submissionId:state.submission.submissionId})
    expect(unknown.submission.status).toBe('unknown')
    expect(draftReducer(unknown,{type:'CHANGE_DATE',ownerId:'1',value:'2026-10-15'})).toBe(unknown)
    expect(draftReducer(unknown,{type:'DISCARD',ownerId:'1'})).toBe(unknown)
    expect(saving(unknown)).toBe(unknown)
  })
  it('rejects stale revisions and mismatched payloads before beginning a write',()=>{
    const state=setup()
    expect(draftReducer(state,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:4,expectedRevision:99,payload:payload(state)})).toBe(state)
    expect(draftReducer(state,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:4,expectedRevision:0,payload:{...payload(state),recordDate:'2026-10-15'}})).toBe(state)
  })
  it('late owner epochs cannot clear a draft and only a matching receipt confirms it',()=>{
    const original=setup(), state=saving(original), p=payload(original)
    const receipt: SubmissionReceipt={submissionId:p.submissionId,kind:'FOOD_BATCH',committedAt:'2026-10-16T08:00:00Z',replayed:false,records:[{id:'501',clientEntryId:p.items[0].clientEntryId,recordDate:p.recordDate,versionAtCreation:0}]}
    expect(draftReducer(state,{type:'SUBMIT_CONFIRMED',ownerId:'1',ownerEpoch:3,receipt})).toBe(state)
    const confirmed=draftReducer(state,{type:'SUBMIT_CONFIRMED',ownerId:'1',ownerEpoch:4,receipt})
    expect(confirmed.draft).toBeNull();expect(confirmed.submission.status).toBe('saved')
    const bad=draftReducer(state,{type:'SUBMIT_CONFIRMED',ownerId:'1',ownerEpoch:4,receipt:{...receipt,records:[]}})
    expect(bad.submission.status).toBe('unknown');expect(bad.draft).not.toBeNull()
  })
  it('new deliberate recording gets fresh IDs and dates validate against application today',()=>{
    const a=createDraft('1','food','2026-10-16','MANUAL') as FoodDraft,b=createDraft('1','food','2026-10-16','MANUAL') as FoodDraft
    expect(a.composer!.clientEntryId).not.toBe(b.composer!.clientEntryId)
    const state=setup()
    expect(validateDraft(state.draft,'1','2026-10-16')).toEqual([])
    expect(validateDraft({...food(state),recordDate:'2026-10-17'},'1','2026-10-16').some(x=>x.field==='recordDate')).toBe(true)
  })
  it('requires AI candidate confirmation and rejects empty batches or bad minutes',()=>{
    const state=setup(), draft=food(state)
    expect(validateDraft({...draft,entries:[]},'1','2026-10-16').length).toBeGreaterThan(0)
    expect(validateDraft({...draft,source:'IMAGE',entries:foodRows(draft).map(x=>({...x,referenceId:null,selectedReference:null}))},'1','2026-10-16')[0].clientEntryId).toBe(foodRows(draft)[0].clientEntryId)
    const exercise=createDraft('1','exercise','2026-10-16','MANUAL')
    expect(validateDraft(exercise,'1','2026-10-16').length).toBeGreaterThan(0)
  })
  it('never treats a server500 as a definite rejection that unlocks an unknown submission',()=>{
    const state=saving(setup())
    const next=draftReducer(state,{type:'SUBMIT_REJECTED',ownerId:'1',ownerEpoch:4,submissionId:'0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd',error:{status:500,code:'INTERNAL_ERROR',message:'Unavailable',path:'/api/records/food/batch',requestId:'2fe11d5a-8677-4dfb-b090-61d68127c887',fieldErrors:[],retryable:false}})
    expect(next.submission.status).toBe('unknown')
    expect(draftReducer(next,{type:'DISCARD',ownerId:'1'})).toBe(next)
  })
  it('retains the saved fact when only the following GET refresh fails',()=>{
    const state=setup(), p=payload(state)
    const receipt: SubmissionReceipt={submissionId:p.submissionId,kind:'FOOD_BATCH',committedAt:'2026-10-16T08:00:00Z',replayed:false,records:[{id:'501',clientEntryId:p.items[0].clientEntryId,recordDate:p.recordDate,versionAtCreation:0}]}
    const saved=draftReducer(saving(state),{type:'SUBMIT_CONFIRMED',ownerId:'1',ownerEpoch:4,receipt})
    const stale=draftReducer(saved,{type:'REFRESH_FAILED',ownerId:'1'})
    expect(stale.submission).toEqual({status:'saved',receipt,refreshFailed:true})
    expect(stale.draft).toBeNull()
  })
  it('refuses an empty/duplicate identity create payload and preserves unknown source items',()=>{
    const state=setup(), p=payload(state)
    const empty={...food(state),entries:[]}
    const emptyState={...state,draft:empty}
    expect(draftReducer(emptyState,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:4,expectedRevision:0,payload:{...p,items:[]}})).toBe(emptyState)
    const row=foodRows(food(state))[0]
    expect(draftReducer(state,{type:'ADD_FOOD',ownerId:'1',row:{...row,rowKey:crypto.randomUUID()}})).toBe(state)
    expect(validateDraft({...food(state),edit:{recordId:'1',expectedVersion:0,originalRecordDate:'2026-10-16'},entries:[row,{...row,rowKey:'different',clientEntryId:'different'}]},'1','2026-10-16').some(x=>x.field==='entries')).toBe(true)
  })

})
