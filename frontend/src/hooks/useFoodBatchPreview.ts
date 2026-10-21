import {rootCorrectionInput} from '../features/entry/rootCorrectionIntent'
import type {FoodCorrectionPreviewV2Request} from '../api/contracts'
import {useQuery} from '@tanstack/react-query'
import type {FoodDraft} from '../features/entry/draftTypes'
import type {CatalogueScope} from './useFoodSearch'
import {validateDraft} from '../features/entry/draftValidation'
import {foodPreviewInput} from '../features/food/foodInputsV2'
import {previewFoodV2,previewFoodCorrectionV2} from '../api/foodEntriesV2'
import {todayInZone} from '../domain/dates'
/** The complete ordered batch is bound to every display-only preview; no last-good value authorises new input. */
export function useFoodBatchPreview(draft:FoodDraft,scope:CatalogueScope,enabled=true){const errors=validateDraft(draft,scope.ownerId,todayInZone());let input:ReturnType<typeof foodPreviewInput>|null=null;try{if(!errors.length)input=foodPreviewInput(draft)}catch{/* Invalid selected references never request a preview. */}const correction=draft.edit?.root&&draft.edit.root.original.input.kind!=='QUICK'&&draft.edit.root.original.input.kind!=='WORKOUT'?draft.edit:null,signature=JSON.stringify(input),valid=enabled&&!!input;const query=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'food-batch-preview',scope.catalogueRevision,correction?.root?.kind??null,correction?.recordId??null,correction?.expectedVersion??null,signature],queryFn:({signal})=>correction?previewFoodCorrectionV2(correction.root!.kind as 'single'|'dish',correction.recordId,rootCorrectionInput(draft) as FoodCorrectionPreviewV2Request,signal):previewFoodV2(input!,signal),enabled:valid,retry:false});return {...query,errors,signature,data:valid?query.data:undefined,ready:valid&&query.isSuccess&&!query.isFetching}}
