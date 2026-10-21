import type {FoodRootV2,ExerciseRootV2,ExerciseEntryV2Request,ExercisePreviewV2} from './contracts'
import {validateExercisePreview} from './exercisePreviewValidation'
import {UnexpectedApiResponse} from './validators'
export function validateFoodRootRead(root:FoodRootV2):void{
 const ids=root.input.kind==='SINGLE'?[root.input.clientEntryId]:root.input.ingredients.map(i=>i.clientEntryId)
 if(root.componentCount!==ids.length||root.componentCount!==root.preview.leaves.length||root.input.clientEntryId!==root.preview.clientEntryId||root.input.kind!==root.preview.kind)throw new UnexpectedApiResponse()
 if(root.componentDetails&&(root.componentDetails.length!==ids.length||root.componentDetails.some((d,index)=>d.clientEntryId!==ids[index]||d.referenceId!==root.preview.leaves[index].referenceId)))throw new UnexpectedApiResponse()
 ids.forEach((id,index)=>{if(id!==root.preview.leaves[index].clientEntryId)throw new UnexpectedApiResponse()})
}
/** The current read model must agree with its saved input; historical unmapped values stay read-only. */
export function validateExerciseRootRead(root:ExerciseRootV2):void{
 const periods=root.input.kind==='WORKOUT'?root.input.periods:[root.input.period]
 if(root.periodCount!==periods.length||root.periodCount!==root.preview.periods.length||root.periodCount!==root.periodDetails.length)throw new UnexpectedApiResponse()
 for(const [index,period] of periods.entries()){
  const detail=root.periodDetails[index]
  if(period.clientEntryId!==root.preview.periods[index].clientEntryId||period.clientEntryId!==detail.clientEntryId)throw new UnexpectedApiResponse()
  if('optionId' in period){
   if(detail.optionId!==period.optionId||detail.option&&(detail.option.id!==period.optionId||detail.option.periodKind!==period.periodKind||detail.option.displayName!==detail.name)||detail.movements.length!==period.movements.length)throw new UnexpectedApiResponse()
   period.movements.forEach((movement,i)=>{const stored=detail.movements[i];if(stored.catalogueId!==movement.catalogueId||stored.setCount!==(movement.setCount??null)||stored.repetitions!==(movement.repetitions??null)||stored.loadKg!==(movement.loadKg??null)||stored.catalogue&&(stored.catalogue.id!==movement.catalogueId||stored.catalogue.defaultPeriodOptionId!==period.optionId||stored.catalogue.loadBasis!==stored.loadBasis||stored.catalogue.displayName!==stored.name))throw new UnexpectedApiResponse()})
  }else if(detail.optionId!==null||detail.option||detail.movements.length)throw new UnexpectedApiResponse()
 }
 if('optionId' in periods[0])validateExercisePreview(root.input as ExerciseEntryV2Request,root.preview as ExercisePreviewV2)
 else if(root.preview.totalMinutes!==periods[0].durationMinutes||root.preview.partial||root.preview.knownEnergyKj!==root.preview.periods[0].estimatedKj)throw new UnexpectedApiResponse()
}
