import {useSaveFeedback} from '../features/feedback/SaveFeedbackContext'
import {useRef,useState} from 'react'
import {useQueryClient} from '@tanstack/react-query'
import type {SubmissionReceipt,ExerciseEstimate,FoodPreviewV2,ExercisePreviewV2,SubmissionReceiptV2} from '../api/contracts'
import type {EntrySubmissionV2} from '../api/submissionsV2'
import {createRecordsV2,fetchSubmissionV2,verifyReceiptV2} from '../api/submissionsV2'
import {createDraft,createWorkoutDraft} from '../features/entry/draftReducer'
import {todayInZone} from '../domain/dates'
import type {CreatePayload} from '../features/entry/draftTypes'
import {ApiRequestError} from '../api/errors'
import {createRecords,fetchSubmissionReceipt} from '../api/submissions'
import {matchesReceipt} from '../features/entry/draftReducer'
import {useDraft} from './useDraft'
import {useSession} from './useSession'
export function useSubmitDraft(){
  const {store:feedback}=useSaveFeedback()
  const draft=useDraft(),{state,actions}=useSession(),queries=useQueryClient(),lock=useRef(false)
  const [busy,setBusy]=useState(false),[message,setMessage]=useState('')
  const owner=state.status==='authenticated'?state.user.id:'',epoch=state.epoch
  function current(){const now=actions.getSnapshot();return now.status==='authenticated'&&now.user.id===owner&&now.epoch===epoch}
  async function refresh(){if(!current())return;try{await queries.invalidateQueries({queryKey:['owner',owner]},{throwOnError:true});if(current())draft.dispatch({type:'REFRESH_COMPLETED',ownerId:owner})}catch{if(current())draft.dispatch({type:'REFRESH_FAILED',ownerId:owner})}}
  async function accept(payload:CreatePayload,proof:SubmissionReceipt){
    if(!current())return
    if(!matchesReceipt(payload,proof)){draft.dispatch({type:'SUBMIT_UNKNOWN',ownerId:owner,ownerEpoch:epoch,submissionId:payload.submissionId});setMessage('The returned proof does not match this submission. Keep checking its original receipt.');return}
    draft.dispatch({type:'SUBMIT_CONFIRMED',ownerId:owner,ownerEpoch:epoch,receipt:proof});setMessage('');await refresh()
  }
  async function perform(payload:CreatePayload){
    try{await accept(payload,await createRecords(payload))}
    catch(error){
      if(!current())return
      setMessage(error instanceof Error?error.message:'The save result could not be verified.')
      if(error instanceof ApiRequestError&&error.outcome==='rejected'){
        draft.dispatch({type:'SUBMIT_REJECTED',ownerId:owner,ownerEpoch:epoch,submissionId:payload.submissionId,changedPreview:error.currentPreview,error:{status:error.status>=400&&error.status<500?error.status:400,code:error.code,message:error.message,path:'items' in payload?'/api/records/food/batch':'/api/records/exercise',requestId:error.requestId??'local-validation',fieldErrors:error.fieldErrors,retryable:error.retryable}})
        if(error.code==='CALCULATION_CHANGED'||error.code==='REFERENCE_INACTIVE')void queries.invalidateQueries({queryKey:['owner',owner]})
      }else draft.dispatch({type:'SUBMIT_UNKNOWN',ownerId:owner,ownerEpoch:epoch,submissionId:payload.submissionId})
    }
  }
  async function refreshV2(noticeId:number){if(!current())return;const key=feedback.getSnapshot()?.event.key;try{await queries.invalidateQueries({queryKey:['owner',owner]},{throwOnError:true});if(current()){draft.dispatch({type:'V2_REFRESH',ownerId:owner,noticeId,failed:false});if(key)feedback.refresh(key,false)}}catch{if(current()){draft.dispatch({type:'V2_REFRESH',ownerId:owner,noticeId,failed:true});if(key)feedback.refresh(key,true)}}}
  async function acceptV2(envelope:EntrySubmissionV2,proof:SubmissionReceiptV2){if(!current())return;try{verifyReceiptV2(envelope,proof)}catch{draft.dispatch({type:'SUBMIT_V2_UNKNOWN',ownerId:owner,ownerEpoch:epoch,submissionId:envelope.payload.submissionId});setMessage('The returned proof does not match every entry. Keep checking its original receipt.');return}const original=draft.getSnapshot().draft;if(!original||original.kind!==(envelope.domain==='FOOD'?'food':'exercise'))return;const today=todayInZone(),next=original.kind==='exercise'&&original.entryKind==='WORKOUT'?createWorkoutDraft(owner,today,'MANUAL'):createDraft(owner,original.kind,today,'MANUAL');if(original.kind==='food'&&next.kind==='food'){next.defaultMealType=original.defaultMealType;if(next.composer)next.composer.mealType=original.defaultMealType;}const accepted=draft.dispatch({type:'SUBMIT_V2_CONFIRMED',ownerId:owner,ownerEpoch:epoch,receipt:proof,nextDraft:next,today,noticeAt:Date.now()});const saved=draft.getSnapshot().lastSaved;if(!accepted||saved?.receipt.submissionId!==proof.submissionId)return;setMessage('');await refreshV2(saved.noticeId)}
  async function performV2(envelope:EntrySubmissionV2){try{await acceptV2(envelope,await createRecordsV2(envelope))}catch(error){if(!current())return;setMessage(error instanceof Error?error.message:'The save result could not be verified.');if(error instanceof ApiRequestError&&error.outcome==='rejected'&&error.code!=='IDEMPOTENCY_CONFLICT'&&error.code!=='CLIENT_ENTRY_CONFLICT'){draft.dispatch({type:'SUBMIT_V2_REJECTED',ownerId:owner,ownerEpoch:epoch,submissionId:envelope.payload.submissionId,error:{status:error.status,code:error.code,message:error.message,path:envelope.domain==='FOOD'?'/api/v2/food/batches':'/api/v2/exercise/entries',requestId:error.requestId??'local-validation',fieldErrors:error.fieldErrors,retryable:error.retryable}});if(error.code==='CALCULATION_CHANGED'||error.code==='REFERENCE_INACTIVE')void queries.invalidateQueries({queryKey:['owner',owner]})}else draft.dispatch({type:'SUBMIT_V2_UNKNOWN',ownerId:owner,ownerEpoch:epoch,submissionId:envelope.payload.submissionId})}}
  async function confirmV2(envelope:EntrySubmissionV2,expectedRevision:number,preview?:FoodPreviewV2|ExercisePreviewV2){if(lock.current||!current())return;if(!draft.dispatch({type:'BEGIN_SUBMIT_V2',ownerId:owner,ownerEpoch:epoch,expectedRevision,envelope,...(envelope.domain==='FOOD'?{foodPreview:preview as FoodPreviewV2|undefined}:{exercisePreview:preview as ExercisePreviewV2|undefined}),today:todayInZone()}))return;const frozen=draft.getSnapshot().submission;if(frozen.status!=='saving'||frozen.apiVersion!==2)return;lock.current=true;setBusy(true);setMessage('');try{await performV2(frozen.envelope)}finally{lock.current=false;if(current())setBusy(false)}}
  async function confirm(payload:CreatePayload,expectedRevision:number,reviewedEstimate?:ExerciseEstimate){
    if(lock.current||!current())return
    if(!draft.beginSubmission(payload,epoch,expectedRevision,reviewedEstimate))return
    const frozen=draft.getSnapshot().submission;if(frozen.status!=='saving'||frozen.apiVersion===2)return
    lock.current=true;setBusy(true);setMessage('')
    try{await perform(frozen.frozenPayload)}finally{lock.current=false;if(current())setBusy(false)}
  }
  async function retry(){
    const frozen=draft.getSnapshot().submission
    if(lock.current||!current()||frozen.status!=='unknown'||frozen.ownerEpoch!==epoch)return
    lock.current=true;setBusy(true);setMessage('')
    try{if(frozen.apiVersion===2)await performV2(frozen.envelope);else await perform(frozen.frozenPayload)}finally{lock.current=false;if(current())setBusy(false)}
  }
  async function recover(){
    const frozen=draft.getSnapshot().submission
    if(lock.current||!current()||frozen.status!=='unknown'||frozen.ownerEpoch!==epoch)return
    lock.current=true;setBusy(true);setMessage('')
    try{if(frozen.apiVersion===2)await acceptV2(frozen.envelope,await fetchSubmissionV2(frozen.envelope));else await accept(frozen.frozenPayload,await fetchSubmissionReceipt(frozen.submissionId))}
    catch(error){if(current())setMessage(error instanceof ApiRequestError&&error.status===404?'No committed receipt is visible yet. The original request may still be running. Keep its original ID and payload.':error instanceof Error?error.message:'Receipt check unavailable.')}
    finally{lock.current=false;if(current())setBusy(false)}
  }
  return {confirm,confirmV2,retry,recover,retryRefresh:()=>{const saved=draft.getSnapshot().lastSaved;return saved?refreshV2(saved.noticeId):refresh()},busy,message}
}
