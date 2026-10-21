import {describe,it,expect} from 'vitest'
import {AxiosError,AxiosHeaders} from 'axios'
import {normaliseApiError} from '../../src/api/errors'
import type {FoodPreviewV2} from '../../src/api/contracts'
const client='22222222-2222-4222-8222-222222222222'
const nutrients={energyKj:500,proteinGrams:1,fatGrams:0,carbsGrams:1}
const preview:FoodPreviewV2={entries:[{kind:'SINGLE',clientEntryId:client,leaves:[{clientEntryId:client,referenceId:'101',inputGrams:100,consumedGrams:100,nutrients}],totals:nutrients}],totals:nutrients,fingerprint:'a'.repeat(64),calculationPolicy:'FOOD_ENTRY_V2_REFERENCE_RATIONAL_V1',inputSignature:'b'.repeat(64)}
const base={status:409,code:'CALCULATION_CHANGED',message:'Review the current food preview.',path:'/api/v2/food/batches',requestId:'11111111-1111-4111-8111-111111111111',fieldErrors:[],retryable:false}
function error(currentPreview:unknown,path=base.path){return new AxiosError<unknown>('private transport payload',AxiosError.ERR_BAD_REQUEST,undefined,undefined,{data:{...base,path,currentPreview},status:409,statusText:'409',headers:new AxiosHeaders(),config:{headers:new AxiosHeaders()}})}
describe('UX-B06 authoritative V2 food conflict boundary',()=>{
 it('retains validated current food preview through normalization and treats rejection as known',()=>{const result=normaliseApiError(error(preview),'POST',true);expect(result.currentFoodPreview).toEqual(preview);expect(result.currentPreview).toBeUndefined();expect(result.outcome).toBe('rejected');expect(normaliseApiError(result,'POST',true).currentFoodPreview).toEqual(preview);expect(JSON.stringify(result)).not.toContain('private transport')})
 it('never adopts malformed authority or a preview from another endpoint',()=>{for(const [value,path] of [[{...preview,entries:[]},base.path],[{...preview,totals:{energyKj:99}},base.path],[{...preview,privateDebug:'unexpected'},base.path],[preview,'/api/other']] as const)expect(normaliseApiError(error(value,path),'POST',true).currentFoodPreview).toBeUndefined();expect(normaliseApiError(error(preview,'/api/v2/food/entries/dish/1'),'PUT',true).currentFoodPreview).toEqual(preview)})
 it('treats a parent-edit requirement as a rejected child write',()=>{const failure=error(preview);if(failure.response)failure.response.data={...base,code:'GROUP_EDIT_REQUIRED',path:'/api/records/food/1'};const result=normaliseApiError(failure,'PUT',true);expect(result.outcome).toBe('rejected');expect(result.code).toBe('GROUP_EDIT_REQUIRED');expect(result.currentFoodPreview).toBeUndefined()})
})
