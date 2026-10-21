import {useEffect,useRef,useState} from 'react'
import {Button} from '@mui/material'
import {DeleteOutlined} from '@mui/icons-material'
import type {FoodRecord,ExerciseRecord} from '../api/contracts'
import {ConfirmDialog,ErrorBox} from './presentation'
import {useRecordMutations} from '../hooks/useRecordMutations'
import {useDraft} from '../hooks/useDraft'
import {useSession} from '../hooks/useSession'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
export function RecordDeletionControl({kind,record,onLeaveDetails,onReload,disabled=false}:{kind:'food'|'exercise';record:FoodRecord|ExerciseRecord;onLeaveDetails:()=>void;onReload:()=>Promise<unknown>|void;disabled?:boolean}){
  const draft=useDraft(),mutation=useRecordMutations(),{state,actions}=useSession(),mounted=useRef(true)
  const [selected,setSelected]=useState<FoodRecord|ExerciseRecord|null>(null),[error,setError]=useState('')
  const owner=state.status==='authenticated'?state.user.id:'',epoch=state.epoch,pending=pendingDraftOperation(draft.state)
  useEffect(()=>{mounted.current=true;return ()=>{mounted.current=false}},[])
  function current(){const now=actions.getSnapshot();return mounted.current&&now.status==='authenticated'&&now.user.id===owner&&now.epoch===epoch}
  async function confirm(){
    if(!selected||pending||mutation.busy||!current())return
    setError('');const operation=mutation.remove(kind,selected),started=draft.getSnapshot().recordMutation
    if(started.status!=='saving'||started.intent.operation!=='DELETE'||started.intent.recordId!==selected.id)return
    await operation;if(!current())return
    const result=draft.getSnapshot().recordMutation;if(result.status==='idle'||result.mutationId!==started.mutationId)return
    if(result.status==='saved'||result.status==='unknown'){setSelected(null);onLeaveDetails()}
    else if(result.status==='rejected')setError(result.error.message)
  }
  async function reload(){setSelected(null);setError('');try{await onReload()}catch{if(current())setError('The current saved record could not be reloaded. Check it before deciding to delete.') }}
  return <><Button color="error" startIcon={<DeleteOutlined />} disabled={disabled||!!pending||mutation.busy||!!draft.state.draft} onClick={()=>{setSelected(structuredClone(record));setError('')}}>Delete record</Button>{!selected&&error&&<ErrorBox message={error} />}
    <ConfirmDialog open={!!selected} title="Delete this saved record?" label="Confirm deletion" danger busy={mutation.busy||pending==='saving'} error={error} onCancel={()=>{setSelected(null);setError('')}} onConfirm={()=>void confirm()}>
      {selected&&<p>{'nutrients' in selected?selected.foodName:selected.exerciseName} · {selected.recordDate}. This entry will be removed from your visible history and the totals for this date.</p>}
      {error&&<Button onClick={()=>void reload()}>Reload current saved record</Button>}
    </ConfirmDialog>
  </>
}
