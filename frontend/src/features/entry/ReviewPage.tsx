import {useSaveNavigation} from '../feedback/useSaveNavigation'
import {RootCorrectionReviewPage} from './RootCorrectionReviewPage'
import {ExerciseCreateReviewPage} from './exercise/ExerciseCreateReviewPage'
import {FoodCreateReviewPage} from './food/FoodCreateReviewPage'
import {standaloneFoods,usesExerciseV2,legacyExerciseId,exerciseDurationInput} from './draftSelectors'
import {useCallback,useState} from 'react'
import {flushSync} from 'react-dom'
import {Link,useNavigate} from 'react-router-dom'
import {Alert,Button,Checkbox,FormControlLabel} from '@mui/material'
import {Check,EditOutlined} from '@mui/icons-material'
import {Page,ConfirmDialog,ErrorBox,LoadingButton} from '../../components/presentation'
import {useDraft} from '../../hooks/useDraft'
import {useSession} from '../../hooks/useSession'
import {useCapabilities} from '../../hooks/useCapabilities'
import {useSubmitDraft} from '../../hooks/useSubmitDraft'
import {useExerciseEstimate} from '../../hooks/useExerciseEstimate'
import {ReviewFoodRow} from './ReviewFoodRow'
import {SubmissionRecovery} from './SubmissionRecovery'
import {prepareSubmission} from './submissionPayload'
import {validateDraft} from './draftValidation'
import {foodInputSignature} from './previewIdentity'
import type {RowPreview} from './FoodItemEditor'
import {todayInZone} from '../../domain/dates'
import {displayEnergy} from '../../domain/units'
import {sourceLabels} from '../../domain/labels'
import {SourceNote} from '../../components/SourceNote'
import {savedRecordLocation} from '../history/historySearch'
import {CorrectionReviewPage} from './CorrectionReviewPage'
import {ChangedExerciseBasis} from '../../components/ChangedExerciseBasis'
export function ReviewPage(){
  useSaveNavigation()
  const {state}=useDraft()
  if(state.draft?.edit?.root||state.recordMutation.apiVersion===2&&state.recordMutation.intent.operation==='UPDATE')return <RootCorrectionReviewPage />
  const correction=!!state.draft?.edit||(state.recordMutation.status!=='idle'&&state.recordMutation.intent.operation==='UPDATE')
  const pending=state.submission.status==='saving'||state.submission.status==='unknown'?state.submission:null,legacyPending=!!pending&&pending.apiVersion!==2
  if(!correction&&!legacyPending&&state.draft?.kind==='exercise'&&(usesExerciseV2(state.draft)||pending?.apiVersion===2))return <ExerciseCreateReviewPage />
  return correction?<CorrectionReviewPage />:!legacyPending&&state.draft?.kind==='food'?<FoodCreateReviewPage />:<CreateReviewPage />
}
function CreateReviewPage(){
  const {state,dispatch}=useDraft(),session=useSession(),capabilities=useCapabilities(),submit=useSubmitDraft(),navigate=useNavigate()
  const [cancel,setCancel]=useState(false),[localError,setLocalError]=useState(''),[previews,setPreviews]=useState<Record<string,RowPreview>>({}),[acknowledgedFingerprint,setAcknowledgedFingerprint]=useState<string|null>(null)
  const report=useCallback((key:string,value:RowPreview)=>setPreviews(old=>old[key]?.signature===value.signature&&old[key]?.preview===value.preview?old:{...old,[key]:value}),[])
  const owner=session.state.status==='authenticated'?session.state.user.id:'',profile=session.state.status==='authenticated'?session.state.profile:null
  const scope={ownerId:owner,epoch:session.state.epoch,catalogueRevision:capabilities.data?.catalogueRevision??'unavailable'},draft=state.draft,submission=state.submission,frozen=submission.status==='saving'||submission.status==='unknown'
  const exercise=useExerciseEstimate(draft?.kind==='exercise'&&!frozen?legacyExerciseId(draft):null,draft?.kind==='exercise'?exerciseDurationInput(draft):'',draft?.recordDate??'',scope,profile?.profileVersion??null)
  if(submission.status==='saved')return <Page title="Your record is saved." eyebrow="CONFIRMED" narrow><Alert severity="success">{submission.receipt.records.length} confirmed records saved for {submission.receipt.records[0]?.recordDate}. Your submission is complete.</Alert>{submission.refreshFailed&&<><Alert severity="warning">Saved successfully. Some refreshed views could not be loaded.</Alert><Button onClick={()=>void submit.retryRefresh()}>Retry refreshed views</Button></>}<div className="actions"><Button component={Link} to="/dashboard">Dashboard</Button><Button component={Link} to={savedRecordLocation(submission.receipt.kind==='FOOD_BATCH'?'food':'exercise',submission.receipt.records[0].id,submission.receipt.records[0].recordDate)}>View saved date</Button><Button component={Link} to="/add" variant="contained">Add another record</Button></div></Page>
  if(!draft)return <Page title="Review your record." narrow><p>Start an entry to review its details before saving.</p><Button component={Link} to="/add">Add a record</Button></Page>
  const errors=validateDraft(draft,owner,todayInZone())
  const previous=submission.status==='rejected'?submission.reviewedEstimate:undefined
  const calculationChanged=!!(submission.status==='rejected'&&submission.error.code==='CALCULATION_CHANGED'&&draft.kind==='exercise'&&previous?.exerciseType.id===legacyExerciseId(draft)&&previous.recordDate===draft.recordDate&&previous.durationMinutes===Number(exerciseDurationInput(draft)))
  const responsePreview=calculationChanged&&submission.status==='rejected'?submission.changedPreview:undefined,queried=exercise.ready?exercise.data:undefined,estimate=(submission.status==='saving'||submission.status==='unknown')&&draft.kind==='exercise'&&submission.apiVersion!==2?submission.reviewedEstimate:responsePreview&&(!queried||queried.calculationFingerprint===previous?.calculationFingerprint)?responsePreview:queried
  const ready=!draft.edit&&!capabilities.isError&&!!capabilities.data?.catalogues[draft.kind].ready&&!!capabilities.data.catalogues[draft.kind].verified&&errors.length===0&&(!calculationChanged||!!estimate&&acknowledgedFingerprint===estimate.calculationFingerprint)&&(draft.kind==='food'?standaloneFoods(draft).every(row=>previews[row.rowKey]?.signature===foodInputSignature(row,scope)&&!!previews[row.rowKey]?.preview):!!estimate)
  async function confirm(){setLocalError('');try{if(!ready||frozen)return;setAcknowledgedFingerprint(null);await submit.confirm(prepareSubmission(draft!,owner,estimate),draft!.revision,draft!.kind==='exercise'?estimate:undefined)}catch(error){setLocalError(error instanceof Error?error.message:'Review the current details before saving.')}}
  return <Page title="Review your record." eyebrow="ONE LAST LOOK" subtitle="Check the references, date and quantities. Nothing is recorded until you confirm." narrow>
    <ErrorBox message={localError||(submission.status==='rejected'?'':submit.message)} />{submission.status==='rejected'&&<Alert severity="error">{submission.error.message}{submission.error.code==='CALCULATION_CHANGED'?' The activity basis changed. Review the newly loaded values before confirming again.':''}</Alert>}
    {calculationChanged&&<><ChangedExerciseBasis previous={previous} current={estimate} unit={profile?.energyUnit??'KJ'} /><FormControlLabel control={<Checkbox checked={!!estimate&&acknowledgedFingerprint===estimate.calculationFingerprint} disabled={!estimate||frozen} onChange={event=>setAcknowledgedFingerprint(event.target.checked&&estimate?estimate.calculationFingerprint:null)} />} label="I reviewed the changed exercise estimate." /></>}
    {draft.source==='VOICE'&&draft.voiceInputStale&&<Alert severity="warning">Voice input changed. Re-extract it or explicitly keep the current entry details before saving. Use Edit details to continue.</Alert>}
    {draft.edit&&<Alert severity="info">Open the record correction flow to review changes to this existing record.</Alert>}
    <p>{draft.recordDate} · {sourceLabels[draft.source]} · {draft.kind==='food'?'Food':'Exercise'}</p>
    {capabilities.isPending&&<p role="status">Checking available catalogues…</p>}{capabilities.isError&&<><ErrorBox message="Catalogue availability could not be checked." /><Button onClick={()=>void capabilities.refetch()}>Retry catalogue check</Button></>}
    <div className="stack">{draft.kind==='food'?standaloneFoods(draft).map((row,index)=><ReviewFoodRow key={row.rowKey} row={row} index={index} scope={scope} unit={profile?.energyUnit??'KJ'} onPreview={report} />):<section className="card form-section"><h2>{estimate?.exerciseType.displayName??'Activity details'}</h2><p>{exerciseDurationInput(draft)} minutes · {draft.recordDate}</p>{exercise.isFetching&&<p role="status">Checking the dated activity basis…</p>}{exercise.isError&&<ErrorBox message={exercise.error instanceof Error?exercise.error.message:'Activity estimate could not be checked.'} />}{estimate&&<><strong>{displayEnergy(estimate.estimatedKj,profile?.energyUnit??'KJ')}</strong><p>{estimate.estimateStatus==='AVAILABLE'?`Based on ${estimate.weightKgUsed} kg recorded on ${estimate.weightDateUsed}; MET ${estimate.metUsed}.`:estimate.unavailableReason}</p><p className="tiny-note">{estimate.exerciseType.sourceDescription} · Code {estimate.exerciseType.sourceActivityCode}. General gross estimate for this activity period.</p><SourceNote dataset={estimate.exerciseType.dataset} /></>}</section>}</div>
    {submission.status==='unknown'?<SubmissionRecovery submissionId={submission.submissionId} busy={submit.busy} onCheck={()=>void submit.recover()} onRetry={()=>void submit.retry()} />:<div className="actions"><Button disabled={frozen||submit.busy} onClick={()=>setCancel(true)}>Cancel entry</Button><Button disabled={frozen||submit.busy} startIcon={<EditOutlined />} onClick={()=>navigate('/entry')}>Edit details</Button><LoadingButton busy={frozen||submit.busy} disabled={!ready} variant="contained" startIcon={<Check />} onClick={()=>void confirm()}>Confirm &amp; save</LoadingButton></div>}
    <ConfirmDialog open={cancel} title="Discard this unsubmitted entry?" label="Discard entry" onCancel={()=>setCancel(false)} onConfirm={()=>{const returnTo=draft.returnTo;flushSync(()=>dispatch({type:'DISCARD',ownerId:owner}));navigate(returnTo)}}>No food or activity records will be created.</ConfirmDialog>
  </Page>
}
