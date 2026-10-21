import {verifyReceiptV2} from './receiptV2Validation'
export {verifyReceiptV2} from './receiptV2Validation'
import type {FoodBatchV2Request,ExerciseCreateV2Request,SubmissionReceiptV2} from './contracts'
import {FoodBatchV2RequestSchema,ExerciseCreateV2RequestSchema} from './schemas'
import {decodeResponse} from './validators'
import {ApiRequestError} from './errors'
import {apiGet,apiPost} from './client'
export type EntrySubmissionV2={apiVersion:2;domain:'FOOD';payload:FoodBatchV2Request}|{apiVersion:2;domain:'EXERCISE';payload:ExerciseCreateV2Request}
export async function createRecordsV2(envelope:EntrySubmissionV2):Promise<SubmissionReceiptV2> {
 let body:FoodBatchV2Request|ExerciseCreateV2Request
 try{body=envelope.domain==='FOOD'?decodeResponse(FoodBatchV2RequestSchema,envelope.payload):decodeResponse(ExerciseCreateV2RequestSchema,envelope.payload)}catch{throw new ApiRequestError({message:'Review the entry details before saving.',code:'VALIDATION_ERROR',status:400,outcome:'rejected',writeAttempted:false})}
 const response=await apiPost(envelope.domain==='FOOD'?'/v2/food/batches':'/v2/exercise/entries',body)
 try{return verifyReceiptV2(envelope,response)}catch{throw new ApiRequestError({message:'The save response could not be verified. Check the original submission.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
}
export async function fetchSubmissionV2(envelope:EntrySubmissionV2,signal?:AbortSignal) {
 const response=await apiGet(`/v2/submissions/${encodeURIComponent(envelope.payload.submissionId)}`,{signal})
 return verifyReceiptV2(envelope,response)
}
