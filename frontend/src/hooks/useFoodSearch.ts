import {useEffect,useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {searchSmartFood,validSmartQuery} from '../api/foodReference'
import type {SmartFoodQueryOptions} from '../api/foodReference'
export interface CatalogueScope {ownerId:string;epoch:number;catalogueRevision:string}
export function useFoodSearch(query:string,page:number,scope:CatalogueScope,debounceMs=300,options:SmartFoodQueryOptions={mode:'SUGGESTED',page,size:5,facets:{}}){
  const text=query.trim(),[debounced,setDebounced]=useState('')
  useEffect(()=>{const timer=setTimeout(()=>setDebounced(text),debounceMs);return ()=>clearTimeout(timer)},[text,debounceMs])
  // New input immediately hides the old query, including during its debounce window.
  const ready=validSmartQuery(text)&&text===debounced
  const result=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'smart-food-search',scope.catalogueRevision,text,options.mode,Object.entries(options.facets).sort(([a],[b])=>a.localeCompare(b)),page,options.size],queryFn:({signal})=>searchSmartFood(text,{...options,page},signal),enabled:ready,retry:false})
  return {...result,data:ready?result.data:undefined,isPending:ready&&result.isPending,isDebouncing:validSmartQuery(text)&&!ready,canSearch:ready}
}
