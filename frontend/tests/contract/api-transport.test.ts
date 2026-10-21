import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, CanceledError } from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { http } from '../../src/api/http'
import {createRecordsV2} from '../../src/api/submissionsV2'
import {foodBatchV2Fixture,receiptV2Fixture} from '../fixtures/entryV2'
import { apiGet, apiPost } from '../../src/api/client'
import { clearCsrfToken } from '../../src/api/csrf'
import { isKnownRejectedWrite, subscribeAuthExpired } from '../../src/api/errors'

const response=(config:InternalAxiosRequestConfig,data:unknown,status=200,type='application/json'):AxiosResponse=>({config,data,status,statusText:String(status),headers:AxiosHeaders.from({'content-type':type})})
const failure=(config:InternalAxiosRequestConfig,status:number,code:string)=>new AxiosError('private request payload',AxiosError.ERR_BAD_REQUEST,config,{},response(config,{status,code,message:'Request rejected.',path:config.url,requestId:'2fe11d5a-8677-4dfb-b090-61d68127c887',fieldErrors:[],retryable:false},status))

beforeEach(clearCsrfToken)
describe('F02 real API transport boundaries (controlled Axios adapter)',()=>{
  it('shares one anonymous token request and sends cookie-enabled same-origin writes',async()=>{
    let tokens=0,writes=0
    http.defaults.adapter=async config=>{
      if(config.url==='/auth/csrf'){tokens++;return response(config,{headerName:'X-CSRF-TOKEN',token:'framework-token'})}
      writes++;expect(config.headers.get('X-CSRF-TOKEN')).toBe('framework-token');expect(config.baseURL).toBe('/api');expect(config.withCredentials).toBe(true);return response(config,{saved:true})
    }
    await Promise.all([apiPost('/records/food/batch',{}),apiPost('/records/exercise',{})])
    expect(tokens).toBe(1);expect(writes).toBe(2)
  })
  it('refreshes only an explicit pre-handler CSRF rejection, once',async()=>{
    let tokens=0,writes=0
    http.defaults.adapter=async config=>{
      if(config.url==='/auth/csrf'){tokens++;return response(config,{headerName:'X-CSRF-TOKEN',token:'token-'+tokens})}
      writes++;if(writes===1)throw failure(config,403,'CSRF_INVALID');return response(config,{saved:true})
    }
    await apiPost('/records/food/batch',{})
    expect(tokens).toBe(2);expect(writes).toBe(2)
  })
  it.each(['timeout','server500'])('never automatically repeats a %s write; result remains unknown',async(kind)=>{
    let writes=0
    http.defaults.adapter=async config=>{
      if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'})
      writes++;if(kind==='server500')throw failure(config,500,'INTERNAL_ERROR');throw new AxiosError('password must not leak',AxiosError.ECONNABORTED,config)
    }
    await expect(apiPost('/records/food/batch',{})).rejects.toMatchObject({code:kind==='timeout'?'REQUEST_TIMEOUT':'INTERNAL_ERROR',outcome:'unknown',writeAttempted:true})
    expect(writes).toBe(1)
  })
  it('treats a failed token GET as no business write attempted',async()=>{
    let writes=0
    http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')throw new AxiosError('offline',AxiosError.ERR_NETWORK,config);writes++;return response(config,{saved:true})}
    try{await apiPost('/records/food/batch',{});throw new Error('Expected failure')}catch(error){expect(isKnownRejectedWrite(error)).toBe(true)}
    expect(writes).toBe(0)
  })
  it('distinguishes401 session expiry from403 permissions and does not loop',async()=>{
    const expired=vi.fn(),unsubscribe=subscribeAuthExpired(expired)
    try{
      http.defaults.adapter=async config=>{throw failure(config,401,'UNAUTHENTICATED')}
      await expect(apiGet('/auth/me')).rejects.toMatchObject({status:401,code:'UNAUTHENTICATED'})
      expect(expired).toHaveBeenCalledOnce()
      http.defaults.adapter=async config=>{throw failure(config,403,'FORBIDDEN')}
      await expect(apiGet('/admin/users')).rejects.toMatchObject({status:403,code:'FORBIDDEN'})
      expect(expired).toHaveBeenCalledOnce()
    }finally{unsubscribe()}
  })
  it('rejects HTML fallback and malformed error responses without treating them as saved',async()=>{
    http.defaults.adapter=async config=>response(config,'<html>proxy fallback</html>',200,'text/html')
    await expect(apiGet('/auth/me')).rejects.toMatchObject({code:'UNEXPECTED_RESPONSE'})
    http.defaults.adapter=async config=>{throw new AxiosError('private SQL',AxiosError.ERR_BAD_REQUEST,config,{},response(config,'<html>error</html>',500,'text/html'))}
    await expect(apiPost('/auth/login',{})).rejects.not.toHaveProperty('config')
  })
  it('passes cancellation signals and preserves multipart boundary selection',async()=>{
    const controller=new AbortController();let uploadType:unknown
    http.defaults.adapter=async config=>{
      if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'})
      if(config.url==='/food/recognize'){uploadType=config.headers.getContentType();expect(config.data).toBeInstanceOf(FormData);return response(config,{candidates:[]})}
      expect(config.signal).toBe(controller.signal);throw new CanceledError('canceled',config)
    }
    await expect(apiGet('/summary/today',{signal:controller.signal})).rejects.toMatchObject({canceled:true})
    const data=new FormData();data.set('image',new Blob(['synthetic']), 'synthetic.png');await apiPost('/food/recognize',data)
    expect(uploadType===false || uploadType===undefined).toBe(true)
  })
  it('does not retry a second CSRF rejection forever',async()=>{
    let writes=0
    http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});writes++;throw failure(config,403,'CSRF_INVALID')}
    await expect(apiPost('/records/food/batch',{})).rejects.toMatchObject({code:'CSRF_INVALID',outcome:'rejected'})
    expect(writes).toBe(2)
  })
  it('does not dispatch a write if canceled while preparing its token',async()=>{
    const controller=new AbortController();controller.abort();let writes=0
    http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});writes++;return response(config,{saved:true})}
    await expect(apiPost('/records/food/batch',{}, {signal:controller.signal})).rejects.toMatchObject({canceled:true,outcome:'rejected',writeAttempted:false})
    expect(writes).toBe(0)
  })
  it('notifies expiry for a rejected token bootstrap without attempting a business write',async()=>{
    const expired=vi.fn(),unsubscribe=subscribeAuthExpired(expired)
    try{
      http.defaults.adapter=async config=>{throw failure(config,401,'UNAUTHENTICATED')}
      await expect(apiPost('/records/food/batch',{})).rejects.toMatchObject({status:401,outcome:'rejected',writeAttempted:false})
      expect(expired).toHaveBeenCalledOnce()
    }finally{unsubscribe()}
  })
  it('recognises a trusted NOT_FOUND business rejection without inventing a retry',async()=>{
    let writes=0
    http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});writes++;throw failure(config,404,'NOT_FOUND')}
    await expect(apiPost('/records/food/batch',{})).rejects.toMatchObject({status:404,outcome:'rejected'})
    expect(writes).toBe(1)
  })

  it('applies the media deadline only to the explicit upload while retaining credentials and CSRF',async()=>{
    http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});expect(config.timeout).toBe(config.url==='/food/recognize'?70000:15000);expect(config.withCredentials).toBe(true);if(config.url==='/food/recognize'){expect(config.headers.get('X-CSRF-TOKEN')).toBe('token');expect(config.headers.getContentType()).toBe(false);expect(config.data).toBeInstanceOf(FormData)}return response(config,{ok:true})}
    await apiPost('/food/recognize',new FormData(),{timeoutMs:70000});await apiGet('/summary/today')
  })

  it.each([[409,'DATE_CONTEXT_CHANGED'],[422,'DATE_REVIEW_REQUIRED'],[422,'SPLIT_DATED_ENTRY'],[422,'SPLIT_EXERCISE_ENTRY'],[422,'PROVIDER_REFUSED'],[413,'PAYLOAD_TOO_LARGE']] as const)('treats canonical stateless-media rejection %s/%s as rejected without retry',async(status,code)=>{let calls=0;http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});calls++;throw failure(config,status,code)};await expect(apiPost('/speech/parse',{})).rejects.toMatchObject({status,code,outcome:'rejected'});expect(calls).toBe(1)})

  it('retains bounded canonical Retry-After metadata without an automatic provider retry',async()=>{let calls=0;http.defaults.adapter=async config=>{if(config.url==='/auth/csrf')return response(config,{headerName:'X-CSRF-TOKEN',token:'token'});calls++;const error=failure(config,429,'RATE_LIMITED');error.response!.headers['retry-after']='2';throw error};await expect(apiPost('/qa/messages',{})).rejects.toMatchObject({status:429,code:'RATE_LIMITED',retryAfterSeconds:2});expect(calls).toBe(1)})

  it('UX-F09 retries only one explicit CSRF rejection with the identical V2 submission body',async()=>{let tokens=0;const bodies:unknown[]=[];http.defaults.adapter=async config=>{if(config.url==='/auth/csrf'){tokens++;return response(config,{headerName:'X-CSRF-TOKEN',token:'token-'+tokens})}expect(config.url).toBe('/v2/food/batches');bodies.push(config.data);if(bodies.length===1)throw failure(config,403,'CSRF_INVALID');return response(config,receiptV2Fixture(),201)};const proof=await createRecordsV2({apiVersion:2,domain:'FOOD',payload:foodBatchV2Fixture()});expect(tokens).toBe(2);expect(bodies).toHaveLength(2);expect(bodies[0]).toBe(bodies[1]);expect(proof.submissionId).toBe(foodBatchV2Fixture().submissionId)})

  it('UX-F12 refreshes an explicit exercise CSRF rejection once with exactly the same Quick envelope',async()=>{const id='22222222-2222-4222-8222-222222222222',submissionId='33333333-3333-4333-8333-333333333333';let tokens=0;const bodies:unknown[]=[];http.defaults.adapter=async config=>{if(config.url==='/auth/csrf'){tokens++;return response(config,{headerName:'X-CSRF-TOKEN',token:'token-'+tokens})}expect(config.url).toBe('/v2/exercise/entries');bodies.push(config.data);if(bodies.length===1)throw failure(config,403,'CSRF_INVALID');return response(config,{schemaVersion:2,submissionId,kind:'EXERCISE_ENTRY_V2',committedAt:'2020-01-02T01:00:00Z',replayed:false,roots:[{kind:'QUICK',id:'9',clientEntryId:id,recordDate:'2020-01-02',versionAtCreation:0}],leaves:[{kind:'ACTIVITY_PERIOD',id:'9',clientEntryId:id,rootClientEntryId:id,recordDate:'2020-01-02',versionAtCreation:0}]},201)};const proof=await createRecordsV2({apiVersion:2,domain:'EXERCISE',payload:{submissionId,recordDate:'2020-01-02',zone:'Australia/Sydney',source:'MANUAL',previewFingerprint:'b'.repeat(64),entry:{kind:'QUICK',clientEntryId:id,period:{clientEntryId:id,periodKind:'ACTIVITY',optionId:'201',durationMinutes:20,equipmentDetails:{},movements:[],calculationFingerprint:'a'.repeat(64)}}}});expect(tokens).toBe(2);expect(bodies).toHaveLength(2);expect(bodies[0]).toBe(bodies[1]);expect(proof.roots[0].kind).toBe('QUICK')})

})
