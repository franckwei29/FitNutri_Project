import {ExerciseEntryEditorV2} from '../features/entry/exercise/ExerciseEntryEditorV2'
import {createFoodRow} from '../features/entry/draftReducer'
import {standaloneFoods,usesExerciseV2} from '../features/entry/draftSelectors'
import {Link,Navigate,useNavigate} from 'react-router-dom'
import {Alert,Button} from '@mui/material'
import {Page,ErrorBox} from '../components/presentation'
import {useDraft} from '../hooks/useDraft'
import {useSession} from '../hooks/useSession'
import {useCapabilities} from '../hooks/useCapabilities'
import {FoodEditor} from '../features/entry/FoodEditor'
import {ExerciseEditor} from '../features/entry/ExerciseEditor'
import {ReviewPage} from '../features/entry/ReviewPage'
import type {EntrySource} from '../api/contracts'
import {VoiceEntryFlow} from '../features/entry/media/VoiceEntryFlow'
import {ImageEntryPanel} from '../features/entry/media/ImageEntryPanel'
import {mapImageCandidates} from '../features/entry/media/imageDraft'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
export function EntryPage(){
  const {state,dispatch,getSnapshot}=useDraft(),session=useSession(),caps=useCapabilities(),navigate=useNavigate()
  if(session.state.status!=='authenticated'||!session.state.profile)return null
  if(pendingDraftOperation(state)||state.submission.status==='saved'||(state.recordMutation.status==='saved'&&state.recordMutation.intent.operation==='UPDATE'))return <ReviewPage />
  if(state.draft?.edit?.root)return <ReviewPage />
  const draft=state.draft
  if(!draft)return <Navigate to="/add" replace />
  const profile=session.state.profile,scope={ownerId:session.state.user.id,epoch:session.state.epoch,catalogueRevision:caps.data?.catalogueRevision??'unavailable'}
  const unavailable=!caps.data?.catalogues[draft.kind].ready||!caps.data.catalogues[draft.kind].verified
  const current=(source:EntrySource)=>{const identity=session.actions.getSnapshot(),snapshot=getSnapshot();return identity.status==='authenticated'&&identity.user.id===scope.ownerId&&identity.epoch===scope.epoch&&snapshot.draft?.ownerId===scope.ownerId&&snapshot.draft?.source===source&&snapshot.draft.kind===draft.kind&&!snapshot.draft.edit&&!pendingDraftOperation(snapshot)}
  const voice=draft.source==='VOICE'&&!draft.edit
  const image=draft.kind==='food'&&draft.source==='IMAGE'&&!draft.edit
  return <Page title={draft.edit?(draft.kind==='food'?'Your food, your details.':'Your activity, your details.'):(draft.kind==='food'?'Add food':'Add exercise')} eyebrow="MAKE IT YOURS" subtitle={draft.edit?'Update this saved record after reviewing your changes.':'Choose an official reference and check the date and quantity before review.'} narrow>
    {caps.isPending&&<p role="status">Checking official catalogue availability…</p>}{caps.isError&&<><ErrorBox message="Catalogue availability could not be checked." /><Button onClick={()=>void caps.refetch()}>Retry catalogue availability</Button></>}
    {caps.data&&unavailable&&<Alert severity="warning">{caps.data.catalogues[draft.kind].reason}</Alert>}
    {image&&draft.kind==='food'&&<ImageEntryPanel key={`${scope.ownerId}:${scope.epoch}`} draft={draft} available={!caps.isError&&!unavailable&&!!caps.data?.features.image.available} checking={caps.isPending} isCurrent={()=>current('IMAGE')} onAccept={(candidates,expectedRevision)=>current('IMAGE')&&dispatch({type:'IMPORT_IMAGE_CANDIDATES',ownerId:scope.ownerId,expectedRevision,rows:mapImageCandidates(candidates)})} onManual={()=>{if(current('IMAGE'))dispatch({type:'USE_MANUAL_ENTRY',ownerId:scope.ownerId,expectedRevision:draft.revision,composer:{...createFoodRow(),mealType:draft.defaultMealType}})}} onDateChange={value=>dispatch({type:'CHANGE_DATE',ownerId:scope.ownerId,value})} />}
    {voice?<VoiceEntryFlow key={`${scope.ownerId}:${scope.epoch}:${draft.kind}`} draft={draft} scope={scope} unit={profile.energyUnit} profileVersion={profile.profileVersion} disabled={caps.isError||unavailable} transcriptionAvailable={!!caps.data?.features.transcription.available} parsingAvailable={!!caps.data?.features.speechParsing.available} isCurrent={()=>current('VOICE')} dispatch={dispatch} onReview={()=>navigate('/review')} />:image&&draft.kind==='food'&&!standaloneFoods(draft).some(row=>row.suggestion)?null:draft.source!=='MANUAL'&&!image&&!draft.edit?<><Alert severity="info">This entry method is currently unavailable. Your entry has not been saved.</Alert><Button component={Link} to="/add">Choose another method</Button></>:draft.kind==='food'?<FoodEditor undo={state.undoRemoval} draft={draft} scope={scope} dispatch={dispatch} unit={profile.energyUnit} disabled={!draft.edit&&(caps.isError||unavailable)} onReview={()=>navigate('/review')} />:draft.source==='MANUAL'&&!draft.edit&&usesExerciseV2(draft)?<ExerciseEntryEditorV2 undo={state.undoRemoval} draft={draft} scope={scope} dispatch={dispatch} unit={profile.energyUnit} profileVersion={profile.profileVersion} disabled={caps.isError||unavailable} onReview={()=>navigate('/review')} />:<ExerciseEditor draft={draft} scope={scope} dispatch={dispatch} unit={profile.energyUnit} profileVersion={profile.profileVersion} disabled={!draft.edit&&(caps.isError||unavailable)} onReview={()=>navigate('/review')} />}
    <p className="tiny-note">{draft.edit?'Your changes stay unsaved until you review and confirm them. The existing record and its source are retained.':'Entries stay unsaved until you review and confirm them. Changing a reference or portion requires a fresh check.'}</p>
  </Page>
}
