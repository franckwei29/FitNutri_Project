import {TableChartOutlined,ExpandMore} from '@mui/icons-material'
import type {DayAggregate,EnergyUnit} from '../api/contracts'
import {displayEnergy} from '../domain/units'
import {buildTrendSeries} from '../domain/trendSeries'
import {TimeSeriesChart} from './TimeSeriesChart'
/** Both pages use the same axes, units and disconnected canonical points. */
export function IntakeChart({days,unit}:{days:DayAggregate[];unit:EnergyUnit}) {
 const data=buildTrendSeries(days,unit),label=unit==='KCAL'?'kcal':'kJ'
 return <figure className="intake-trend" style={{margin:0}}>
  {data.some(p=>p.intake!==null)?<TimeSeriesChart data={data} dataKey="intake" unit={label} />:<p className="empty-state">No known intake values in this period.</p>}
  <figcaption className="tiny-note">Recorded intake ({label}). Missing values are gaps, not zero intake.</figcaption>
  <details><summary className="data-table-action"><TableChartOutlined /> Data table <ExpandMore className="disclosure-arrow" /></summary><div className="table-scroll" role="region" aria-label="Intake data table" tabIndex={0}><table aria-label="Recorded intake values"><thead><tr><th scope="col">Date</th><th scope="col">Recorded intake ({label})</th></tr></thead><tbody>{days.map(day=><tr key={day.recordDate}><th scope="row">{day.recordDate}</th><td>{!day.intake.hasData?'Not recorded':displayEnergy(day.intake.knownSum,unit)}{day.intake.partial?' (partial)':''}</td></tr>)}</tbody></table></div></details>
 </figure>
}
