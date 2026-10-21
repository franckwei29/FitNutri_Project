import type {SubmissionReceipt} from './contracts'
import type {CreatePayload} from '../features/entry/draftTypes'
import {ExerciseCreateRequestSchema,FoodBatchRequestSchema,SubmissionReceiptSchema} from './schemas'
import {apiGet,apiPost} from './client'
import {decodeResponse} from './validators'
import {ApiRequestError} from './errors'
export async function createRecords(payload:CreatePayload):Promise<SubmissionReceipt>{
  let body:CreatePayload
  try{body='items' in payload?decodeResponse(FoodBatchRequestSchema,payload):decodeResponse(ExerciseCreateRequestSchema,payload)}catch{throw new ApiRequestError({message:'Review the record details before saving.',code:'VALIDATION_ERROR',status:400,outcome:'rejected',writeAttempted:false})}
  const result=await apiPost('items' in body?'/records/food/batch':'/records/exercise',body)
  try{return decodeResponse(SubmissionReceiptSchema,result)}catch{throw new ApiRequestError({message:'The save response could not be verified. Check the original submission.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
}
export async function fetchSubmissionReceipt(submissionId:string,signal?:AbortSignal):Promise<SubmissionReceipt>{return decodeResponse(SubmissionReceiptSchema,await apiGet(`/submissions/${encodeURIComponent(submissionId)}`,{signal}))}
