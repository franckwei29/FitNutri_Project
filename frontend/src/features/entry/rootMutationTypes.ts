import type {ApiError,FoodRootV2,ExerciseRootV2,FoodUpdateV2Request,ExerciseUpdateV2Request,FoodPreviewV2,ExercisePreviewV2} from '../../api/contracts'
import type {RootKind} from '../history/historySearch'
export type SavedRoot=FoodRootV2|ExerciseRootV2
export type RootUpdateIntent={apiVersion:2;operation:'UPDATE';kind:'food';rootKind:'single'|'dish';recordId:string;returnTo:string;original:FoodRootV2;body:FoodUpdateV2Request;reviewedPreview:FoodPreviewV2}|{apiVersion:2;operation:'UPDATE';kind:'exercise';rootKind:'quick'|'workout';recordId:string;returnTo:string;original:ExerciseRootV2;body:ExerciseUpdateV2Request;reviewedPreview:ExercisePreviewV2}
export type RootMutationIntent=RootUpdateIntent|{apiVersion:2;operation:'DELETE';kind:'food'|'exercise';rootKind:RootKind;recordId:string;original:SavedRoot;expectedVersion:number}
type Pending={apiVersion:2;ownerEpoch:number;mutationId:string;intent:RootMutationIntent}
export type RootConfirmation={result:'UPDATED'|'MATCHES';record:SavedRoot}|{result:'DELETED'|'UNAVAILABLE';record?:never}
export type RootMutationState=({status:'saving'|'unknown'}&Pending)|({status:'rejected';error:ApiError}&Pending)|({status:'saved';refreshFailed:boolean}&Pending&RootConfirmation)
type Stamp={ownerId:string;ownerEpoch:number;mutationId:string}
export type RootMutationAction=(Stamp&{type:'BEGIN_ROOT_MUTATION';intent:RootMutationIntent;expectedRevision?:number;today?:string})|(Stamp&{type:'ROOT_MUTATION_UNKNOWN'|'ROOT_MUTATION_RESUME'})|(Stamp&{type:'ROOT_MUTATION_REJECTED';error:ApiError})|(Stamp&{type:'ROOT_MUTATION_CONFIRMED'}&RootConfirmation)|(Stamp&{type:'ROOT_MUTATION_REFRESH';failed:boolean})
export function rootExpectedVersion(intent:RootMutationIntent){return intent.operation==='UPDATE'?intent.body.expectedVersion:intent.expectedVersion}
