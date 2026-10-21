import type {EnergyUnit,MetricAggregate} from '../api/contracts'
import {displayEnergy} from '../domain/units'
export function MetricCard({label,metric,unit,grams=false}:{label:string;metric:MetricAggregate;unit:EnergyUnit;grams?:boolean}){
  const value=!metric.hasData?'Not recorded':metric.knownSum===null?'Unavailable':grams?`${new Intl.NumberFormat('en-AU',{maximumFractionDigits:1}).format(metric.knownSum)} g`:displayEnergy(metric.knownSum,unit)
  return <section className="card stat-card" aria-label={label}><span className="stat-label">{label}</span><strong>{value}</strong><span className="muted">{!metric.hasData?'No records for this date.':metric.partial?`Partial: ${metric.knownValueCount} of ${metric.recordCount} values known`:metric.knownSum===null?`${metric.missingValueCount} recorded values unavailable.`:`${metric.knownValueCount} known ${metric.knownValueCount===1?'value':'values'}`}</span>{metric.legacyValueCount>0&&<small>Includes {metric.legacyValueCount} legacy values.</small>}</section>
}
