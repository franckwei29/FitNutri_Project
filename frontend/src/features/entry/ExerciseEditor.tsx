import {legacyExerciseId,exerciseDurationInput} from './draftSelectors'
import {useQuery} from '@tanstack/react-query'
import {Alert,Autocomplete,Button,LinearProgress,TextField} from '@mui/material'
import {ArrowForward,DirectionsRun} from '@mui/icons-material'
import type {EnergyUnit,ExerciseEstimate,ExerciseType} from '../../api/contracts'
import type {DraftAction,ExerciseDraft} from './draftTypes'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import {loadExerciseTypes} from '../../api/exerciseTypes'
import {useExerciseEstimate} from '../../hooks/useExerciseEstimate'
import {DateField} from '../../components/presentation'
import {SourceNote} from '../../components/SourceNote'
import {parseQuantity} from '../../domain/inputRules'
import {isValidLocalDate,todayInZone} from '../../domain/dates'
import {displayEnergy} from '../../domain/units'
import {isCorrectionDirty} from './correctionIntent'
export function ExerciseEditor({draft,scope,dispatch,unit,profileVersion,disabled=false,onReview}:{draft:ExerciseDraft;scope:CatalogueScope;dispatch:(action:DraftAction)=>unknown;unit:EnergyUnit;profileVersion:number|null;disabled?:boolean;onReview:(estimate:ExerciseEstimate)=>void}){
  const types=useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'exercise-types',scope.catalogueRevision],queryFn:({signal})=>loadExerciseTypes(signal),retry:false})
  const original=draft.edit?.original,originalExercise=original&&!('nutrients' in original)?original:undefined,preserved=!!originalExercise&&!isCorrectionDirty(draft)
  const result=useExerciseEstimate(preserved?null:legacyExerciseId(draft),exerciseDurationInput(draft),draft.recordDate,scope,profileVersion,draft.edit?.recordId)
  const estimate=result.ready?result.data:undefined,selected=types.data?.find(x=>x.id===legacyExerciseId(draft))??(estimate?.exerciseType.id===legacyExerciseId(draft)?estimate.exerciseType:originalExercise?.exerciseType?.id===legacyExerciseId(draft)?originalExercise.exerciseType:null)
  const basis=estimate??(preserved?originalExercise:undefined),ownOriginal=!!legacyExerciseId(draft)&&originalExercise?.exerciseType?.id===legacyExerciseId(draft)
  const reason=estimate?.unavailableReason??(basis?.estimateStatus==='MISSING_WEIGHT'?'No weight was recorded on or before this activity date.':basis?.estimateStatus==='NO_APPLICABLE_REFERENCE'?'No applicable activity reference was available.':'This legacy estimate could not be fully verified.')
  const quantity=parseQuantity(exerciseDurationInput(draft),'minutes'),today=todayInZone(),validDate=isValidLocalDate(draft.recordDate)&&draft.recordDate<=today
  return <div className="stack">
    <fieldset disabled={disabled} style={{border:0,padding:0,margin:0}}><DateField today={today} value={draft.recordDate} onChange={value=>dispatch({type:'CHANGE_DATE',ownerId:scope.ownerId,value})} error={validDate?undefined:'Enter a valid date that is not in the future.'} /></fieldset>
    {types.isError&&<><Alert severity="error">Activity choices could not be loaded.</Alert><Button onClick={()=>void types.refetch()} disabled={disabled}>Retry activity choices</Button></>}
    {draft.suggestion&&<section aria-label="Voice activity suggestion"><p>Voice suggestion: {draft.suggestion.proposedName??'Activity type missing'}</p>{draft.suggestion.warnings.map((text,i)=><p className="tiny-note" key={i}>{text}</p>)}<div className="actions">{draft.suggestion.typeMatches.map((type,i)=><Button key={type.id} aria-label={`Choose suggested activity ${i+1}`} disabled={disabled||!types.data?.some(current=>current.id===type.id&&current.active)} onClick={()=>dispatch({type:'SET_EXERCISE_TYPE',ownerId:scope.ownerId,value:type.id})}>{type.displayName} · {type.sourceActivityCode}</Button>)}</div><p className="tiny-note">Choose the actual activity and minutes. The dated estimate below uses the server reference and weight history.</p></section>}
    <Autocomplete<ExerciseType> options={types.data??[]} value={selected} disabled={disabled||types.isPending||types.isError} loading={types.isPending} getOptionLabel={item=>item.displayName} getOptionKey={item=>item.id} isOptionEqualToValue={(a,b)=>a.id===b.id} onChange={(_,item)=>dispatch({type:'SET_EXERCISE_TYPE',ownerId:scope.ownerId,value:item?.id??null})}
      renderOption={(props,item)=>{const {key,...rest}=props;return <li key={key} {...rest}><span>{item.displayName}<small style={{display:'block'}}>{item.sourceDescription} · {item.sourceActivityCode}</small></span></li>}}
      renderInput={params=><TextField {...params} label="Exercise type" helperText="Choose the activity and conditions that match your session." />} />
    <TextField label="Duration (minutes)" value={exerciseDurationInput(draft)} disabled={disabled} onChange={e=>dispatch({type:'SET_DURATION',ownerId:scope.ownerId,value:e.target.value})} error={!!exerciseDurationInput(draft)&&!quantity.ok} helperText={exerciseDurationInput(draft)&&!quantity.ok?quantity.message:'Decimal minutes are supported, up to 1,440 minutes.'} slotProps={{htmlInput:{inputMode:'decimal'}}} />
    {result.isFetching&&<LinearProgress aria-label="Loading dated exercise estimate" />}
    {result.isError&&<><Alert severity="error">{result.error instanceof Error?result.error.message:'Exercise estimate unavailable.'}</Alert><Button disabled={disabled||!quantity.ok||!validDate||!legacyExerciseId(draft)} onClick={()=>void result.refetch()}>Retry exercise estimate</Button></>}
    <div className="estimate-panel"><DirectionsRun /><span>{preserved?'Saved energy expenditure':'Estimated energy expenditure'}</span><strong>{displayEnergy(basis?.estimatedKj??null,unit)}</strong>
      {basis?.estimateStatus==='AVAILABLE'?<p>Based on {basis.durationMinutes} min and {basis.weightKgUsed} kg recorded on {basis.weightDateUsed}. MET {basis.metUsed}; formula {basis.formulaVersion}.</p>:basis?<><p>{reason}</p><p>You can still review and save the activity without an energy estimate.</p></>:<p>Choose an activity, date and positive duration to load its estimate.</p>}
    </div>
    {preserved&&<p className="tiny-note">Unchanged activity inputs retain the saved estimate.</p>}
    {selected&&<><p className="tiny-note">{selected.sourceDescription} · Official activity code {selected.sourceActivityCode}. Reference ages {selected.dataset.minAge}–{selected.dataset.maxAge??'above'}; applicability uses approximate age from year of birth. A general gross estimate for the activity period.</p><SourceNote dataset={selected.dataset} /></>}
    <div className="actions"><Button variant="contained" endIcon={<ArrowForward />} disabled={disabled||!estimate||(types.isError&&!ownOriginal)||(!!draft.edit&&!isCorrectionDirty(draft))} onClick={()=>{if(estimate)onReview(estimate)}}>{draft.edit?'Review exercise changes':'Review exercise entry'}</Button></div>
  </div>
}
