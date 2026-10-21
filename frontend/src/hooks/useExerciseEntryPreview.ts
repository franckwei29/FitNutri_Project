import {rootCorrectionInput} from '../features/entry/rootCorrectionIntent'
import type {ExerciseCorrectionPreviewV2Request} from '../api/contracts'
import {useQuery} from '@tanstack/react-query'
import type {ExerciseDraft} from '../features/entry/draftTypes'
import type {CatalogueScope} from './useFoodSearch'
import {validateDraft} from '../features/entry/draftValidation'
import {exercisePreviewInput} from '../features/workout/exerciseInputsV2'
import {previewExerciseV2,previewExerciseCorrectionV2} from '../api/exerciseEntriesV2'
import {todayInZone} from '../domain/dates'
/** Real dated option/period preview; never estimates energy from movements or optional equipment details. */
export function useExerciseEntryPreview(draft:ExerciseDraft,scope:CatalogueScope,profileVersion:number|null,enabled=true){const errors=validateDraft(draft,scope.ownerId,todayInZone());let input:ReturnType<typeof exercisePreviewInput>|null=null;try{if(!errors.length)input=exercisePreviewInput(draft)}catch{/* Unselected or malformed inputs have no authority to preview. */}const correction=draft.edit?.root?draft.edit:null,valid=enabled&&!!input,signature=JSON.stringify(input),query=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'exercise-entry-preview',scope.catalogueRevision,profileVersion,correction?.root?.kind??null,correction?.recordId??null,correction?.expectedVersion??null,signature],queryFn:({signal})=>correction?previewExerciseCorrectionV2(correction.root!.kind as 'quick'|'workout',correction.recordId,rootCorrectionInput(draft) as ExerciseCorrectionPreviewV2Request,signal):previewExerciseV2(input!,signal),enabled:valid,retry:false});return {...query,errors,signature,data:valid?query.data:undefined,ready:valid&&query.isSuccess&&!query.isFetching}}
