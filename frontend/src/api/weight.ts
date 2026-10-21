import { z } from 'zod'
import type { WeightRequest,WeightResult,WeightEntry } from './contracts'
import { WeightRequestSchema,WeightResultSchema,WeightEntrySchema } from './schemas'
import { apiGet,apiPost } from './client'
import { decodeResponse,UnexpectedApiResponse } from './validators'
import { ApiRequestError } from './errors'
import { DEFAULT_ZONE,isValidRecordRange } from '../domain/dates'
export async function fetchWeights(from:string,to:string,signal?:AbortSignal):Promise<WeightEntry[]> {if(!isValidRecordRange(from,to))throw new Error('Choose a date range of 1 to 366 days.');const rows=decodeResponse(z.array(WeightEntrySchema),await apiGet('/profile/weight',{params:{from,to,zone:DEFAULT_ZONE},signal}));if(rows.some(w=>w.recordDate<from||w.recordDate>to)||new Set(rows.map(w=>w.recordDate)).size!==rows.length||new Set(rows.map(w=>w.id)).size!==rows.length)throw new UnexpectedApiResponse();return rows}
export async function upsertWeight(payload:WeightRequest):Promise<WeightResult> {
  let body:WeightRequest
  try {body=decodeResponse(WeightRequestSchema,payload)} catch {throw new ApiRequestError({message:'Check the weight and measurement date.',code:'VALIDATION_ERROR',outcome:'rejected',writeAttempted:false})}
  const raw=await apiPost('/profile/weight',body)
  try {return decodeResponse(WeightResultSchema,raw)} catch {throw new ApiRequestError({message:'The weight save response could not be verified. Check the saved measurement.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
}
