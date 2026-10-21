import {useQuery} from '@tanstack/react-query'
import {previewDatedExercise} from '../api/exerciseTypes'
import {DEFAULT_ZONE,isValidLocalDate,todayInZone} from '../domain/dates'
import {parseQuantity} from '../domain/inputRules'
import type {CatalogueScope} from './useFoodSearch'
export function useExerciseEstimate(typeId:string|null,input:string,date:string,scope:CatalogueScope,profileVersion:number|null,recordId?:string){
  const minutes=parseQuantity(input,'minutes'),valid=typeId!==null&&minutes.ok&&isValidLocalDate(date)&&date<=todayInZone()
  const result=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'exercise-estimate',scope.catalogueRevision,profileVersion,typeId,minutes.ok?minutes.value:input,date,DEFAULT_ZONE,recordId??null],queryFn:({signal})=>previewDatedExercise(typeId!,minutes.ok?minutes.value:NaN,date,signal,recordId),enabled:valid,retry:false})
  return {...result,data:valid?result.data:undefined,ready:valid&&result.isSuccess&&!result.isFetching}
}
