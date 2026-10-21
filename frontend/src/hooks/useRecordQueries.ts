import {mergeRootPages} from '../features/history/mergeRootPages'
import {fetchFoodEntriesV2} from '../api/foodEntriesV2'
import {fetchExerciseEntriesV2} from '../api/exerciseEntriesV2'
import {useQuery,useInfiniteQuery} from '@tanstack/react-query'
import {fetchFoodRecords,fetchExerciseRecords} from '../api/records'
import {isValidRecordRange} from '../domain/dates'
import {queryKeys} from '../app/queryKeys'
import {useSession} from './useSession'
import type {MealType} from '../api/contracts'
export function useRecordQueries(from:string,to:string,options:{enabled?:boolean;mealType?:MealType;exerciseTypeId?:string;kind?:'food'|'exercise'|'all'}={}){
  const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',enabled=!!owner&&isValidRecordRange(from,to)&&options.enabled!==false
  const foods=useQuery({queryKey:queryKeys.foodRecords(owner,state.epoch,from,to,options.mealType),queryFn:({signal})=>fetchFoodRecords(from,to,signal,options.mealType),enabled:enabled&&options.kind!=='exercise',retry:false})
  const exercises=useQuery({queryKey:queryKeys.exerciseRecords(owner,state.epoch,from,to,options.exerciseTypeId),queryFn:({signal})=>fetchExerciseRecords(from,to,signal,options.exerciseTypeId),enabled:enabled&&options.kind!=='food',retry:false})
  return {foods,exercises}
}

/** Root pages are whole saved entries. Every query is owned and epoch/range scoped. */
export function useEntryRootQueries(from:string,to:string,options:{enabled?:boolean;kind?:'food'|'exercise'|'all'}={}){
 const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',enabled=!!owner&&isValidRecordRange(from,to)&&options.enabled!==false
 const foods=useInfiniteQuery({queryKey:['owner',owner,state.epoch,'entry-roots','food',from,to,'Australia/Sydney'],initialPageParam:0,queryFn:({pageParam,signal})=>fetchFoodEntriesV2(from,to,pageParam,signal),getNextPageParam:last=>last.hasMore?last.page+1:undefined,select:data=>({results:mergeRootPages(data.pages),totalMatches:data.pages[0].totalMatches}),enabled:enabled&&options.kind!=='exercise',retry:false})
 const exercises=useInfiniteQuery({queryKey:['owner',owner,state.epoch,'entry-roots','exercise',from,to,'Australia/Sydney'],initialPageParam:0,queryFn:({pageParam,signal})=>fetchExerciseEntriesV2(from,to,pageParam,signal),getNextPageParam:last=>last.hasMore?last.page+1:undefined,select:data=>({results:mergeRootPages(data.pages),totalMatches:data.pages[0].totalMatches}),enabled:enabled&&options.kind!=='food',retry:false})
 return {foods,exercises}
}
