import {useRef,useState} from 'react'
import {useQueryClient} from '@tanstack/react-query'
import type {ApiError,FoodRecord,ExerciseRecord,ExerciseEstimate} from '../api/contracts'
import {ApiRequestError} from '../api/errors'
import {updateFoodRecord,updateExerciseRecord,deleteRecord} from '../api/recordMutations'
import {fetchFoodRecord,fetchExerciseRecord} from '../api/records'
import type {CorrectionIntent} from '../features/entry/correctionIntent'
import {matchesCorrectionIntent} from '../features/entry/correctionIntent'
import type {LegacyRecordMutationState,RecordMutationIntent,MutationConfirmation} from '../features/entry/recordMutationTypes'
import {legacyRecordMutation,mutationExpectedVersion} from '../features/entry/recordMutationTypes'
import {useDraft} from './useDraft'
import {useSession} from './useSession'
import {queryKeys} from '../app/queryKeys'
type PendingMutation=Extract<LegacyRecordMutationState,{status:'saving'|'unknown'}>
export function useRecordMutations(){
  const draft=useDraft(),{state,actions}=useSession(),queries=useQueryClient(),lock=useRef(false)
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(''),[currentPreview,setCurrentPreview]=useState<ExerciseEstimate|null>(null)
  const owner=state.status==='authenticated'?state.user.id:'',epoch=state.epoch
  function current(){const now=actions.getSnapshot();return now.status==='authenticated'&&now.user.id===owner&&now.epoch===epoch}
  function stamp(pending:{ownerEpoch:number;mutationId:string}){return {ownerId:owner,ownerEpoch:pending.ownerEpoch,mutationId:pending.mutationId}}
  async function refresh(pending?:{ownerEpoch:number;mutationId:string}){
    if(!current())return;const saved=legacyRecordMutation(draft.getSnapshot().recordMutation);if(saved.status!=='saved')return;const target=pending??saved
    try{
      if(saved.intent.operation==='DELETE'){
        const key=queryKeys.record(owner,saved.ownerEpoch,saved.intent.kind,saved.intent.recordId)
        await Promise.all([
          queries.invalidateQueries({queryKey:['owner',owner],predicate:query=>!(query.queryKey.length===key.length&&key.every((part,index)=>query.queryKey[index]===part))},{throwOnError:true}),
          queries.invalidateQueries({queryKey:key,exact:true},{throwOnError:true}).catch(error=>{if(!(error instanceof ApiRequestError&&error.status===404&&error.code==='NOT_FOUND'))throw error}),
        ])
      }else await queries.invalidateQueries({queryKey:['owner',owner]},{throwOnError:true})
      if(current())draft.dispatch({type:'RECORD_MUTATION_REFRESH',...stamp(target),failed:false})
    }
    catch{if(current())draft.dispatch({type:'RECORD_MUTATION_REFRESH',...stamp(target),failed:true})}
  }
  async function accept(pending:PendingMutation,result:MutationConfirmation){
    if(!current())return
    draft.dispatch({type:'RECORD_MUTATION_CONFIRMED',...stamp(pending),...result})
    const next=legacyRecordMutation(draft.getSnapshot().recordMutation)
    if(next.status==='saved'&&next.mutationId===pending.mutationId){setMessage('');await refresh(pending)}else setMessage('The returned record does not match the frozen change. Keep checking the original record.')
  }
  function reject(pending:PendingMutation,error:ApiError,observedRecord?:FoodRecord|ExerciseRecord,changedPreview?:ExerciseEstimate){if(current())draft.dispatch({type:'RECORD_MUTATION_REJECTED',...stamp(pending),error,observedRecord,changedPreview})}
  function observedError(pending:PendingMutation,status:number,code:string,text:string):ApiError{return {status,code,message:text,path:`/api/records/${pending.intent.kind}/${pending.intent.recordId}`,requestId:'local-observed-state',fieldErrors:[],retryable:false}}
  async function perform(pending:PendingMutation){
    try{
      const intent=pending.intent
      if(intent.operation==='DELETE'){await deleteRecord(intent.kind,intent.recordId,intent.expectedVersion);await accept(pending,{result:'DELETED'})}
      else{const row=intent.kind==='food'?await updateFoodRecord(intent.recordId,intent.body):await updateExerciseRecord(intent.recordId,intent.body);await accept(pending,{result:'UPDATED',record:row})}
    }catch(error){
      if(!current())return;setMessage(error instanceof Error?error.message:'The change result could not be verified.')
      if(error instanceof ApiRequestError&&error.outcome==='rejected'){
        reject(pending,{status:error.status>=400&&error.status<500?error.status:400,code:error.code,message:error.message,path:`/api/records/${pending.intent.kind}/${pending.intent.recordId}`,requestId:error.requestId??'local-validation',fieldErrors:error.fieldErrors,retryable:error.retryable},undefined,error.currentPreview);setCurrentPreview(error.currentPreview??null)
        if(error.code==='CALCULATION_CHANGED'||error.code==='REFERENCE_INACTIVE')void queries.invalidateQueries({queryKey:['owner',owner]})
      }else draft.dispatch({type:'RECORD_MUTATION_UNKNOWN',...stamp(pending)})
    }
  }
  async function begin(intent:RecordMutationIntent,expectedRevision?:number){
    if(lock.current||!current())return
    if(!draft.dispatch({type:'BEGIN_RECORD_MUTATION',ownerId:owner,ownerEpoch:epoch,mutationId:crypto.randomUUID(),intent,expectedRevision}))return
    const pending=legacyRecordMutation(draft.getSnapshot().recordMutation);if(pending.status!=='saving')return
    lock.current=true;setBusy(true);setMessage('');setCurrentPreview(null)
    try{await perform(pending)}finally{lock.current=false;if(current())setBusy(false)}
  }
  function update(intent:CorrectionIntent,expectedRevision:number){return begin({operation:'UPDATE',...intent},expectedRevision)}
  function remove(kind:'food'|'exercise',original:FoodRecord|ExerciseRecord){return begin({operation:'DELETE',kind,recordId:original.id,expectedVersion:original.version,original})}
  function resume():PendingMutation|null{
    if(lock.current||!current())return null;const pending=legacyRecordMutation(draft.getSnapshot().recordMutation)
    if(pending.status!=='unknown'||pending.ownerEpoch!==epoch||!draft.dispatch({type:'RECORD_MUTATION_RESUME',...stamp(pending)}))return null
    lock.current=true;setBusy(true);setMessage('');return pending
  }
  async function retry(){const pending=resume();if(!pending)return;try{await perform(pending)}finally{lock.current=false;if(current())setBusy(false)}}
  async function recover(){
    const pending=resume();if(!pending)return
    try{
      const intent=pending.intent,row=intent.kind==='food'?await fetchFoodRecord(intent.recordId):await fetchExerciseRecord(intent.recordId)
      if(!current())return
      if(intent.operation==='UPDATE'&&matchesCorrectionIntent(intent,row))await accept(pending,{result:'MATCHES',record:row})
      else if(row.version>mutationExpectedVersion(intent)){reject(pending,observedError(pending,409,'VERSION_CONFLICT','The current record differs from the reviewed change. Review the latest state before making another change.'),row);setMessage('The current record has a later, different version. Your draft and its original version are retained.')}
      else{draft.dispatch({type:'RECORD_MUTATION_UNKNOWN',...stamp(pending)});setMessage('The original change may still be running. Keep its frozen input and expected version; check again or retry exactly that change.')}
    }catch(error){
      if(!current())return
      if(error instanceof ApiRequestError&&error.status===404&&error.code==='NOT_FOUND'){if(pending.intent.operation==='DELETE')await accept(pending,{result:'UNAVAILABLE'});else{reject(pending,observedError(pending,404,'NOT_FOUND','This previously read record is now unavailable. The correction is not confirmed.'));setMessage('The record is no longer available. Your correction input has been retained.')}}
      else{draft.dispatch({type:'RECORD_MUTATION_UNKNOWN',...stamp(pending)});setMessage(error instanceof Error?error.message:'Current record could not be checked.')}
    }finally{lock.current=false;if(current())setBusy(false)}
  }
  return {update,remove,retry,recover,retryRefresh:()=>refresh(),busy,message,currentPreview}
}
