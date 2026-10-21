import type {FoodSearch} from './contracts'
import {FoodSearchSchema,FoodReferenceSchema,IdSchema,SmartFoodSearchV2Schema} from './schemas'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {apiGet} from './client'
/** The backend owns matching, order, pagination and active/verified eligibility. */
export async function searchActiveFood(query:string,page=0,signal?:AbortSignal):Promise<FoodSearch>{
  if(!Number.isSafeInteger(page)||page<0||query.trim().length>200)throw new Error('Check the search and page.')
  return decodeResponse(FoodSearchSchema,await apiGet('/food-reference/search',{params:{query:query.trim(),page,size:20},signal}))
}

export interface SmartFoodQueryOptions{mode:'SUGGESTED'|'ALL'|'APPROXIMATE';page:number;size:number;facets:Record<string,string>}
const facets:Record<string,readonly string[]>={state:['RAW','COOKED'],type:['WHITE','BROWN'],part:['BREAST','THIGH'],cookingMethod:['FRIED','BOILED'],addedFat:['YES','NO'],dairyFat:['REGULAR','REDUCED','SKIM']}
export function validSmartQuery(query:string){const text=query.trim();return text.length<=80&&(text.length>=2||/\p{Script=Han}/u.test(text))}
export async function searchSmartFood(query:string,options:SmartFoodQueryOptions,signal?:AbortSignal){if(!validSmartQuery(query)||!Number.isSafeInteger(options.page)||options.page<0||!Number.isSafeInteger(options.size)||options.size<1||options.size>100||!['SUGGESTED','ALL','APPROXIMATE'].includes(options.mode)||Object.entries(options.facets).some(([key,value])=>!facets[key]?.includes(value)))throw new Error('Choose a valid food query and supported conditions.');const selected=Object.fromEntries(Object.entries(options.facets).sort(([a],[b])=>a.localeCompare(b)));const result=decodeResponse(SmartFoodSearchV2Schema,await apiGet('/food-reference/smart-search',{params:{query:query.trim(),page:options.page,size:options.size,mode:options.mode,facets:JSON.stringify(selected)},signal}));if(result.page!==options.page||result.size!==options.size||result.results.length>options.size||result.results.some(row=>!row.reference.active||!row.reference.dataset.verified||Object.values(row.reference.per100g).some(value=>value===null))||result.nextQuestion&&(!facets[result.nextQuestion.key]||result.nextQuestion.values.some(value=>!facets[result.nextQuestion!.key].includes(value))))throw new UnexpectedApiResponse();return result}
export async function getFoodReference(id:string,signal?:AbortSignal){decodeResponse(IdSchema,id);const result=decodeResponse(FoodReferenceSchema,await apiGet(`/food-reference/by-id/${encodeURIComponent(id)}`,{signal}));if(result.referenceId!==id)throw new UnexpectedApiResponse();return result}
