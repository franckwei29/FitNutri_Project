import {z} from 'zod'
import type {FoodRecord,ExerciseRecord,FoodUpdateRequest,ExerciseUpdateRequest} from './contracts'
import {FoodRecordSchema,ExerciseRecordSchema,FoodUpdateRequestSchema,ExerciseUpdateRequestSchema,IdSchema} from './schemas'
import {apiDelete,apiPut} from './client'
import {decodeResponse} from './validators'
import {ApiRequestError} from './errors'
function invalid(){return new ApiRequestError({message:'Check the record and reviewed version before saving.',code:'VALIDATION_ERROR',status:400,outcome:'rejected',writeAttempted:false})}
function unknown(){return new ApiRequestError({message:'The change response could not be verified. Read the current record before repeating a change.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
export async function updateFoodRecord(id:string,request:FoodUpdateRequest):Promise<FoodRecord>{
  let body:FoodUpdateRequest
  try{decodeResponse(IdSchema,id);body=decodeResponse(FoodUpdateRequestSchema,request)}catch{throw invalid()}
  const raw=await apiPut(`/records/food/${encodeURIComponent(id)}`,body)
  try{const row=decodeResponse(FoodRecordSchema,raw);if(row.id!==id||row.version<body.expectedVersion||row.version>body.expectedVersion+1||row.reference?.referenceId!==body.referenceId||row.quantityGrams!==body.quantityGrams||row.mealType!==body.mealType||row.recordDate!==body.recordDate)throw unknown();return row}catch{throw unknown()}
}
export async function updateExerciseRecord(id:string,request:ExerciseUpdateRequest):Promise<ExerciseRecord>{
  let body:ExerciseUpdateRequest
  try{decodeResponse(IdSchema,id);body=decodeResponse(ExerciseUpdateRequestSchema,request)}catch{throw invalid()}
  const raw=await apiPut(`/records/exercise/${encodeURIComponent(id)}`,body)
  try{const row=decodeResponse(ExerciseRecordSchema,raw);if(row.id!==id||row.version<body.expectedVersion||row.version>body.expectedVersion+1||row.exerciseType?.id!==body.exerciseTypeId||row.durationMinutes!==body.durationMinutes||row.recordDate!==body.recordDate)throw unknown();return row}catch{throw unknown()}
}
export async function deleteRecord(kind:'food'|'exercise',id:string,expectedVersion:number):Promise<void>{
  try{decodeResponse(IdSchema,id);decodeResponse(z.number().int().nonnegative(),expectedVersion)}catch{throw invalid()}
  const result=await apiDelete(`/records/${kind}/${encodeURIComponent(id)}`,{params:{expectedVersion}})
  if(result!==undefined)throw unknown()
}
