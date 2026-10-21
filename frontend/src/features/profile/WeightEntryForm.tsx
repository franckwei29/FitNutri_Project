import { useCallback,useEffect,useRef,useState } from 'react'
import { useQuery,useQueryClient } from '@tanstack/react-query'
import { Alert,Button,TextField } from '@mui/material'
import type { WeightEntry,WeightRequest,WeightResult } from '../../api/contracts'
import { fetchWeights,upsertWeight } from '../../api/weight'
import { ApiRequestError } from '../../api/errors'
import { ConfirmDialog,DateField,ErrorBox,LoadingButton } from '../../components/presentation'
import { messageOf } from '../../components/formHelpers'
import { DEFAULT_ZONE,isValidLocalDate,shiftCalendarDate,todayInZone } from '../../domain/dates'
import { queryKeys } from '../../app/queryKeys'
import { useSession } from '../../hooks/useSession'
import { useNavigationSafety } from '../../hooks/useNavigationSafety'

export function WeightEntryForm() {
  const {state,actions}=useSession(),navigation=useNavigationSafety(),queries=useQueryClient()
  const owner=state.status==='authenticated'?state.user.id:'',epoch=state.epoch
  const [today]=useState(()=>todayInZone()),[date,setDate]=useState(()=>state.status==='authenticated'?state.weightRecovery?.recordDate??today:today),[value,setValue]=useState(()=>state.status==='authenticated'&&state.weightRecovery?String(state.weightRecovery.weightKg):''),[correction,setCorrection]=useState<WeightEntry|null>(null)
  const [from,setFrom]=useState(()=>shiftCalendarDate(today,-89)),[to,setTo]=useState(today)
  const [busy,setBusy]=useState(false),[checking,setChecking]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('')
  const [unknown,setUnknown]=useState<WeightRequest|null>(()=>state.status==='authenticated'?state.weightRecovery??null:null),[conflict,setConflict]=useState<{request:WeightRequest;current:WeightEntry}|null>(null),[confirmed,setConfirmed]=useState<WeightResult|null>(null)
  const lock=useRef(false),validRange=isValidLocalDate(from)&&isValidLocalDate(to)&&from<=to&&to<=today&&shiftCalendarDate(from,365)>=to
  const history=useQuery({queryKey:queryKeys.weights(owner,from,to),queryFn:({signal})=>fetchWeights(from,to,signal),enabled:!!owner&&validRange})
  const reset=useCallback(()=>{setValue('');setCorrection(null)},[]),updateNavigation=navigation?.updateForm
  const pending=busy?'saving':unknown?'unknown':null,dirty=value!==''
  useEffect(()=>{updateNavigation?.('weight',{dirty,pending,onDiscard:reset});return ()=>updateNavigation?.('weight',null)},[dirty,pending,reset,updateNavigation])
  function currentOwner() {const current=actions.getSnapshot();return current.status==='authenticated'&&current.epoch===epoch&&current.user.id===owner}
  async function saved(result:WeightResult) {
    if(!currentOwner()) return
    setConfirmed(result);setUnknown(null);setConflict(null);reset();setNotice('Weight saved. Existing activity records keep their saved estimates.')
    try {actions.clearWeightRecovery(epoch);await Promise.all([queries.invalidateQueries({queryKey:['owner',owner]}),actions.refreshProfile()])}
    catch {setError('Weight was saved, but the latest profile could not be refreshed. Retry the profile read before changing settings.')}
  }
  async function submit(request:WeightRequest) {
    if(lock.current) return;lock.current=true;setBusy(true);setError('');setNotice('')
    try {const result=await upsertWeight(request);await saved(result)}
    catch(e) {
      if(!currentOwner()) return
      setError(messageOf(e))
      if(e instanceof ApiRequestError&&e.outcome==='unknown') {actions.markWeightWriteUnknown(request,epoch);setUnknown(request);setConflict(null)}
      else if(e instanceof ApiRequestError&&e.code==='VERSION_CONFLICT'&&e.currentItem) setConflict({request,current:e.currentItem})
    } finally {lock.current=false;setBusy(false)}
  }
  function submitForm(e:React.FormEvent) {
    e.preventDefault();if(unknown||lock.current) return
    if(!/^\d+(?:\.\d{1,2})?$/.test(value)||Number(value)<20||Number(value)>400||!isValidLocalDate(date)||date>today) {setError('Enter 20 to 400 kg with up to 2 decimal places and a valid non-future date.');return}
    void submit({recordDate:date,weightKg:Number(value),expectedVersion:correction?.version??null,zone:DEFAULT_ZONE})
  }
  async function checkSaved() {
    if(!unknown||checking) return;setChecking(true);setError('')
    try {
      const rows=await fetchWeights(unknown.recordDate,unknown.recordDate)
      if(!currentOwner()) return
      const row=rows[0]
      if(row&&row.weightKg===unknown.weightKg) {
        await actions.refreshProfile();await queries.invalidateQueries({queryKey:['owner',owner]});actions.clearWeightRecovery(epoch);setUnknown(null);reset();setNotice('The saved measurement is confirmed. No second save was sent.')
      } else if(row) {setConflict({request:unknown,current:row});setError('The server has a different measurement. Review it before making a correction.')}
      else setError('This measurement is not confirmed yet. Check again; no second save has been sent.')
    } catch(e) {setError(messageOf(e))} finally {setChecking(false)}
  }
  const profile=state.status==='authenticated'?state.profile:null
  const current=confirmed?.profileVersion!=null&&(profile?.profileVersion??-1)<confirmed.profileVersion?confirmed.currentWeight:profile?.currentWeight??confirmed?.currentWeight
  return <section className="card form-section"><p className="eyebrow">DATED MEASUREMENTS</p><h2>Add or correct a weight entry</h2>
    <div className="weight-current"><span>Latest weight</span><strong>{current?`${current.weightKg} kg`:'Not recorded'}</strong>{current&&<small>{current.recordDate}</small>}</div>
    <ErrorBox message={error} />{notice&&<Alert severity="success">{notice}</Alert>}
    {unknown&&<><Alert severity="warning">The save result is unknown. Check the saved measurement before repeating a change.</Alert><LoadingButton busy={checking} onClick={()=>void checkSaved()}>Check saved weight</LoadingButton></>}
    <form onSubmit={submitForm} noValidate><fieldset disabled={busy||!!unknown} className="profile-fieldset"><div className="form-grid"><TextField label="Weight (kg)" type="number" value={value} onChange={e=>setValue(e.target.value)} slotProps={{htmlInput:{step:'0.01'}}} /><DateField label="Weight date" today={today} value={date} onChange={next=>{setDate(next);setCorrection(null)}} /></div>
      <p className="tiny-note">{correction?`You are correcting the saved measurement for ${correction.recordDate}.`:'New dates keep earlier measurements. For an existing date, review its saved value before confirming a correction.'}</p><div className="actions"><Button onClick={reset}>Clear weight form</Button><LoadingButton type="submit" busy={busy} variant="contained">{correction?'Save correction':'Save weight'}</LoadingButton></div>
    </fieldset></form>
    <h3>Weight history</h3><div className="form-grid"><DateField label="History from" today={today} value={from} onChange={setFrom} /><DateField label="History to" today={today} value={to} onChange={setTo} /></div>
    {!validRange?<Alert severity="error">Choose a non-future date range of 1 to 366 days.</Alert>:history.isPending?<p role="status">Loading weight history…</p>:history.isError?<><ErrorBox message="Weight history could not be loaded." /><Button onClick={()=>void history.refetch()}>Retry weight history</Button></>:<div className="weight-history">{history.data?.length?history.data.map(row=><div className="summary-line" key={row.id}><span>{row.recordDate}</span><strong>{row.weightKg} kg</strong><Button disabled={busy||!!unknown} onClick={()=>{setDate(row.recordDate);setValue(String(row.weightKg));setCorrection(row);setError('');setNotice('')}}>Correct {row.recordDate}</Button></div>):<p>No weights recorded in this date range.</p>}</div>}
    <ConfirmDialog open={!!conflict} title="Review this day's saved weight" busy={busy} error={error} label="Confirm correction" onCancel={()=>setConflict(null)} onConfirm={()=>{if(conflict) void submit({...conflict.request,expectedVersion:conflict.current.version})}}>
      {conflict&&<p>For {conflict.current.recordDate}, the server has {conflict.current.weightKg} kg. Your proposed value is {conflict.request.weightKg} kg. Confirm to correct that measurement.</p>}
    </ConfirmDialog>
  </section>
}
