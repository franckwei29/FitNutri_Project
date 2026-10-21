import {useQuery} from '@tanstack/react-query'
import {Alert,Button,Dialog,DialogActions,DialogContent,DialogTitle} from '@mui/material'
import type {Highlight} from '../features/history/historySearch'
import type {FoodRecord,ExerciseRecord} from '../api/contracts'
import {fetchFoodRecord,fetchExerciseRecord} from '../api/records'
import {ApiRequestError} from '../api/errors'
import {useSession} from '../hooks/useSession'
import {sourceLabels,mealLabels} from '../domain/labels'
import {displayEnergy} from '../domain/units'
import {NutrientTiles} from './NutrientTiles'
import {SourceNote} from './SourceNote'
import {RecordDeletionControl} from './RecordDeletionControl'
import {queryKeys} from '../app/queryKeys'
import {RecordEditControl} from './RecordEditControl'
export function RecordDetailsDialog({target,from,to,type,onClose,onLocated}:{target:Highlight|null;from:string;to:string;type:'food'|'exercise'|'all';onClose:()=>void;onLocated:(target:Highlight,date:string)=>void}){
  const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',unit=state.status==='authenticated'?state.profile?.energyUnit??'KJ':'KJ'
  const detail=useQuery<FoodRecord|ExerciseRecord>({queryKey:queryKeys.record(owner,state.epoch,target?.kind??null,target?.id??null),queryFn:async({signal})=>target!.kind==='food'?fetchFoodRecord(target!.id,signal):fetchExerciseRecord(target!.id,signal),enabled:!!owner&&!!target,retry:false}),row=detail.data
  const outside=!!row&&!!target&&(row.recordDate<from||row.recordDate>to||(type!=='all'&&type!==target.kind))
  return <Dialog open={!!target} onClose={onClose} aria-labelledby="saved-record-details-title" fullWidth maxWidth="sm"><DialogTitle id="saved-record-details-title">Saved record details</DialogTitle><DialogContent>
    {detail.isPending&&<p role="status">Loading saved snapshot…</p>}{detail.isError&&<><Alert severity="error">{detail.error instanceof ApiRequestError&&detail.error.status===404?'This record was removed or is not available to this account.':'Saved record details could not be loaded.'}</Alert>{!(detail.error instanceof ApiRequestError&&detail.error.status===404)&&<Button onClick={()=>void detail.refetch()}>Retry record details</Button>}</>}
    {!detail.isError&&row&&<><h2>{'nutrients' in row?row.foodName:row.exerciseName}</h2><p>Record date: {row.recordDate}</p><p>{sourceLabels[row.source]}{row.dataQuality==='LEGACY_UNVERIFIED'?' · Legacy source':' · Confirmed source'}</p>
      {outside&&<Alert severity="info">This record’s current date or type is outside the selected filters. Its saved snapshot is still available here.</Alert>}
      {'nutrients' in row?<><p>{row.quantityGrams} g · {mealLabels[row.mealType]}</p><NutrientTiles values={row.nutrients} unit={unit} />{row.reference?<><SourceNote dataset={row.reference.dataset} />{!row.reference.active&&<p>This original reference is no longer selectable for new entries. Its saved snapshot is retained.</p>}</>:<p>No verified food reference metadata is available for this historical snapshot.</p>}</>:<><p>{row.durationMinutes} minutes</p><strong>{displayEnergy(row.estimatedKj,unit)}</strong><p>{row.estimateStatus==='AVAILABLE'?`Saved basis: ${row.weightKgUsed} kg recorded on ${row.weightDateUsed}; MET ${row.metUsed}.`:row.estimateStatus==='MISSING_WEIGHT'?'No weight was recorded on or before this activity date.':row.estimateStatus==='NO_APPLICABLE_REFERENCE'?'No applicable activity reference was available.':'This legacy estimate could not be fully verified.'}</p>{row.exerciseType?<><p>{row.exerciseType.sourceDescription} · Official code {row.exerciseType.sourceActivityCode}</p><SourceNote dataset={row.exerciseType.dataset} /></>:<p>No verified activity reference metadata is available for this historical snapshot.</p>}</>}
      <p className="tiny-note">Saved at {row.createdAt}. These values are the saved snapshot; opening this view does not recalculate them.</p>
    </>}
  </DialogContent><DialogActions>{row&&target&&<><RecordEditControl key={`edit:${target.kind}:${target.id}`} kind={target.kind} id={target.id} disabled={detail.isError||detail.isFetching} /><RecordDeletionControl key={`${target.kind}:${target.id}`} kind={target.kind} record={row} disabled={detail.isError||detail.isFetching} onLeaveDetails={onClose} onReload={()=>detail.refetch({throwOnError:true})} /></>}{outside&&row&&target&&<Button onClick={()=>onLocated(target,row.recordDate)}>View this record’s current date</Button>}<Button onClick={onClose}>Close details</Button></DialogActions></Dialog>
}
