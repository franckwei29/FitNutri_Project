// Three Demo account.tsx sections retain the original copy, classes and controls.
import { Alert,Button,Checkbox,FormControlLabel,TextField } from '@mui/material'
import { Check,CheckCircleOutlined,LockOutlined } from '@mui/icons-material'
import { DateField,SelectField } from '../../components/presentation'
import { safetyLabels } from '../../domain/labels'
import type { ProfileFormState } from './profileForm'
import { changeEnergyUnit,editEnergyTarget } from './profileForm'
const sexNames={MALE:'Male',FEMALE:'Female',OTHER:'Other',PREFER_NOT_TO_SAY:'Prefer not to say'}
const activityNames={SEDENTARY:'Sedentary',LIGHT:'Lightly active',MODERATE:'Moderately active',HIGH:'Highly active'}
const goalNames={LOSE_FAT:'Lose fat',GAIN_MUSCLE:'Gain muscle',MAINTAIN:'Maintain',NOT_SURE:'Not sure yet'}
export function ProfileSections({form,setForm,errors,step,onboarding,today}:{form:ProfileFormState;setForm:(value:ProfileFormState)=>void;errors:Record<string,string>;step:number;onboarding:boolean;today:string}) {
  const patch=(value:Partial<ProfileFormState>)=>setForm({...form,...value})
  const eligible=form.healthSafetyAnswer==='NO'&&form.qaConsentGranted
  const about=<section className="card form-section"><p className="eyebrow">ABOUT YOU</p><h2>{onboarding?'Start with the basics':'Personal information'}</h2><div className="stack">
    <TextField label="Display name" value={form.displayName} onChange={e=>patch({displayName:e.target.value})} error={!!errors.displayName} helperText={errors.displayName} />
    <div className={`adult-confirm ${errors.adultConfirmed?'invalid':''}`}><FormControlLabel control={<Checkbox checked={form.adultConfirmed} onChange={e=>patch({adultConfirmed:e.target.checked})} />} label="I confirm I am 18 years of age or older." />{errors.adultConfirmed&&<p className="error-text" role="alert">{errors.adultConfirmed}</p>}</div>
    <div className="form-grid"><TextField label="Year of birth" type="number" value={form.birthYear} onChange={e=>patch({birthYear:e.target.value})} error={!!errors.birthYear} helperText={errors.birthYear} />
      <SelectField label="Sex" value={form.sex} onChange={value=>patch({sex:value as ProfileFormState['sex']})} options={sexNames} error={errors.sex} />
      <TextField label="Height (cm)" type="number" value={form.heightCm} onChange={e=>patch({heightCm:e.target.value})} error={!!errors.heightCm} helperText={errors.heightCm} slotProps={{htmlInput:{step:'0.1'}}} />
    </div>
    {onboarding&&<><hr className="compact-divider" /><h3>Your initial weight</h3><div className="form-grid"><TextField label="Weight (kg)" type="number" value={form.weightInput} onChange={e=>patch({weightInput:e.target.value})} error={!!errors.weightInput} helperText={errors.weightInput} slotProps={{htmlInput:{step:'0.01'}}} /><DateField label="Weight date" today={today} value={form.weightDate} onChange={weightDate=>patch({weightDate})} error={errors.weightDate} /></div></>}
  </div></section>
  const goals=<section className="card form-section"><p className="eyebrow">YOUR EVERYDAY DIRECTION</p><h2>Goals & activity</h2><div className="stack">
    <SelectField label="Activity level" value={form.activityLevel} onChange={value=>patch({activityLevel:value as ProfileFormState['activityLevel']})} options={activityNames} error={errors.activityLevel} />
    <div><p className="field-label">Primary goal</p><div className="choice-grid">{Object.entries(goalNames).map(([value,label])=><button type="button" className={`choice-button ${form.goalType===value?'selected':''}`} aria-pressed={form.goalType===value} key={value} onClick={()=>patch({goalType:value as ProfileFormState['goalType']})}>{label}{form.goalType===value&&<Check fontSize="small" />}</button>)}</div>{form.goalType&&<Button size="small" onClick={()=>patch({goalType:null})}>Remove goal</Button>}</div>
    <SelectField label="Energy display unit" value={form.energyUnit} onChange={value=>{if(value) setForm(changeEnergyUnit(form,value as ProfileFormState['energyUnit']))}} options={{KJ:'Kilojoules (kJ)',KCAL:'Kilocalories (kcal)'}} />
    <TextField label={`Daily intake reference (${form.energyUnit==='KJ'?'kJ':'kcal'}, optional)`} type="number" value={form.targetInput} onChange={e=>setForm(editEnergyTarget(form,e.target.value))} error={!!errors.dailyTargetKj||form.dailyTargetKj===undefined} helperText={errors.dailyTargetKj||'A personal reference, not a prescribed intake. Leave empty to show actual values only.'} />
    {form.targetInput!==''&&<Button size="small" onClick={()=>setForm(editEnergyTarget(form,''))}>Remove energy reference</Button>}
  </div></section>
  const safety=<section className="card form-section"><p className="eyebrow">HEALTH & GENERAL Q&A</p><h2>A thoughtful starting point</h2><p className="section-subtitle">This answer only determines whether general health Q&A is available. Please do not enter medical details.</p><p className="field-label">Do you have any existing health conditions that may affect physical activity or nutrition?</p><div className="stack compact">
    {Object.entries(safetyLabels).map(([value,label])=><button type="button" key={value} className={`choice-button safety-choice ${form.healthSafetyAnswer===value?'selected':''}`} aria-pressed={form.healthSafetyAnswer===value} onClick={()=>patch({healthSafetyAnswer:value as ProfileFormState['healthSafetyAnswer']})}>{label}{form.healthSafetyAnswer===value&&<Check fontSize="small" />}</button>)}
    {errors.healthSafetyAnswer&&<p className="error-text" role="alert">{errors.healthSafetyAnswer}</p>}
    <div className="consent-box"><FormControlLabel sx={{alignItems:'flex-start',m:0,'& .MuiCheckbox-root':{p:'0 8px 0 0',minHeight:24},'& .MuiFormControlLabel-label':{lineHeight:'24px'}}} control={<Checkbox checked={form.qaConsentGranted} onChange={e=>patch({qaConsentGranted:e.target.checked})} />} label="Allow FitNutri to use my confirmed records to answer general health questions." /><p className="tiny-note">You can withdraw this consent at any time. Your raw health-safety answer is not part of the Q&A context.</p></div>
    <Alert severity={eligible?'success':'info'} icon={eligible?<CheckCircleOutlined />:<LockOutlined />}>{eligible?'Your profile permits General Health Q&A. Service availability is checked separately.':'General Health Q&A is unavailable. Food, exercise, history and factual summaries remain available.'}</Alert>
  </div></section>
  return onboarding?<div className="onboarding-content">{step===0?about:step===1?goals:safety}</div>:<div className="two-col profile-columns">{about}<div className="stack">{goals}{safety}</div></div>
}
