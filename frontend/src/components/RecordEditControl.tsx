import {useEffect,useRef,useState} from 'react'
import {flushSync} from 'react-dom'
import {useNavigate} from 'react-router-dom'
import {Button} from '@mui/material'
import {EditOutlined} from '@mui/icons-material'
import type {FoodRecord,ExerciseRecord} from '../api/contracts'
import {fetchFoodRecord,fetchExerciseRecord} from '../api/records'
import {loadEditDraft} from '../features/entry/correctionIntent'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
import {useDraft} from '../hooks/useDraft'
import {useSession} from '../hooks/useSession'
import {ConfirmDialog,ErrorBox} from './presentation'
export function RecordEditControl({kind,id,disabled=false}:{kind:'food'|'exercise';id:string;disabled?:boolean}){
  const draft=useDraft(),{state,actions}=useSession(),navigate=useNavigate(),lock=useRef(false),mounted=useRef(true),controller=useRef<AbortController|null>(null)
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[replacement,setReplacement]=useState<FoodRecord|ExerciseRecord|null>(null)
  const owner=state.status==='authenticated'?state.user.id:'',epoch=state.epoch
  useEffect(()=>{mounted.current=true;return ()=>{mounted.current=false;controller.current?.abort()}},[])
  function current(){const now=actions.getSnapshot();return mounted.current&&now.status==='authenticated'&&now.user.id===owner&&now.epoch===epoch}
  function begin(record:FoodRecord|ExerciseRecord,replace=false){
    if(!current()||pendingDraftOperation(draft.getSnapshot()))return
    if(draft.getSnapshot().draft&&!replace){setReplacement(record);return}
    let started=false;flushSync(()=>{if(replace)draft.dispatch({type:'DISCARD',ownerId:owner});started=draft.dispatch({type:'LOAD_EDIT',ownerId:owner,draft:loadEditDraft(owner,kind,record)})})
    if(started)navigate('/entry')
  }
  async function load(){
    if(lock.current||!current()||pendingDraftOperation(draft.getSnapshot()))return
    lock.current=true;setBusy(true);setError('');controller.current=new AbortController()
    try{const record=kind==='food'?await fetchFoodRecord(id,controller.current.signal):await fetchExerciseRecord(id,controller.current.signal);if(current())begin(record)}catch(error){if(current())setError(error instanceof Error?error.message:'The current record could not be loaded.')}
    finally{lock.current=false;if(current())setBusy(false)}
  }
  return <><Button startIcon={<EditOutlined />} disabled={disabled||busy||!!pendingDraftOperation(draft.state)} onClick={()=>void load()}>Edit record</Button><ErrorBox message={error} /><ConfirmDialog open={!!replacement} title="Replace the current unsubmitted draft?" label="Replace draft" onCancel={()=>setReplacement(null)} onConfirm={()=>{if(replacement)begin(replacement,true)}}>Your unsubmitted changes will be discarded. Confirmed records remain unchanged.</ConfirmDialog></>
}
