import { AxiosHeaders } from 'axios'
import { http } from './http'
import { clearCsrfToken, ensureCsrfToken } from './csrf'
import { ApiRequestError, normaliseApiError, signalAuthExpired } from './errors'

export interface RequestOptions { signal?: AbortSignal; params?: Record<string, unknown>; timeoutMs?:number }
async function request(method: string,path: string,body: unknown,options: RequestOptions={}):Promise<unknown> {
  const write=!['GET','HEAD','OPTIONS'].includes(method)
  for(let attempt=0;attempt<2;attempt++) {
    const headers=new AxiosHeaders()
    if(write) {
      try {const token=await ensureCsrfToken();headers.set(token.headerName,token.token)}
      catch(error) {throw normaliseApiError(error,method,false)}
    }
    if(options.signal?.aborted) throw new ApiRequestError({message:'Request canceled before dispatch.',code:'REQUEST_CANCELED',canceled:true,outcome:write?'rejected':'read-failed',writeAttempted:false})
    // false tells Axios to leave Content-Type to the browser for real FormData boundaries.
    if(body instanceof FormData) headers.setContentType(false)
    try {
      const response=await http.request<unknown>({method,url:path,data:body,headers,params:options.params,signal:options.signal,...(options.timeoutMs?{timeout:options.timeoutMs}:{})})
      if(response.status===204) return undefined
      const type=String(response.headers['content-type']??'')
      if(!type.includes('application/json')&&!type.includes('+json')) throw new ApiRequestError({message:'Unexpected server response. Refresh and try again.',code:'UNEXPECTED_RESPONSE',status:response.status,outcome:write?'unknown':'read-failed',writeAttempted:write})
      return response.data
    } catch(error) {
      const failure=normaliseApiError(error,method,write)
      if(write && attempt===0 && failure.status===403 && failure.code==='CSRF_INVALID') {clearCsrfToken();continue}
      if(failure.status===401 && failure.code==='UNAUTHENTICATED') signalAuthExpired()
      throw failure
    }
  }
  throw new ApiRequestError({message:'The request could not be completed.',code:'UNEXPECTED_RESPONSE',outcome:write?'unknown':'read-failed',writeAttempted:write})
}
export function apiGet(path:string,options?:RequestOptions):Promise<unknown> {return request('GET',path,undefined,options)}
export function apiPost(path:string,body?:unknown,options?:RequestOptions):Promise<unknown> {return request('POST',path,body,options)}
export function apiPut(path:string,body:unknown,options?:RequestOptions):Promise<unknown> {return request('PUT',path,body,options)}
export function apiPatch(path:string,body:unknown,options?:RequestOptions):Promise<unknown> {return request('PATCH',path,body,options)}
export function apiDelete(path:string,options?:RequestOptions):Promise<unknown> {return request('DELETE',path,undefined,options)}
