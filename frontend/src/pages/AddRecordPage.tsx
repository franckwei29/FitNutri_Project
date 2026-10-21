import {hasBusinessDraft} from '../features/entry/draftBusinessInput'
import {useState} from 'react'
import {flushSync} from 'react-dom'
import {Link,useNavigate,useSearchParams} from 'react-router-dom'
import {Alert,Button} from '@mui/material'
import {ArrowForward,Check,DirectionsRun,EditOutlined,MicNoneOutlined,PhotoCameraOutlined,RestaurantOutlined} from '@mui/icons-material'
import type {EntrySource} from '../api/contracts'
import {Page,ConfirmDialog,ErrorBox} from '../components/presentation'
import {useDraft} from '../hooks/useDraft'
import {useSession} from '../hooks/useSession'
import {useCapabilities} from '../hooks/useCapabilities'
import {createDraft,createWorkoutDraft} from '../features/entry/draftReducer'
import {todayInZone} from '../domain/dates'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
export function AddRecordPage(){
  const [params]=useSearchParams(),navigate=useNavigate(),{state,dispatch}=useDraft(),session=useSession(),capabilities=useCapabilities()
  const [kind,setKind]=useState<'food'|'exercise'>(params.get('kind')==='exercise'?'exercise':'food'),[method,setMethod]=useState<EntrySource>('MANUAL'),[replace,setReplace]=useState(false)
  if(session.state.status!=='authenticated')return null
  const owner=session.state.user.id,caps=capabilities.data,frozen=!!pendingDraftOperation(state)
  const catalogueReady=!!caps?.catalogues[kind].ready&&!!caps.catalogues[kind].verified
  const permitted=(source:EntrySource)=>catalogueReady&&(source==='MANUAL'||(source==='IMAGE'?kind==='food'&&!!caps?.features.image.available:!!caps?.features.transcription.available&&!!caps.features.speechParsing.available))
  function begin(){if(frozen||!permitted(method))return;const next=kind==='exercise'&&method==='MANUAL'&&state.preferredExerciseMode==='WORKOUT'?createWorkoutDraft(owner,todayInZone(),method):createDraft(owner,kind,todayInZone(),method);if(next.kind==='food'&&method==='MANUAL'){next.defaultMealType=state.preferredMealType??'';if(next.composer)next.composer.mealType=next.defaultMealType}flushSync(()=>dispatch({type:'START_NEW',ownerId:owner,draft:next,replaceConfirmed:replace||!hasBusinessDraft(state.draft)}));setReplace(false);navigate('/entry')}
  return <Page title="Add a record." eyebrow="MAKE ROOM FOR YOUR DAY" subtitle="Choose what to record and the easiest way to enter it." narrow back="/dashboard">
    {state.draft&&(hasBusinessDraft(state.draft)||frozen)&&<Alert severity="info" action={<Button onClick={()=>navigate(frozen?'/review':'/entry')}>Resume entry</Button>}>You have an unsaved {state.draft.kind} entry.{frozen?' Its submission is awaiting confirmation.':''}</Alert>}
    {capabilities.isPending&&<p role="status">Checking available entry methods…</p>}{capabilities.isError&&<><ErrorBox message="Entry availability could not be checked." /><Button onClick={()=>void capabilities.refetch()}>Retry availability</Button></>}
    <section className="card form-section entry-picker"><h2>What are you recording?</h2><div className="two-col">{(['food','exercise'] as const).map(value=><button key={value} className={`record-kind ${kind===value?'selected':''}`} disabled={frozen} aria-pressed={kind===value} onClick={()=>{setKind(value);if(value==='exercise'&&method==='IMAGE')setMethod('MANUAL')}}><span className="picker-icon">{value==='food'?<RestaurantOutlined />:<DirectionsRun />}</span><div><strong>{value==='food'?'Food':'Exercise'}</strong><p>{value==='food'?'Log a meal or a snack':'Log your physical activity'}</p></div>{kind===value&&<Check fontSize="small" />}</button>)}</div>
      <h2 className="picker-heading">How would you like to enter it?</h2><div className="method-grid">{[{id:'MANUAL' as const,title:'Manual entry',text:'Search, select and add your details.',icon:<EditOutlined />},{id:'VOICE' as const,title:'Voice entry',text:'Start with a spoken description.',icon:<MicNoneOutlined />},{id:'IMAGE' as const,title:'Upload food image',text:'Review foods suggested from a photo.',icon:<PhotoCameraOutlined />}].map(item=><button key={item.id} className={`method-card ${method===item.id?'selected':''}`} disabled={frozen||capabilities.isError||!permitted(item.id)} aria-pressed={method===item.id} onClick={()=>setMethod(item.id)}><span className="picker-icon">{item.icon}</span><strong>{item.title}</strong><p>{item.text}</p>{item.id==='IMAGE'&&<span className="tag">FOOD ONLY</span>}{!permitted(item.id)&&<small>{!catalogueReady?caps?.catalogues[kind].reason??'Checking catalogue availability…':item.id==='VOICE'?caps?.features.transcription.reason??caps?.features.speechParsing.reason:caps?.features.image.reason}</small>}{method===item.id&&<Check className="selection-check" />}</button>)}</div>
      <div className="actions"><Button component={Link} to="/dashboard" disabled={frozen}>Cancel</Button><Button variant="contained" endIcon={<ArrowForward />} disabled={frozen||capabilities.isError||!permitted(method)} onClick={()=>hasBusinessDraft(state.draft)?setReplace(true):begin()}>Continue</Button></div>
    </section><div className="flow-note"><span>Enter</span><ArrowForward /><span>Review</span><ArrowForward /><span>Confirm &amp; save</span></div>
    <ConfirmDialog open={replace} title="Start a new entry?" label="Start new entry" onCancel={()=>setReplace(false)} onConfirm={begin}>Your current unsubmitted entry will be replaced. Confirmed records stay in History.</ConfirmDialog>
  </Page>
}
