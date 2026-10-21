import { useCallback,useEffect,useRef,useState } from 'react'
import { flushSync } from 'react-dom'
import { Alert,Button,Step,StepLabel,Stepper } from '@mui/material'
import { ArrowBack,ArrowForward } from '@mui/icons-material'
import type { ProfileResponse } from '../../api/contracts'
import { completeOnboarding,fetchProfile,updateSettings } from '../../api/profile'
import { ApiRequestError } from '../../api/errors'
import { ErrorBox,LoadingButton } from '../../components/presentation'
import { focusError,messageOf } from '../../components/formHelpers'
import { safetyLabels } from '../../domain/labels'
import { todayInZone } from '../../domain/dates'
import { useSession } from '../../hooks/useSession'
import { useNavigationSafety } from '../../hooks/useNavigationSafety'
import { ProfileSections } from './ProfileSections'
import { formField,initialiseProfileForm,onboardingPayload,settingsPayload,stepForField,validateProfileForm } from './profileForm'
import type { ProfileFormState } from './profileForm'
interface Recovery {kind:'unknown'|'conflict';latest:ProfileResponse|null}
/** Initial snapshot and versions remain stable across background refetches; only explicit reset/save replaces them. */
export function ProfileSettingsForm({initial,onboarding,onSaved}:{initial:ProfileResponse;onboarding:boolean;onSaved?:(profile:ProfileResponse)=>void}) {
  const {state:session,actions}=useSession(),navigation=useNavigationSafety()
  const [today]=useState(()=>todayInZone()),[baseline,setBaseline]=useState(initial)
  const [form,setForm]=useState(()=>initialiseProfileForm(initial,today)),[step,setStep]=useState(0)
  const [errors,setErrors]=useState<Record<string,string>>({}),[error,setError]=useState(''),[notice,setNotice]=useState('')
  const [busy,setBusy]=useState(false),[checking,setChecking]=useState(false),[recovery,setRecovery]=useState<Recovery|null>(()=>session.status==='authenticated'&&session.profileRecoveryPending?{kind:'unknown',latest:null}:null)
  const lock=useRef(false),epoch=session.epoch
  const dirty=JSON.stringify(form)!==JSON.stringify(initialiseProfileForm(baseline,today))
  const discard=useCallback(()=>{setForm(initialiseProfileForm(baseline,today));setErrors({})},[baseline,today])
  const setNavigation=navigation?.updateForm
  const pending=busy?'saving':recovery?.kind==='unknown'?'unknown':null
  useEffect(()=>{setNavigation?.('settings',{dirty,pending,onDiscard:discard});return ()=>setNavigation?.('settings',null)},[dirty,pending,discard,setNavigation])
  const updateForm=(next:ProfileFormState)=>{if(!lock.current&&!recovery){setForm(next);setNotice('')}}
  function resetTo(saved:ProfileResponse) {setBaseline(saved);setForm(initialiseProfileForm(saved,today));setErrors({});setRecovery(null);setError('')}
  function acceptSaved(saved:ProfileResponse) {
    let accepted=false
    // Clear this form's blocker in the same commit as server-confirmed completion.
    // Otherwise the route guard can treat its own successful redirect as an unsaved navigation.
    flushSync(()=>{accepted=actions.acceptProfile(saved,epoch);if(accepted){resetTo(saved);setBusy(false);setNavigation?.('settings',null)}})
    return accepted
  }
  async function submit(e:React.FormEvent) {
    e.preventDefault();if(lock.current||recovery) return
    const all=validateProfileForm(form,today,onboarding)
    const current=onboarding&&step<2?Object.fromEntries(Object.entries(all).filter(([field])=>stepForField(field)===step)):all
    setErrors(current)
    if(Object.keys(current).length) {if(onboarding) setStep(Math.min(...Object.keys(current).map(stepForField)));focusError();return}
    if(onboarding&&step<2) {setStep(step+1);return}
    lock.current=true;setBusy(true);setError('');setNotice('')
    try {
      const saved=onboarding?await completeOnboarding(onboardingPayload(form,baseline)):await updateSettings(settingsPayload(form,baseline))
      if(acceptSaved(saved)) {setNotice('Your profile is saved.');onSaved?.(saved)}
    } catch(e) {
      setError(messageOf(e))
      if(e instanceof ApiRequestError) {
        const mapped=Object.fromEntries(e.fieldErrors.map(f=>[formField(f.field),f.message]));setErrors(mapped)
        if(onboarding&&Object.keys(mapped).length) setStep(Math.min(...e.fieldErrors.map(f=>stepForField(f.field))))
        if(Object.keys(mapped).length) focusError()
        if(e.outcome==='unknown') {actions.markProfileWriteUnknown(epoch);setRecovery({kind:'unknown',latest:null})}
        else if(e.code==='VERSION_CONFLICT'||e.code==='ONBOARDING_ALREADY_COMPLETED') setRecovery({kind:'conflict',latest:null})
      }
    } finally {lock.current=false;setBusy(false)}
  }
  async function checkSaved() {
    if(checking) return;setChecking(true);setError('')
    try {
      const latest=await fetchProfile()
      if(actions.getSnapshot().epoch!==epoch||latest.user.id!==baseline.user.id) return
      setRecovery(current=>current?{...current,latest}:null)
    } catch(e) {setError(messageOf(e))} finally {setChecking(false)}
  }
  function useLatest() {
    const latest=recovery?.latest;if(!latest||!acceptSaved(latest)) return
    setNotice('Saved settings loaded. Review them before making further changes.');if(onboarding&&latest.onboardingCompleted) onSaved?.(latest)
  }
  return <><ErrorBox message={error} />{notice&&<Alert severity="success" sx={{mb:2}}>{notice}</Alert>}
    {recovery&&<section className="card form-section" aria-label="Check saved settings"><Alert severity="warning">{recovery.kind==='unknown'?'The save result is not confirmed. Check the server before doing anything else.':'Settings changed or setup is already complete. Review the current saved settings.'}</Alert><LoadingButton busy={checking} onClick={()=>void checkSaved()}>Check saved settings</LoadingButton>
      {recovery.latest&&<><dl><dt>Saved display name</dt><dd>{recovery.latest.user.displayName}</dd><dt>Setup complete</dt><dd>{recovery.latest.onboardingCompleted?'Yes':'No'}</dd><dt>Current measured weight</dt><dd>{recovery.latest.currentWeight?`${recovery.latest.currentWeight.weightKg} kg · ${recovery.latest.currentWeight.recordDate}`:'Not recorded'}</dd><dt>Adult confirmation</dt><dd>{recovery.latest.adultConfirmed?'Confirmed':'Not confirmed'}</dd><dt>Year of birth</dt><dd>{recovery.latest.birthYear??'Not answered'}</dd><dt>Sex</dt><dd>{recovery.latest.sex??'Not answered'}</dd><dt>Height</dt><dd>{recovery.latest.heightCm==null?'Not answered':`${recovery.latest.heightCm} cm`}</dd><dt>Activity level</dt><dd>{recovery.latest.activityLevel??'Not answered'}</dd><dt>Goal</dt><dd>{recovery.latest.goalType??'Not set'}</dd><dt>Health-safety answer</dt><dd>{recovery.latest.healthSafetyAnswer?safetyLabels[recovery.latest.healthSafetyAnswer]:'Not answered'}</dd><dt>Q&A consent</dt><dd>{recovery.latest.qaConsentGranted?'Granted':'Not granted'}</dd><dt>Energy reference</dt><dd>{recovery.latest.dailyTargetKj==null?'Not set':`${recovery.latest.dailyTargetKj} kJ`}</dd></dl>
      {(!onboarding||recovery.latest.onboardingCompleted||recovery.kind==='conflict')&&<Button onClick={useLatest}>{onboarding&&recovery.latest.onboardingCompleted?'Use saved profile':'Load latest saved settings'}</Button>}
      {onboarding&&!recovery.latest.onboardingCompleted&&recovery.kind==='unknown'&&<p>Setup is not confirmed yet. Check again; this page will not submit another initial weight.</p>}</>}
    </section>}
    {onboarding&&<Stepper activeStep={step} alternativeLabel className="onboarding-stepper">{['About you','Goals & activity','Health & Q&A'].map(label=><Step key={label}><StepLabel>{label}</StepLabel></Step>)}</Stepper>}
    <form onSubmit={e=>void submit(e)} noValidate><fieldset disabled={busy||!!recovery} className="profile-fieldset"><ProfileSections form={form} setForm={updateForm} errors={errors} step={step} onboarding={onboarding} today={today} />
      <div className="actions">{onboarding?<>{step>0&&<Button startIcon={<ArrowBack />} onClick={()=>setStep(step-1)}>Back</Button>}<span className="step-caption">Step {step+1} of 3</span></>:<Button onClick={discard}>Reset changes</Button>}
        <LoadingButton type="submit" busy={busy} variant="contained" endIcon={<ArrowForward />} disabled={!!recovery}>{onboarding?(step===2?'Complete setup':'Continue'):'Save settings'}</LoadingButton>
      </div></fieldset></form>
    <p className="page-footnote">For general wellbeing only. Updating your profile does not rewrite previously saved nutrition or activity estimates.</p>
  </>
}
