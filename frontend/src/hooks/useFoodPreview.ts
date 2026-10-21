import {useQuery} from '@tanstack/react-query'
import {previewFoodPortion} from '../api/foodPreview'
import {parseQuantity} from '../domain/inputRules'
import type {CatalogueScope} from './useFoodSearch'
export function useFoodPreview(referenceId:string|null,input:string,scope:CatalogueScope,recordId?:string){
  const grams=parseQuantity(input,'grams'),valid=referenceId!==null&&grams.ok
  const result=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'food-preview',scope.catalogueRevision,referenceId,grams.ok?grams.value:input,recordId??null],queryFn:({signal})=>previewFoodPortion(referenceId!,grams.ok?grams.value:NaN,signal,recordId),enabled:valid,retry:false})
  return {...result,data:valid?result.data:undefined,ready:valid&&result.isSuccess&&!result.isFetching}
}
