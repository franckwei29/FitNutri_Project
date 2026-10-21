import {useCallback,useEffect,useRef,useState} from 'react'
import {Link} from 'react-router-dom'
import {Alert,Avatar,Button,Checkbox,Chip,FormControlLabel,LinearProgress,TextField} from '@mui/material'
import {AutoAwesomeOutlined,PersonOutlined,SendOutlined} from '@mui/icons-material'
import {DateField,Page} from '../components/presentation'
import {useSession} from '../hooks/useSession'
import {useCapabilities} from '../hooks/useCapabilities'
import type {SessionState,SessionController} from '../app/sessionEpoch'
import {askQuestion,qaRequestProblem} from '../api/qa'
import {ApiRequestError} from '../api/errors'
import type {QaRequest,QaResponse} from '../api/contracts'
import {DEFAULT_ZONE,shiftCalendarDate,todayInZone} from '../domain/dates'
function eligible(state:SessionState){return state.status==='authenticated'&&state.user.role==='USER'&&state.user.enabled&&!state.profileRecoveryPending&&!!state.profile?.adultConfirmed&&state.profile.onboardingCompleted&&state.profile.healthSafetyAnswer==='NO'&&state.profile.qaConsentGranted&&state.profile.qaEligibility.eligible}
export function ChatPage(){
 const {state,actions}=useSession(),capabilities=useCapabilities(),[restricted,setRestricted]=useState<string|null>(null),[checking,setChecking]=useState(false),[checkError,setCheckError]=useState('')
 if(state.status!=='authenticated'||state.user.role!=='USER'||!state.profile)return null
 const ownerKey=`${state.user.id}:${state.epoch}`,allowed=eligible(state),serverRestricted=restricted===ownerKey
 async function checkEligibility(){setChecking(true);setCheckError('');try{await actions.refreshProfile();const current=actions.getSnapshot();if(current.status==='authenticated'&&`${current.user.id}:${current.epoch}`===ownerKey&&eligible(current))setRestricted(null)}catch{setCheckError('Current eligibility could not be checked. Try again when the server is available.')}finally{setChecking(false)}}
 if(!allowed||serverRestricted)return <Page title="General health Q&A" eyebrow="YOUR WELLBEING" narrow><section className="card locked-chat"><h2>Q&A needs your consent and an eligible profile.</h2><p>Review your completed adult profile, safety response and current Q&A consent. Recording and factual summaries remain available.</p>{checkError&&<Alert severity="error">{checkError}</Alert>}<div className="actions"><Button component={Link} to="/profile" variant="contained">Review Profile</Button><Button component={Link} to="/add">Continue recording</Button><Button disabled={checking} onClick={()=>void checkEligibility()}>Check current eligibility</Button></div></section></Page>
 const available=!!capabilities.data?.features.qa.available&&!capabilities.isError&&!capabilities.isPending
 const availabilityKey=capabilities.isPending?'checking':capabilities.isError?'error':available?'available':'unavailable'
 return <Page title="General health Q&A" eyebrow="A LITTLE CLARITY" subtitle="General wellbeing information and your selected recorded summaries. Each question is independent; no conversation history is sent." narrow>
  {capabilities.isPending&&<p role="status">Checking Q&A service availability…</p>}{capabilities.isError&&<Alert severity="error">Q&A service availability could not be checked.</Alert>}{!capabilities.isPending&&!capabilities.isError&&!available&&<Alert severity="warning">{capabilities.data?.features.qa.reason??'The Q&A service is currently unavailable.'}</Alert>}{(capabilities.isError||!available)&&<Button onClick={()=>void capabilities.refetch()}>Retry service availability</Button>}
  <OwnedConversation key={`${ownerKey}:${availabilityKey}`} ownerId={state.user.id} epoch={state.epoch} actions={actions} available={available} onRestricted={()=>{setRestricted(ownerKey);void actions.refreshProfile().catch(()=>{})}} />
 </Page>
}
interface Message {key:string;request:QaRequest;status:'pending'|'answered'|'error';response?:QaResponse;error?:string}
function OwnedConversation({ownerId,epoch,actions,available,onRestricted}:{ownerId:string;epoch:number;actions:SessionController;available:boolean;onRestricted:()=>void}){
 const today=todayInZone(),[text,setText]=useState(''),[include,setInclude]=useState(false),[from,setFrom]=useState(()=>shiftCalendarDate(today,-6)),[to,setTo]=useState(today),[messages,setMessages]=useState<Message[]>([]),[pendingKey,setPendingKey]=useState<string|null>(null),[problem,setProblem]=useState(''),[cooldownUntil,setCooldownUntil]=useState(0),[now,setNow]=useState(()=>Date.now())
 const mounted=useRef(false),busy=useRef(false),generation=useRef(0),controller=useRef<AbortController|null>(null)
 useEffect(()=>{mounted.current=true;return()=>{mounted.current=false;controller.current?.abort()}},[])
 useEffect(()=>{if(cooldownUntil<=Date.now())return;const timer=setInterval(()=>{const timestamp=Date.now();setNow(timestamp);if(timestamp>=cooldownUntil)clearInterval(timer)},1000);return()=>clearInterval(timer)},[cooldownUntil])
 const remaining=Math.max(0,Math.ceil((cooldownUntil-now)/1000)),waiting=pendingKey!==null
 const current=useCallback(()=>{const state=actions.getSnapshot();return mounted.current&&state.status==='authenticated'&&state.user.id===ownerId&&state.epoch===epoch&&eligible(state)},[actions,ownerId,epoch])
 const perform=useCallback(async(request:QaRequest,key:string,retry=false)=>{
  if(busy.current||!available||!current()||Date.now()<cooldownUntil)return
  busy.current=true;setPendingKey(key);setProblem('');const token=++generation.current,abort=new AbortController();controller.current=abort
  setMessages(previous=>retry?previous.map(message=>message.key===key?{...message,status:'pending',response:undefined,error:undefined}:message):[...previous,{key,request:structuredClone(request),status:'pending'}])
  const valid=()=>current()&&generation.current===token&&!abort.signal.aborted
  try{const response=await askQuestion(request,abort.signal);if(valid())setMessages(previous=>previous.map(message=>message.key===key?{...message,status:'answered',response,error:undefined}:message))}
  catch(error){if(!valid())return
   if(error instanceof ApiRequestError&&(error.status===401||error.status===403)){onRestricted();if(error.status===401)actions.expire();else if(error.code!=='QA_RESTRICTED')void actions.restore();return}
   const detail=error instanceof ApiRequestError?error.fieldErrors.find(field=>['message','recordRange','recordRange.from','recordRange.to','zone'].includes(field.field))?.message:undefined
   const message=detail??(error instanceof ApiRequestError&&error.code==='REQUEST_TIMEOUT'?'The answer timed out. You can retry this question.':error instanceof Error?error.message:'The question could not be processed. Try again.')
   if(error instanceof ApiRequestError&&error.status===429){const start=Date.now();setNow(start);setCooldownUntil(start+(error.retryAfterSeconds??30)*1000)}
   setMessages(previous=>previous.map(entry=>entry.key===key?{...entry,status:'error',response:undefined,error:message}:entry))
  }finally{if(valid()){busy.current=false;controller.current=null;setPendingKey(null)}}
 },[actions,available,cooldownUntil,current,onRestricted])
 function send(){const request:QaRequest={message:text,recordRange:include?{from,to}:null,zone:DEFAULT_ZONE},invalid=qaRequestProblem(request);if(invalid){setProblem(invalid);return}if(busy.current||!available||!current()||Date.now()<cooldownUntil)return;setText('');void perform(request,crypto.randomUUID())}
 function stopWaiting(){generation.current++;controller.current?.abort();controller.current=null;busy.current=false;const key=pendingKey;setPendingKey(null);setMessages(previous=>previous.map(message=>message.key===key?{...message,status:'error',error:'Stopped waiting. The service may still process the earlier question; no record is written.'}:message))}
 function clear(){generation.current++;controller.current?.abort();controller.current=null;busy.current=false;setPendingKey(null);setMessages([]);setText('');setProblem('')}
 return <section className="card chat-card" aria-label="General health questions">
  <div className="chat-header" style={{flexWrap:'wrap'}}><span className="chat-mark"><AutoAwesomeOutlined /></span><div><h2>FitNutri Assistant</h2><p>General information · one question at a time</p></div><Chip label="Non-medical" size="small" /></div>
  <div className="stack" style={{padding:'16px 24px'}}><p className="tiny-note">Questions and answers stay only on this page. The service receives the current question and, only when selected, your recorded summary. Do not include personal identifiers or medical details.</p><FormControlLabel control={<Checkbox checked={include} disabled={!available||waiting} onChange={event=>setInclude(event.target.checked)} />} label="Include my recorded summary (up to 31 days)" />
   {include&&<div className="form-grid"><DateField label="Summary from" value={from} today={today} onChange={setFrom} /><DateField label="Summary to" value={to} today={today} onChange={setTo} /></div>}
   <div className="actions"><Button disabled={!messages.length&&!waiting} onClick={clear}>Clear conversation</Button><Button component={Link} to="/profile">Review Profile</Button></div>
  </div>
  {remaining>0&&<Alert severity="info">Please wait {remaining} seconds before asking again.</Alert>}
  <div className="chat-messages" role="log" aria-label="Question and answer messages" aria-live="polite">
   {!messages.length&&<p>Ask a general wellbeing question, or select a date range for a factual recorded summary.</p>}
   {messages.map((message,index)=><article key={message.key} aria-label={`Question ${index+1}`} className="stack"><div className="chat-message user"><Avatar className="avatar"><PersonOutlined /></Avatar><div style={{whiteSpace:'pre-wrap',overflowWrap:'anywhere'}}>{message.request.message}{message.request.recordRange&&<p className="tiny-note">Selected range: {message.request.recordRange.from} to {message.request.recordRange.to}</p>}</div></div>
    <div className="chat-message assistant"><Avatar className="avatar"><AutoAwesomeOutlined /></Avatar><div style={{whiteSpace:'pre-wrap',overflowWrap:'anywhere'}}>{message.status==='pending'?<><p role="status">Waiting for an answer…</p><LinearProgress aria-label={`Processing question ${index+1}`} /></>:message.status==='error'?<><Alert severity="error">{message.error}</Alert><Button disabled={!available||waiting||remaining>0} onClick={()=>void perform(message.request,message.key,true)}>Retry question {index+1}</Button></>:message.response&&<>{message.response.scope==='REFUSED'&&<Chip label="Out of scope" size="small" />}<p>{message.response.answer}</p>{message.response.recordRangeUsed?<p className="tiny-note">Recorded range used: {message.response.recordRangeUsed.from} to {message.response.recordRangeUsed.to}</p>:<p className="tiny-note">No recorded summary was sent for this answer.</p>}<p className="chat-disclaimer">{message.response.disclaimer}</p></>}</div></div>
   </article>)}
  </div>
  {waiting&&<Button onClick={stopWaiting}>Stop waiting for answer</Button>}
  {problem&&<Alert severity="error">{problem}</Alert>}
  <form className="chat-composer" onSubmit={event=>{event.preventDefault();send()}}><TextField label="Question" multiline minRows={2} fullWidth value={text} disabled={!available} onChange={event=>setText(event.target.value)} helperText={`${text.length}/2,000 characters. No conversation memory or record changes.`} slotProps={{htmlInput:{maxLength:2000}}} /><Button type="submit" variant="contained" aria-label="Send question" disabled={!available||waiting||remaining>0||!text.trim()}><SendOutlined /></Button></form>
 </section>
}
