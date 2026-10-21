import {RootMutationBoundary} from './RootMutationBoundary'
import {legacyRecordMutation} from '../features/entry/recordMutationTypes'
import {Alert,Button} from '@mui/material'
import {useDraft} from '../hooks/useDraft'
import {useRecordMutations} from '../hooks/useRecordMutations'
export function RecordMutationBoundary(){const {state}=useDraft();return state.recordMutation.apiVersion===2?<RootMutationBoundary />:<LegacyRecordMutationBoundary />}
function LegacyRecordMutationBoundary(){
  const {state,dispatch}=useDraft(),mutation=useRecordMutations(),current=legacyRecordMutation(state.recordMutation)
  if(current.status==='idle'||current.status==='rejected')return null
  if(current.status==='saving')return <Alert severity="info" role="status">Waiting for confirmation of the record change. Its original input and version remain frozen.</Alert>
  if(current.status==='unknown')return <section className="card form-section"><Alert severity="warning">A record change is awaiting confirmation. Its original input and version remain frozen.</Alert>{mutation.message&&<p role="status">{mutation.message}</p>}<div className="actions"><Button disabled={mutation.busy} onClick={()=>void mutation.recover()}>Check current record</Button><Button disabled={mutation.busy} onClick={()=>void mutation.retry()}>Retry exact change</Button></div><p className="tiny-note">A read-back comparison checks current saved state. It does not identify which request caused a matching value.</p></section>
  if(current.status!=='saved')return null
  const text=current.result==='UPDATED'?'Record changes saved.':current.result==='MATCHES'?'The current record matches the reviewed changes. This does not identify which request changed it.':current.result==='DELETED'?'The record is deleted. It is excluded from lists and summaries.':'The previously read record is no longer visible. This does not prove which request removed it.'
  return <Alert severity={current.result==='UNAVAILABLE'?'info':'success'} action={<Button onClick={()=>dispatch({type:'RESET_RECORD_MUTATION',ownerId:state.ownerId!})}>Dismiss notice</Button>}>{text}{current.refreshFailed&&<><p>Some views could not refresh. The confirmed state is retained.</p><Button onClick={()=>void mutation.retryRefresh()}>Retry refreshed record views</Button></>}</Alert>
}
