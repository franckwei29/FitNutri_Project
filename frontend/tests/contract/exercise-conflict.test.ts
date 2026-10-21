import {AxiosError,AxiosHeaders} from 'axios'
import {describe,expect,it} from 'vitest'
import {normaliseApiError} from '../../src/api/errors'
import examples from '../../../docs/api/examples/write-requests.json'
const fixture=examples.find(x=>x.schema==='CalculationChangedResponse')?.value
function error(data:unknown,status=409){return new AxiosError('private transport text',AxiosError.ERR_BAD_REQUEST,undefined,undefined,{data,status,statusText:String(status),headers:new AxiosHeaders(),config:{headers:new AxiosHeaders()}})}
describe('B10 reviewed exercise calculation conflict boundary',()=>{
 it('decodes only a validated current preview and retains it through transport renormalization',()=>{
  const result=normaliseApiError(error(fixture),'POST',true)
  expect(result).toMatchObject({status:409,code:'CALCULATION_CHANGED',outcome:'rejected',currentPreview:{estimatedKj:170.847,weightKgUsed:70,estimateStatus:'AVAILABLE'}})
  expect(normaliseApiError(result,'POST',true).currentPreview).toEqual(result.currentPreview)
  expect(result).not.toHaveProperty('config');expect(result.message).not.toContain('private')
 })
 it('does not trust malformed preview or preview attached to an unrelated endpoint/status',()=>{
  if(!fixture || !('currentPreview' in fixture))throw new Error('Missing shared conflict fixture')
  for(const data of [{...fixture,currentPreview:{estimatedKj:999}},{...fixture,path:'/api/profile/weight'},{...fixture,status:500}]){
   const result=normaliseApiError(error(data,data.status),'POST',true);expect(result.currentPreview).toBeUndefined()
  }
 })
 it('recognizes the definitive entry identity conflict without unlocking an ambiguous server500',()=>{
  const base={status:409,code:'CLIENT_ENTRY_CONFLICT',message:'Review the original entry.',path:'/api/records/food/batch',requestId:'2fe11d5a-8677-4dfb-b090-61d68127c887',fieldErrors:[],retryable:false}
  expect(normaliseApiError(error(base),'POST',true).outcome).toBe('rejected')
  expect(normaliseApiError(error({...base,status:500},500),'POST',true).outcome).toBe('unknown')
 })
})
