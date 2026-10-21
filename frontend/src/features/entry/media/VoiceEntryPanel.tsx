import {standaloneFoods} from '../draftSelectors'
import {useEffect,useRef,useState} from 'react'
import {Alert,Button,LinearProgress,TextField} from '@mui/material'
import {MicNoneOutlined} from '@mui/icons-material'
import type {ParsedEntry} from '../../../api/contracts'
import {parseTranscript,transcribeAudio} from '../../../api/speech'
import type {RecordedAudio} from '../../../domain/media'
import {MAX_RECORDING_MS} from '../../../domain/media'
import {ConfirmDialog} from '../../../components/presentation'
import type {Draft} from '../draftTypes'
import {Recorder,isSupported} from './Recorder'
import {exerciseHasInput} from '../../workout/workoutSelectors'
import type {RecorderState} from './Recorder'
import {entryFailureMessage} from './entryFailure'
type Phase='idle'|'requestingPermission'|'recording'|'stopping'|'audioReady'|'transcribing'|'transcriptReady'|'extracting'|'candidates'|'error'
interface Props {draft:Draft;transcriptionAvailable:boolean;parsingAvailable:boolean;isCurrent:()=>boolean;onAccept:(parsed:ParsedEntry,expectedRevision:number)=>boolean;onManual:()=>void;onEntryBlocked:(value:boolean)=>void}
interface Acceptance {parsed:ParsedEntry;revision:number;generation:number;text:string}
const activePhases:Phase[]=['requestingPermission','recording','stopping','transcribing','extracting']
/** Ephemeral recording/transcript only. Every imported candidate still uses shared review/save. */
export function VoiceEntryPanel({draft,transcriptionAvailable,parsingAvailable,isCurrent,onAccept,onManual,onEntryBlocked}:Props){
 const [phase,setPhase]=useState<Phase>('idle'),[elapsed,setElapsed]=useState(0),[recording,setRecording]=useState<RecordedAudio|null>(null),[transcript,setTranscript]=useState(''),[hasTranscript,setHasTranscript]=useState(false)
 const [pending,setPending]=useState<ParsedEntry|null>(null),[error,setError]=useState(''),[notice,setNotice]=useState(''),[stale,setStaleState]=useState(!!draft.voiceInputStale),[replacement,setReplacement]=useState<Acceptance|null>(null),[newRecording,setNewRecording]=useState(false)
 const recorder=useRef<Recorder|null>(null),mounted=useRef(false),generation=useRef(0),request=useRef<AbortController|null>(null),busy=useRef(false),staleRef=useRef(!!draft.voiceInputStale),textRef=useRef(''),acceptedText=useRef<string|null>(null)
 const assisted=draft.kind==='food'?standaloneFoods(draft).some(row=>row.suggestion):!!draft.suggestion,kind=draft.kind==='food'?'FOOD':'EXERCISE',supported=isSupported()
 useEffect(()=>{mounted.current=true;recorder.current=new Recorder();return()=>{mounted.current=false;request.current?.abort();recorder.current?.dispose();recorder.current=null}},[])
 function current(token:number){return mounted.current&&generation.current===token&&isCurrent()}
 function transition(next:Phase){busy.current=activePhases.includes(next);setPhase(next);onEntryBlocked(busy.current||staleRef.current)}
 function setStale(value:boolean){staleRef.current=value;setStaleState(value);onEntryBlocked(value||busy.current)}
 function invalidate(){generation.current++;request.current?.abort();request.current=null;setPending(null);setReplacement(null)}
 function acceptState(state:RecorderState,token:number){
  if(!current(token))return
  setElapsed(state.elapsedMs)
  if(state.status==='ready'){setRecording(state.recording);transition('audioReady')}
  else if(state.status==='error'){setError(state.message);transition('error')}
  else transition(state.status)
 }
 async function beginRecording(){
  setNewRecording(false);if(busy.current||!isCurrent()||!transcriptionAvailable)return
  invalidate();recorder.current?.cancel();setRecording(null);setTranscript('');textRef.current='';setHasTranscript(false);setError('');setNotice('');setStale(assisted)
  const token=generation.current;transition('requestingPermission')
  try{await recorder.current!.start({maxDurationMs:MAX_RECORDING_MS,onState:state=>acceptState(state,token)})}catch(e){if(current(token)){setError(entryFailureMessage(e,[]));transition('error')}}
 }
 async function stopRecording(){try{await recorder.current?.stop()}catch{/* Recorder state supplies the safe error/cancellation; no invented Blob. */}}
 function cancelRecording(){invalidate();recorder.current?.cancel();setRecording(null);setError('');setNotice('Recording canceled. Your current entry details are unchanged.');transition(hasTranscript?'transcriptReady':'idle')}
 async function transcribe(){
  if(!recording||busy.current||!isCurrent()||!transcriptionAvailable)return
  const token=++generation.current,controller=new AbortController();request.current=controller;setError('');setNotice('');transition('transcribing')
  try{const response=await transcribeAudio(recording,controller.signal);if(!current(token))return;recorder.current?.cancel();setRecording(null);textRef.current=response.transcript;setTranscript(response.transcript);setHasTranscript(true);setStale(assisted);setNotice(response.notices.join(' '));transition('transcriptReady')}
  catch(e){if(current(token)){setError(entryFailureMessage(e,['audio']));transition('error')}}
  finally{if(current(token))request.current=null}
 }
 function changeText(value:string){invalidate();textRef.current=value;setTranscript(value);setError('');setNotice('Transcript changed. Extract it again before using its details.');setStale(assisted);transition('transcriptReady')}
 async function extract(){
  if(busy.current||!isCurrent()||!parsingAvailable||!hasTranscript||!transcript.trim()||transcript.length>10000)return
  const token=++generation.current,controller=new AbortController(),text=transcript;request.current=controller;setPending(null);setError('');setNotice('');setStale(assisted);transition('extracting')
  try{const result=await parseTranscript(kind,text,controller.signal);if(!current(token)||textRef.current!==text)return;setPending(result);transition('candidates')}
  catch(e){if(current(token)){setError(entryFailureMessage(e,['baseDate','transcript','recordDate','zone']));transition('error')}}
  finally{if(current(token))request.current=null}
 }
 function apply(value:Acceptance){
  if(!current(value.generation)||!parsingAvailable||textRef.current!==value.text)return
  if(onAccept(value.parsed,value.revision)){acceptedText.current=value.text;setPending(null);setReplacement(null);setStale(false);setNotice('Extracted details added below. Review the references, date and quantities before saving.');transition('transcriptReady')}
  else{setReplacement(null);setError('Your entry details changed. Review them before replacing candidates.');transition('error')}
 }
 function useDetails(){
  if(!pending||!parsingAvailable||!isCurrent()||pending.kind==='FOOD'&&!pending.items.length)return
  const value={parsed:structuredClone(pending),revision:draft.revision,generation:generation.current,text:transcript}
  const edited=assisted||(draft.kind==='food'?standaloneFoods(draft).some(row=>row.referenceId||row.quantityGramsInput||row.mealType):exerciseHasInput(draft))
  if(edited)setReplacement(value);else apply(value)
 }
 function keepCurrent(){invalidate();recorder.current?.cancel();setRecording(null);const text=acceptedText.current??'';textRef.current=text;setTranscript(text);setHasTranscript(!!text);setError('');setNotice('Kept your current entry details. The new recording or transcript changes were discarded.');setStale(false);transition(text?'transcriptReady':'idle')}
 function stopWaiting(){invalidate();setError('');setNotice('Stopped waiting for voice results. Your entry details are unchanged.');transition(hasTranscript?'transcriptReady':recording?'audioReady':'idle')}
 const isBusy=activePhases.includes(phase),capturing=['requestingPermission','recording','stopping'].includes(phase),processing=phase==='transcribing'||phase==='extracting',canUse=!!pending&&(pending.kind==='EXERCISE'||pending.items.length>0)
 return <section className="card form-section stack" aria-label="Voice entry">
  <h2>Start with a spoken {draft.kind==='food'?'food':'activity'} description</h2><p>Record, transcribe, edit and extract. Nothing is saved until the shared review is confirmed.</p>
  {!supported&&<Alert severity="warning">Microphone recording is unavailable in this browser. Continue with manual entry.</Alert>}{(!transcriptionAvailable||!parsingAvailable)&&<Alert severity="warning">Voice processing is currently unavailable. Your current details are retained.</Alert>}
  <div className="voice-stage"><span className={`mic-button ${phase==='recording'?'recording':''}`} aria-hidden="true"><MicNoneOutlined /></span><strong>{phase==='recording'?`Recording · ${Math.floor(elapsed/1000)} s`:phase==='requestingPermission'?'Waiting for microphone permission':phase==='stopping'?'Finishing recording':'Up to 60 seconds · WebM or MP4 · 8 MiB'}</strong></div>
  {capturing&&<p role="status">{phase==='recording'?'Microphone recording is active. Stop or cancel when finished.':'Waiting for the browser recording lifecycle.'}</p>}
  <div className="actions">{phase==='recording'?<Button variant="contained" onClick={()=>void stopRecording()}>Stop recording</Button>:<Button variant="contained" disabled={!supported||!transcriptionAvailable||isBusy} onClick={()=>transcript||recording?setNewRecording(true):void beginRecording()}>Start recording</Button>}{capturing&&<Button onClick={cancelRecording}>Cancel recording</Button>}{recording&&!capturing&&<Button disabled={isBusy||!transcriptionAvailable} onClick={()=>void transcribe()}>Transcribe recording</Button>}{!assisted&&<Button onClick={()=>{invalidate();recorder.current?.cancel();onManual()}}>Continue with manual entry</Button>}</div>
  {recording&&!capturing&&<p>{(recording.durationMs/1000).toFixed(1)} seconds recorded. Transcription sends this recording to the configured service; the audio is not saved as a record.</p>}
  {processing&&<><LinearProgress aria-label={phase==='transcribing'?'Transcribing recording':'Extracting voice details'} /><p role="status">{phase==='transcribing'?'Transcribing audio…':'Extracting unconfirmed details…'}</p><Button onClick={stopWaiting}>Stop waiting for voice results</Button></>}
  {error&&<Alert severity="error">{error}</Alert>}{notice&&<Alert severity="info">{notice}</Alert>}
  {hasTranscript&&<><TextField label="Editable transcript" multiline minRows={3} value={transcript} disabled={capturing||phase==='transcribing'} onChange={event=>changeText(event.target.value)} helperText="Edit the transcript before extraction. Use explicit grams, meal, activity minutes and today/yesterday or YYYY-MM-DD dates. Unsupported quantities stay missing." slotProps={{htmlInput:{maxLength:10000}}} /><Button variant="outlined" disabled={isBusy||!parsingAvailable||!transcript.trim()||transcript.length>10000} onClick={()=>void extract()}>Extract {draft.kind==='food'?'food':'exercise'} details</Button></>}
  {assisted&&stale&&<><Alert severity="warning">The transcript or recording changed. Re-extract and accept the new details, or explicitly keep your current {draft.kind} details.</Alert><Button onClick={keepCurrent}>Keep current {draft.kind} details</Button></>}
  {pending&&<section aria-label="Pending voice details"><h3>Pending extracted details</h3><p>{pending.recordDate}{pending.dateWasDefaulted?' · Date defaulted; review it.':''}</p>{pending.warnings.map((text,i)=><p key={i}>{text}</p>)}{pending.kind==='FOOD'?<>{!pending.items.length&&<p>No food details were extracted. Edit the transcript or continue manually.</p>}<ol>{pending.items.map(item=><li key={item.candidateKey}><strong>{item.proposedName}</strong><p>{item.suggestedGrams===null?'Grams missing':`${item.suggestedGrams} g to review`} · {item.mealType??'Meal missing'}</p>{item.warnings.map((text,i)=><p key={i}>{text}</p>)}</li>)}</ol></>:<p>{pending.exercise?.proposedName??'Activity type missing'} · {pending.exercise?.durationMinutes===null?'Minutes missing':`${pending.exercise?.durationMinutes} minutes to review`}</p>}<Button disabled={!canUse||!parsingAvailable} onClick={useDetails}>Use extracted details</Button></section>}
  <ConfirmDialog open={!!replacement} title="Replace current voice details?" label="Replace voice details" onCancel={()=>setReplacement(null)} onConfirm={()=>{if(replacement)apply(replacement)}}>Your current unsaved edited details will be replaced by these unconfirmed candidates. Saved records are retained.</ConfirmDialog>
  <ConfirmDialog open={newRecording} title="Start a new recording?" label="Start new recording" onCancel={()=>setNewRecording(false)} onConfirm={()=>void beginRecording()}>The current local recording and transcript will be discarded. Your existing entry details are retained until you accept new candidates.</ConfirmDialog>
 </section>
}
