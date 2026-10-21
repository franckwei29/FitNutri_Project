import {standaloneFoods} from '../draftSelectors'
import {useEffect,useRef,useState} from 'react'
import {Alert,Button,LinearProgress} from '@mui/material'
import {PhotoCameraOutlined} from '@mui/icons-material'
import type {FoodCandidate,ImageRecognition} from '../../../api/contracts'
import {imageFileProblem,recognizeImage} from '../../../api/recognition'
import {ConfirmDialog,DateField} from '../../../components/presentation'
import {todayInZone} from '../../../domain/dates'
import {entryFailureMessage} from './entryFailure'
import type {FoodDraft} from '../draftTypes'
interface Props {draft:FoodDraft;available:boolean;checking?:boolean;isCurrent:()=>boolean;onAccept:(candidates:FoodCandidate[],expectedRevision:number)=>boolean;onManual:()=>void;onDateChange:(date:string)=>void}
interface Acceptance {candidates:FoodCandidate[];revision:number;generation:number}
/** Files and previews stay local to this mounted panel; processing never calls record APIs. */
export function ImageEntryPanel({draft,available,checking=false,isCurrent,onAccept,onManual,onDateChange}:Props){
  const [selection,setSelection]=useState<{file:File;url:string}|null>(null),[phase,setPhase]=useState<'idle'|'selected'|'processing'|'candidates'|'error'>('idle')
  const [result,setResult]=useState<ImageRecognition|null>(null),[error,setError]=useState(''),[notice,setNotice]=useState(''),[replacement,setReplacement]=useState<Acceptance|null>(null)
  const mounted=useRef(true),generation=useRef(0),acceptedGeneration=useRef(-1),request=useRef<AbortController|null>(null),inFlight=useRef(false),preview=useRef<string|null>(null)
  useEffect(()=>{mounted.current=true;return()=>{mounted.current=false;request.current?.abort();if(preview.current)URL.revokeObjectURL(preview.current);preview.current=null}},[])
  function stop(){generation.current++;request.current?.abort();request.current=null;inFlight.current=false;setResult(null);setPhase(selection?'selected':'idle');setNotice('Stopped waiting for image results. Your food details are unchanged.');setError('')}
  function choose(file:File|undefined){
    generation.current++;request.current?.abort();request.current=null;inFlight.current=false;setResult(null);setReplacement(null);setError('');setNotice('')
    if(preview.current)URL.revokeObjectURL(preview.current);preview.current=null
    if(!file){setSelection(null);setPhase('idle');return}
    const problem=imageFileProblem(file);if(problem){setSelection(null);setPhase('error');setError(problem);return}
    const url=URL.createObjectURL(file);preview.current=url;setSelection({file,url});setPhase('selected')
  }
  async function process(){
    if(!selection||inFlight.current||phase==='processing'||!available||!isCurrent())return
    inFlight.current=true;const token=++generation.current,controller=new AbortController();request.current=controller;setPhase('processing');setResult(null);setError('');setNotice('')
    const current=()=>mounted.current&&generation.current===token&&isCurrent()&&!controller.signal.aborted
    try{const response=await recognizeImage(selection.file,controller.signal);if(!current())return;setResult(response);setPhase('candidates')}
    catch(e){if(current()){setError(entryFailureMessage(e,['image']));setPhase('error')}}
    finally{if(generation.current===token){request.current=null;inFlight.current=false}}
  }
  function apply(value:Acceptance){
    if(!mounted.current||!isCurrent()||!available||generation.current!==value.generation||acceptedGeneration.current===value.generation)return
    if(onAccept(value.candidates,value.revision)){acceptedGeneration.current=value.generation;setReplacement(null);setResult(null);setNotice('Candidates added to your food details. Review each item below.')}
    else{setReplacement(null);setError('Your food details changed. Review them before choosing candidates again.')}
  }
  function accept(){
    if(!result?.candidates.length||!isCurrent()||!available)return
    const value={candidates:structuredClone(result.candidates),revision:draft.revision,generation:generation.current}
    const edited=standaloneFoods(draft).some(row=>row.suggestion||row.referenceId||row.quantityGramsInput||row.mealType)
    if(edited)setReplacement(value);else apply(value)
  }
  const assisted=standaloneFoods(draft).some(row=>row.suggestion),processing=phase==='processing'
  return <section className="card form-section stack" aria-label="Image entry">
    <h2>Start with a food image</h2><p>Image suggestions are a starting point. You choose the reference, grams and meal before saving.</p>
    {!assisted&&<DateField value={draft.recordDate} today={todayInZone()} onChange={onDateChange} />}
    {!available&&!checking&&<Alert severity="warning">Image processing is currently unavailable. You can continue with manual entry.</Alert>}
    <label className="upload-zone"><PhotoCameraOutlined /><strong>Choose a food image</strong><span>JPEG, PNG or WebP · up to 8 MiB · up to 4,096 pixels per side</span><input aria-label="Food image" type="file" accept="image/jpeg,image/png,image/webp" disabled={!available} style={{maxWidth:'90%'}} onChange={event=>{const file=event.currentTarget.files?.[0];event.currentTarget.value='';choose(file)}} /></label>
    {selection&&<div className="image-preview"><img src={selection.url} alt="Selected food image for review" /></div>}
    {error&&<Alert severity="error">{error}</Alert>}{notice&&<Alert severity="info">{notice}</Alert>}
    {processing&&<><p role="status">Processing image… Your food details stay unsaved.</p><LinearProgress aria-label="Processing food image" /></>}
    <div className="actions"><Button variant="contained" disabled={!selection||processing||!available} onClick={()=>void process()}>Process image</Button>{processing?<Button onClick={stop}>Stop waiting for image results</Button>:selection&&<Button onClick={()=>choose(undefined)}>Remove selected image</Button>}{!assisted&&<Button onClick={()=>{stop();onManual()}}>Continue with manual entry</Button>}</div>
    {result&&<section aria-label="Pending image candidates"><h3>Pending food suggestions</h3>{result.notices.map((text,index)=><p key={index}>{text}</p>)}{!result.candidates.length&&<p role="status">No food candidates were returned. Try another image or enter foods manually.</p>}<ol>{result.candidates.map(candidate=><li key={candidate.candidateKey}><strong>{candidate.proposedName}</strong><p>{candidate.suggestedGrams===null?'Grams need to be entered.':`${candidate.suggestedGrams} g suggested; you must confirm the actual portion.`}</p><p>{candidate.referenceMatches.length} matching references available for review.</p>{candidate.warnings.map((text,index)=><p className="tiny-note" key={index}>{text}</p>)}</li>)}</ol><Button disabled={!result.candidates.length||!available} onClick={accept}>Use candidates in food entry</Button></section>}
    <ConfirmDialog open={!!replacement} title="Replace current food candidates?" label="Replace food candidates" onCancel={()=>setReplacement(null)} onConfirm={()=>{if(replacement)apply(replacement)}}>Your current unsaved food details will be replaced. The selected date and saved records are retained.</ConfirmDialog>
  </section>
}
