import type { CsrfToken } from './contracts'
import { CsrfTokenSchema } from './schemas'
import { http } from './http'
import { decodeResponse } from './validators'
import { ApiRequestError, normaliseApiError, signalAuthExpired } from './errors'

let cached: CsrfToken | null=null
let pending: Promise<CsrfToken> | null=null
let generation=0
export function clearCsrfToken():void {cached=null;pending=null;generation++}
export function ensureCsrfToken():Promise<CsrfToken> {
  if(cached) return Promise.resolve(cached)
  if(pending) return pending
  const started=generation
  const operation:Promise<CsrfToken>=http.get<unknown>('/auth/csrf').then(response=>{
    const token=decodeResponse(CsrfTokenSchema,response.data)
    if(started!==generation) throw new ApiRequestError({message:'The session changed. Try again.',code:'SESSION_CHANGED',canceled:true,outcome:'read-failed',writeAttempted:false})
    cached=token;return token
  }).catch((error:unknown)=>{const failure=normaliseApiError(error);if(failure.status===401&&failure.code==='UNAUTHENTICATED')signalAuthExpired();throw failure}).finally(()=>{if(pending===operation)pending=null})
  pending=operation;return operation
}
