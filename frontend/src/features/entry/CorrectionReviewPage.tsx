import {legacyRecordMutation} from './recordMutationTypes'
import {standaloneFoods,legacyExerciseId,exerciseDurationInput} from './draftSelectors'
import {useCallback,useEffect,useRef,useState} from 'react'
import {flushSync} from 'react-dom'
import {Link,useNavigate} from 'react-router-dom'
import {Alert,Button,Checkbox,FormControlLabel} from '@mui/material'
import {Check,EditOutlined} from '@mui/icons-material'
import type {FoodRecord,ExerciseRecord} from '../../api/contracts'
import {fetchFoodRecord,fetchExerciseRecord} from '../../api/records'
import {Page,ConfirmDialog,ErrorBox,LoadingButton} from '../../components/presentation'
import {useDraft} from '../../hooks/useDraft'
import {useSession} from '../../hooks/useSession'
import {useCapabilities} from '../../hooks/useCapabilities'
import {useRecordMutations} from '../../hooks/useRecordMutations'
import {useExerciseEstimate} from '../../hooks/useExerciseEstimate'
import {pendingDraftOperation} from './recordMutationState'
import {foodCalculationChanged,isCorrectionDirty,loadEditDraft,prepareCorrection} from './correctionIntent'
import {validateDraft} from './draftValidation'
import {ReviewFoodRow} from './ReviewFoodRow'
import type {RowPreview} from './FoodItemEditor'
import {foodInputSignature} from './previewIdentity'
import {savedRecordLocation} from '../history/historySearch'
import {todayInZone} from '../../domain/dates'
import {displayEnergy} from '../../domain/units'
import {mealLabels,sourceLabels} from '../../domain/labels'
import {SourceNote} from '../../components/SourceNote'
import {ChangedExerciseBasis} from '../../components/ChangedExerciseBasis'
export function CorrectionReviewPage(){
  const {state,dispatch}=useDraft(),session=useSession(),caps=useCapabilities(),mutation=useRecordMutations(),navigate=useNavigate(),mounted=useRef(true)
  const [previews,setPreviews]=useState<Record<string,RowPreview>>({}),[localError,setLocalError]=useState(''),[cancel,setCancel]=useState(false),[checking,setChecking]=useState(false),[latest,setLatest]=useState<FoodRecord|ExerciseRecord|null>(null),[replace,setReplace]=useState(false),[acknowledgedFingerprint,setAcknowledgedFingerprint]=useState<string|null>(null)
  const report=useCallback((key:string,value:RowPreview)=>setPreviews(old=>old[key]?.signature===value.signature&&old[key]?.preview===value.preview?old:{...old,[key]:value}),[])
  const owner=session.state.status==='authenticated'?session.state.user.id:'',epoch=session.state.epoch,profile=session.state.status==='authenticated'?session.state.profile:null,draft=state.draft,change=legacyRecordMutation(state.recordMutation),original=draft?.edit?.original
  const scope={ownerId:owner,epoch,catalogueRevision:caps.data?.catalogueRevision??'unavailable'},dirty=!!draft&&isCorrectionDirty(draft),pending=pendingDraftOperation(state)
  const exercise=useExerciseEstimate(draft?.kind==='exercise'&&dirty&&!pending?legacyExerciseId(draft):null,draft?.kind==='exercise'?exerciseDurationInput(draft):'',draft?.recordDate??'',scope,profile?.profileVersion??null,draft?.edit?.recordId)
  const attempted=change.status==='rejected'&&change.intent.operation==='UPDATE'&&change.intent.kind==='exercise'?change.intent:null
  const calculationChanged=!!(attempted&&change.status==='rejected'&&change.error.code==='CALCULATION_CHANGED'&&draft?.kind==='exercise'&&attempted.body.exerciseTypeId===legacyExerciseId(draft)&&attempted.body.recordDate===draft.recordDate&&attempted.body.durationMinutes===Number(exerciseDurationInput(draft)))
  const responsePreview=calculationChanged&&change.status==='rejected'?change.changedPreview:undefined,queriedEstimate=exercise.ready?exercise.data:undefined
  const held=(change.status==='saving'||change.status==='unknown')&&change.intent.operation==='UPDATE'&&change.intent.kind==='exercise'?change.intent.reviewedEstimate:undefined
  const estimate=pending?held:responsePreview&&(!queriedEstimate||queriedEstimate.calculationFingerprint===attempted?.body.calculationFingerprint)?responsePreview:queriedEstimate
  useEffect(()=>{mounted.current=true;return ()=>{mounted.current=false}},[])
  function current(){const now=session.actions.getSnapshot();return mounted.current&&now.status==='authenticated'&&now.user.id===owner&&now.epoch===epoch}
  if(change.status==='saved'&&(change.result==='UPDATED'||change.result==='MATCHES'))return <Page title={change.result==='MATCHES'?'Current record matches your changes.':'Your changes are saved.'} eyebrow="SAVED RECORD" narrow>{change.result==='MATCHES'&&<Alert severity="info">Current saved state matches the reviewed changes. This does not prove which request changed it.</Alert>}<p>{change.record.recordDate} · {sourceLabels[change.record.source]}. The existing record was retained.</p><div className="actions"><Button component={Link} to={savedRecordLocation(change.intent.kind,change.record.id,change.record.recordDate)}>View changed record</Button><Button component={Link} to="/dashboard">Dashboard</Button></div></Page>
  if(!draft?.edit||!original)return <Page title="Review your changes." narrow><ErrorBox message="Load the current saved record before reviewing a correction." /><Button component={Link} to="/history">History</Button></Page>
  const row=draft.kind==='food'?standaloneFoods(draft)[0]:null,originalFood='nutrients' in original?original:undefined,preserved=draft.kind==='food'&&!!originalFood&&!foodCalculationChanged(draft)
  const errors=validateDraft(draft,owner,todayInZone()),blockedConflict=change.status==='rejected'&&['VERSION_CONFLICT','NOT_FOUND'].includes(change.error.code)
  const ready=dirty&&!pending&&!blockedConflict&&errors.length===0&&(!calculationChanged||!!estimate&&acknowledgedFingerprint===estimate.calculationFingerprint)&&(draft.kind==='food'?preserved||!!row&&previews[row.rowKey]?.signature===foodInputSignature(row,scope)&&!!previews[row.rowKey]?.preview:!!estimate)
  const observed=change.status==='rejected'?change.observedRecord:null,newest=observed&&(!latest||observed.version>=latest.version)?observed:latest
  async function confirm(){setLocalError('');try{if(!ready||!draft)return;setAcknowledgedFingerprint(null);await mutation.update(prepareCorrection(draft,owner,estimate),draft.revision)}catch(error){if(current())setLocalError(error instanceof Error?error.message:'Review the current changes before saving.')}}
  async function readLatest(){if(!current()||checking||pending||!draft?.edit)return;setChecking(true);setLocalError('');try{const record=draft.kind==='food'?await fetchFoodRecord(draft.edit.recordId):await fetchExerciseRecord(draft.edit.recordId);if(current())setLatest(record)}catch(error){if(current())setLocalError(error instanceof Error?error.message:'The latest saved record could not be loaded.')}finally{if(current())setChecking(false)}}
  function reload(){if(!current()||pending||!newest||!draft)return;flushSync(()=>{dispatch({type:'DISCARD',ownerId:owner});dispatch({type:'LOAD_EDIT',ownerId:owner,draft:loadEditDraft(owner,draft.kind,newest)})});setLatest(null);setReplace(false);setLocalError('')}
  return <Page title="Review your changes." eyebrow="UPDATE A SAVED RECORD" subtitle="Confirm changes to this existing record after checking its date and quantities." narrow>
    <ErrorBox message={localError||(change.status==='rejected'?'':mutation.message)} />{change.status==='rejected'&&<ErrorBox message={change.error.message} />}
    {calculationChanged&&<><ChangedExerciseBasis previous={attempted?.reviewedEstimate} current={estimate} unit={profile?.energyUnit??'KJ'} /><FormControlLabel control={<Checkbox checked={!!estimate&&acknowledgedFingerprint===estimate.calculationFingerprint} disabled={!estimate||!!pending} onChange={event=>setAcknowledgedFingerprint(event.target.checked&&estimate?estimate.calculationFingerprint:null)} />} label="I reviewed the changed exercise estimate." /></>}
    <p>Reviewed version: {draft.edit.expectedVersion}</p><p>Source retained: {sourceLabels[draft.source]}</p>
    <section className="card form-section"><h2>Reviewed saved snapshot</h2><p>{'nutrients' in original?original.foodName:original.exerciseName} · {original.recordDate}</p>{'nutrients' in original?<p>{original.quantityGrams} g · {mealLabels[original.mealType]}</p>:<p>{original.durationMinutes} minutes · {displayEnergy(original.estimatedKj,profile?.energyUnit??'KJ')}</p>}</section>
    <div className="stack"><h2>Proposed changes</h2><p>Proposed date: {draft.recordDate}</p>{draft.kind==='food'&&row?<><p>Proposed portion: {row.quantityGramsInput} g</p><ReviewFoodRow row={row} index={0} scope={scope} unit={profile?.energyUnit??'KJ'} onPreview={report} original={originalFood} recordId={draft.edit.recordId} /></>:<section className="card form-section"><p>{draft.kind==='exercise'?exerciseDurationInput(draft):''} minutes</p>{exercise.isFetching&&<p role="status">Checking the current dated estimate…</p>}{exercise.isError&&<ErrorBox message={exercise.error instanceof Error?exercise.error.message:'Current estimate could not be loaded.'} />}{estimate&&<><h3>{estimate.exerciseType.displayName}</h3><strong>{displayEnergy(estimate.estimatedKj,profile?.energyUnit??'KJ')}</strong><p>{estimate.estimateStatus==='AVAILABLE'?`Current basis: ${estimate.weightKgUsed} kg recorded on ${estimate.weightDateUsed}; MET ${estimate.metUsed}.`:estimate.unavailableReason}</p><SourceNote dataset={estimate.exerciseType.dataset} /></>}{!dirty&&!('nutrients' in original)&&<p>Unchanged activity inputs retain the saved estimate.</p>}</section>}</div>
    {blockedConflict&&<Alert severity="warning">The reviewed version cannot be overwritten. Read and compare the latest saved record before starting again.</Alert>}
    {(blockedConflict||newest)&&<section className="card form-section"><Button disabled={checking||!!pending} onClick={()=>void readLatest()}>Read latest saved record</Button>{newest&&<><p>Latest saved version: {newest.version}</p><p>{newest.recordDate} · {'nutrients' in newest?`${newest.quantityGrams} g · ${mealLabels[newest.mealType]}`:`${newest.durationMinutes} minutes`}</p><Button disabled={!!pending} onClick={()=>setReplace(true)}>Start from latest saved record</Button></>}</section>}
    {pending?<p role="status">Resolve the pending record change using the confirmation controls above. The reviewed input and version are frozen.</p>:<div className="actions"><Button onClick={()=>setCancel(true)}>Cancel correction</Button><Button startIcon={<EditOutlined />} onClick={()=>navigate('/entry')}>Edit details</Button><LoadingButton busy={mutation.busy} disabled={!ready} variant="contained" startIcon={<Check />} onClick={()=>void confirm()}>Confirm changes</LoadingButton></div>}
    {!dirty&&<p role="status">No changes to confirm. Edit the date or details to make a correction.</p>}
    <ConfirmDialog open={cancel} title="Discard this unsubmitted correction?" label="Discard correction" onCancel={()=>setCancel(false)} onConfirm={()=>{const destination=draft.returnTo;flushSync(()=>dispatch({type:'DISCARD',ownerId:owner}));navigate(destination)}}>The saved record will remain unchanged.</ConfirmDialog>
    <ConfirmDialog open={replace} title="Replace this correction with the latest saved record?" label="Use latest saved record" onCancel={()=>setReplace(false)} onConfirm={reload}>Your current unsubmitted changes will be discarded. Review and edit the newly loaded snapshot before confirming another change.</ConfirmDialog>
  </Page>
}
