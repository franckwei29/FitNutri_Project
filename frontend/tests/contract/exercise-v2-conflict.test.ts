import {describe,it,expect} from 'vitest'
import {AxiosError,AxiosHeaders} from 'axios'
import {normaliseApiError} from '../../src/api/errors'
const preview={periods:[{clientEntryId:'22222222-2222-4222-8222-222222222222',durationMinutes:20,status:'AVAILABLE',estimatedKj:488.133,calculationFingerprint:'a'.repeat(64),unavailableReason:null}],totalMinutes:20,knownEnergyKj:488.133,partial:false,fingerprint:'b'.repeat(64),inputSignature:'c'.repeat(64)}
const base={status:409,code:'CALCULATION_CHANGED',message:'Review current exercise estimate.',path:'/api/v2/exercise/entries',requestId:'11111111-1111-4111-8111-111111111111',fieldErrors:[],retryable:false}
function failure(value:unknown,path=base.path){return new AxiosError<unknown>('private transport',AxiosError.ERR_BAD_REQUEST,undefined,undefined,{data:{...base,path,currentPreview:value},status:409,statusText:'409',headers:new AxiosHeaders(),config:{headers:new AxiosHeaders()}})}
describe('UX-B12 separate V2 exercise authority',()=>{
 it('retains a strict V2 estimate through error normalization without treating it as V1 or food',()=>{const error=normaliseApiError(failure(preview),'POST',true);expect(error.currentExercisePreviewV2).toEqual(preview);expect(error.currentPreview).toBeUndefined();expect(error.currentFoodPreview).toBeUndefined();expect(error.outcome).toBe('rejected');expect(normaliseApiError(error,'POST',true).currentExercisePreviewV2).toEqual(preview)})
 it('never adopts malformed or other-path authority',()=>{for(const [value,path] of [[{...preview,periods:[]},base.path],[{...preview,debugMet:5},base.path],[preview,'/api/records/exercise'],[preview,'/api/v2/food/batches']] as const)expect(normaliseApiError(failure(value,path),'POST',true).currentExercisePreviewV2).toBeUndefined();expect(normaliseApiError(failure(preview,'/api/v2/exercise/entries/workout/1'),'PUT',true).currentExercisePreviewV2).toEqual(preview)})
})
