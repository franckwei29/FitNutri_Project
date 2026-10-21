import type {RootMutationState} from './rootMutationTypes'
import type {ApiError,FoodRecord,ExerciseRecord,ExerciseEstimate} from '../../api/contracts'
import type {CorrectionIntent} from './correctionIntent'
export type RecordMutationIntent=({operation:'UPDATE'}&CorrectionIntent)|{operation:'DELETE';kind:'food'|'exercise';recordId:string;expectedVersion:number;original:FoodRecord|ExerciseRecord}
type Stamp={ownerId:string;ownerEpoch:number;mutationId:string}
type Pending={intent:RecordMutationIntent;ownerEpoch:number;mutationId:string}
export type MutationConfirmation={result:'UPDATED'|'MATCHES';record:FoodRecord|ExerciseRecord}|{result:'DELETED'|'UNAVAILABLE';record?:never}
export type LegacyRecordMutationState=({status:'idle'}|({status:'saving'|'unknown'}&Pending)|({status:'rejected';error:ApiError;observedRecord?:FoodRecord|ExerciseRecord;changedPreview?:ExerciseEstimate}&Pending)|({status:'saved';refreshFailed:boolean}&Pending&MutationConfirmation))&{apiVersion?:1}
export type RecordMutationState=LegacyRecordMutationState|RootMutationState
export function legacyRecordMutation(state:RecordMutationState):LegacyRecordMutationState{return state.apiVersion===2?{status:'idle'}:state}
export type RecordMutationAction=
 |(Stamp&{type:'BEGIN_RECORD_MUTATION';intent:RecordMutationIntent;expectedRevision?:number})
 |(Stamp&{type:'RECORD_MUTATION_UNKNOWN'})
 |(Stamp&{type:'RECORD_MUTATION_RESUME'})
 |(Stamp&{type:'RECORD_MUTATION_REJECTED';error:ApiError;observedRecord?:FoodRecord|ExerciseRecord;changedPreview?:ExerciseEstimate})
 |(Stamp&{type:'RECORD_MUTATION_CONFIRMED'}&MutationConfirmation)
 |(Stamp&{type:'RECORD_MUTATION_REFRESH';failed:boolean})
 |{type:'RESET_RECORD_MUTATION';ownerId:string}
export function mutationExpectedVersion(intent:RecordMutationIntent){return intent.operation==='DELETE'?intent.expectedVersion:intent.body.expectedVersion}
