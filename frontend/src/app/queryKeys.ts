import {DEFAULT_ZONE} from '../domain/dates'
import type {MealType} from '../api/contracts'
/** Every private cache key begins with its authenticated owner. */
export const queryKeys={profile:(ownerId:string)=>['owner',ownerId,'profile'] as const,weights:(ownerId:string,from:string,to:string)=>['owner',ownerId,'weights',from,to] as const,
  foodSearch:(ownerId:string,epoch:number,catalogueRevision:string,query:string,page:number)=>['owner',ownerId,epoch,'food-search',catalogueRevision,query,page] as const,
  foodRecords:(ownerId:string,epoch:number,from:string,to:string,meal?:MealType)=>['owner',ownerId,epoch,'food-records',from,to,DEFAULT_ZONE,meal??null] as const,
  exerciseRecords:(ownerId:string,epoch:number,from:string,to:string,typeId?:string)=>['owner',ownerId,epoch,'exercise-records',from,to,DEFAULT_ZONE,typeId??null] as const,
  daySummary:(ownerId:string,epoch:number,date:string,profileVersion:number|null)=>['owner',ownerId,epoch,'summary','daily',date,DEFAULT_ZONE,profileVersion] as const,
  periodSummary:(ownerId:string,epoch:number,from:string,to:string,profileVersion:number|null)=>['owner',ownerId,epoch,'summary','period',from,to,DEFAULT_ZONE,profileVersion] as const,
  weeklySummary:(ownerId:string,epoch:number,end:string,profileVersion:number|null)=>['owner',ownerId,epoch,'summary','weekly',end,DEFAULT_ZONE,profileVersion] as const,
  record:(ownerId:string,epoch:number,kind:'food'|'exercise'|null,id:string|null)=>['owner',ownerId,epoch,'record',kind,id] as const,
}
