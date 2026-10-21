import type {FoodPreview} from './contracts'
import {FoodPreviewSchema} from './schemas'
import {apiGet} from './client'
import {decodeResponse,UnexpectedApiResponse} from './validators'
export async function previewFoodPortion(referenceId:string,grams:number,signal?:AbortSignal,recordId?:string):Promise<FoodPreview>{
  const result=decodeResponse(FoodPreviewSchema,await apiGet(`/food-reference/by-id/${encodeURIComponent(referenceId)}/nutrition`,{params:{grams,...(recordId?{recordId}:{})},signal}))
  if(result.reference.referenceId!==referenceId||result.quantityGrams!==grams||Object.values(result.nutrients).some(v=>v===null))throw new UnexpectedApiResponse()
  return result
}
