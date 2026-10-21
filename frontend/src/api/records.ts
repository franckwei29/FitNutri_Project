import {z} from 'zod'
import type {FoodRecord,ExerciseRecord,MealType} from './contracts'
import {FoodRecordSchema,ExerciseRecordSchema} from './schemas'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {apiGet} from './client'
import {DEFAULT_ZONE,isValidRecordRange} from '../domain/dates'
function requireRange(from:string,to:string){if(!isValidRecordRange(from,to))throw new Error('Choose a date range of 1 to 366 days.')}
export async function fetchFoodRecords(from:string,to:string,signal?:AbortSignal,mealType?:MealType):Promise<FoodRecord[]>{requireRange(from,to);const rows=decodeResponse(z.array(FoodRecordSchema),await apiGet('/records/food',{params:{from,to,zone:DEFAULT_ZONE,...(mealType?{mealType}:{})},signal}));if(rows.some(r=>r.recordDate<from||r.recordDate>to))throw new UnexpectedApiResponse();return rows}
export async function fetchExerciseRecords(from:string,to:string,signal?:AbortSignal,exerciseTypeId?:string):Promise<ExerciseRecord[]>{requireRange(from,to);const rows=decodeResponse(z.array(ExerciseRecordSchema),await apiGet('/records/exercise',{params:{from,to,zone:DEFAULT_ZONE,...(exerciseTypeId?{exerciseTypeId}:{})},signal}));if(rows.some(r=>r.recordDate<from||r.recordDate>to))throw new UnexpectedApiResponse();return rows}
export async function fetchFoodRecord(id:string,signal?:AbortSignal):Promise<FoodRecord>{const row=decodeResponse(FoodRecordSchema,await apiGet(`/records/food/${encodeURIComponent(id)}`,{signal}));if(row.id!==id)throw new UnexpectedApiResponse();return row}
export async function fetchExerciseRecord(id:string,signal?:AbortSignal):Promise<ExerciseRecord>{const row=decodeResponse(ExerciseRecordSchema,await apiGet(`/records/exercise/${encodeURIComponent(id)}`,{signal}));if(row.id!==id)throw new UnexpectedApiResponse();return row}
