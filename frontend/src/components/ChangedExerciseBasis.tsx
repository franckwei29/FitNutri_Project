import {Alert} from '@mui/material'
import type {EnergyUnit,ExerciseEstimate} from '../api/contracts'
import {displayEnergy} from '../domain/units'
type Basis=Pick<ExerciseEstimate,'estimatedKj'|'weightKgUsed'|'weightDateUsed'|'metUsed'|'estimateStatus'>
export function ChangedExerciseBasis({previous,current,unit}:{previous:Basis|undefined;current:Basis|undefined;unit:EnergyUnit}){
  const text=(basis:Basis)=>`${basis.weightKgUsed===null?'weight unavailable':`${basis.weightKgUsed} kg${basis.weightDateUsed?` on ${basis.weightDateUsed}`:''}`} · ${displayEnergy(basis.estimatedKj,unit)} · ${basis.metUsed===null?'MET unavailable':`MET ${basis.metUsed}`}`
  return <Alert severity="warning"><p>The calculation basis changed. The rejected request did not save these changes. Review the current basis before confirming again.</p><p>{previous?`Previously reviewed basis: ${text(previous)}`:'The previous reviewed basis is unavailable.'}</p><p>{current?`Current server basis: ${text(current)}`:'Load the current estimate before confirming again.'}</p></Alert>
}
