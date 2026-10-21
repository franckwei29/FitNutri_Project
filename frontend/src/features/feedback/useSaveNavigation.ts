import {useEffect} from 'react'
import {useNavigate} from 'react-router-dom'
import {useSaveFeedback} from './SaveFeedbackContext'
import {useDraft} from '../../hooks/useDraft'
/** Wait for the rendered draft AND router blocker to observe confirmation first. */
export function useSaveNavigation(){
 const {store,value}=useSaveFeedback(),{state}=useDraft(),navigate=useNavigate(),saved=state.lastSaved
 useEffect(()=>{
  if(!value||value.navigated||!saved||state.submission.status!=='idle'||value.event.key!==`${state.ownerId}:${saved.ownerEpoch}:${saved.receipt.submissionId}`)return
  let active=true
  queueMicrotask(()=>{if(!active)return;store.navigate(value.event.key);void navigate('/dashboard',{replace:true})})
  return ()=>{active=false}
 },[value,saved,state.ownerId,state.submission.status,store,navigate])
}
