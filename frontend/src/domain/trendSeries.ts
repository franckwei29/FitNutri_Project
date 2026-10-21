import type {DayAggregate,EnergyUnit,WeightEntry} from '../api/contracts'
import {energyValue} from './units'
/** Presentation projection only: no aggregation, interpolation, inferred weights or targets. */
export function buildTrendSeries(days:DayAggregate[],unit:EnergyUnit,weights:WeightEntry[]=[]){
  const measured=new Map(weights.map(w=>[w.recordDate,w.weightKg]))
  return days.map(day=>({recordDate:day.recordDate,intake:energyValue(day.intake.knownSum,unit),expenditure:energyValue(day.expenditure.knownSum,unit),weightKg:measured.get(day.recordDate)??null}))
}
