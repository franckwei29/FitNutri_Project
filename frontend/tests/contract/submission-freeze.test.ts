import {createHash} from 'node:crypto'
import {describe,expect,it} from 'vitest'
import {FoodBatchRequestSchema} from '../../src/api/schemas'
import {draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import type {FoodDraft} from '../../src/features/entry/draftTypes'
import fixture from '../../../docs/api/examples/submission-canonicalization.json'
describe('B08 cross-language frozen create fixture',()=>{
 it('freezes exactly the contract inputs consumed by Java, with separate stable IDs and no derived nutrition',()=>{
  const request=FoodBatchRequestSchema.parse(fixture.food.request)
  const draft:FoodDraft={kind:'food',ownerId:'1',recordDate:request.recordDate,source:request.source,revision:0,returnTo:'/dashboard',defaultMealType:'',composer:null,selectionTarget:null,composerQuery:'',entries:request.items.map((item,index)=>({entryKind:'SINGLE' as const,rowKey:`local-row-${index}`,clientEntryId:item.clientEntryId,referenceId:item.referenceId,selectedReference:{referenceId:item.referenceId,publicFoodKey:'synthetic',displayName:'Synthetic fixture only',active:true,dataset:{id:'1',name:'Synthetic fixture',release:'test',revision:'one',sourceUrl:null,nutrientPolicy:'synthetic',verified:false}},quantityGramsInput:String(item.quantityGrams),mealType:item.mealType}))}
  let state=draftReducer(emptyDraftState('1'),{type:'START_NEW',ownerId:'1',draft})
  state=draftReducer(state,{type:'BEGIN_SUBMIT',ownerId:'1',ownerEpoch:7,expectedRevision:0,payload:request})
  expect(state.submission.status).toBe('saving');if(state.submission.status!=='saving'||state.submission.apiVersion===2)throw new Error('Expected frozen create')
  const frozen=state.submission.frozenPayload;expect(frozen).toEqual(fixture.food.request);expect(Object.isFrozen(frozen)).toBe(true)
  request.items[0].quantityGrams=999
  expect(frozen).toEqual(fixture.food.request)
  const unknown=draftReducer(state,{type:'SUBMIT_UNKNOWN',ownerId:'1',ownerEpoch:7,submissionId:request.submissionId})
  expect(unknown.submission.status).toBe('unknown');if(unknown.submission.status!=='unknown'||unknown.submission.apiVersion===2)throw new Error('Expected unknown')
  expect(unknown.submission.frozenPayload).toBe(frozen)
  expect(draftReducer(unknown,{type:'CHANGE_DATE',ownerId:'1',value:'2026-10-15'})).toBe(unknown)
 })
 it('checks the independent fixture digest without making the client hash authoritative',()=>{expect(createHash('sha256').update(fixture.food.canonicalJson,'utf8').digest('hex')).toBe(fixture.food.sha256)})
})
