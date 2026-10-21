import {z} from 'zod'
import type {ExerciseEstimate,ExerciseType} from './contracts'
import {ExerciseEstimateSchema,ExerciseTypeSchema} from './schemas'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {apiGet} from './client'
import {DEFAULT_ZONE} from '../domain/dates'
export async function loadExerciseTypes(signal?:AbortSignal):Promise<ExerciseType[]>{return decodeResponse(z.array(ExerciseTypeSchema),await apiGet('/records/exercise/types',{signal}))}
export async function previewDatedExercise(exerciseTypeId:string,durationMinutes:number,recordDate:string,signal?:AbortSignal,recordId?:string):Promise<ExerciseEstimate>{
  const result=decodeResponse(ExerciseEstimateSchema,await apiGet('/records/exercise/estimate',{params:{exerciseTypeId,durationMinutes,recordDate,zone:DEFAULT_ZONE,...(recordId?{recordId}:{})},signal}))
  const basis=[result.estimatedKj,result.weightKgUsed,result.weightDateUsed,result.metUsed]
  const validStatus=result.estimateStatus==='AVAILABLE'?basis.every(x=>x!==null)&&result.unavailableReason===null&&result.weightDateUsed!==null&&result.weightDateUsed<=recordDate:['MISSING_WEIGHT','NO_APPLICABLE_REFERENCE'].includes(result.estimateStatus)&&basis.every(x=>x===null)&&!!result.unavailableReason
  if(result.exerciseType.id!==exerciseTypeId||result.durationMinutes!==durationMinutes||result.recordDate!==recordDate||!validStatus)throw new UnexpectedApiResponse()
  return result
}
