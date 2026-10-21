import {useQuery} from '@tanstack/react-query'
import {Button} from '@mui/material'
import type {EnergyUnit,PeriodSummary} from '../../api/contracts'
import {fetchWeights} from '../../api/weight'
import {queryKeys} from '../../app/queryKeys'
import {useSession} from '../../hooks/useSession'
import {RecordCharts,WeightChart} from '../../components/RecordCharts'
import {PeriodStatistics} from '../../components/PeriodStatistics'
import {RuleSummary} from '../../components/RuleSummary'
import {ErrorBox} from '../../components/presentation'
function MeasuredWeights({summary}:{summary:PeriodSummary}){
  const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:''
  const query=useQuery({queryKey:[...queryKeys.weights(owner,summary.rangeStart,summary.rangeEnd),state.epoch],queryFn:({signal})=>fetchWeights(summary.rangeStart,summary.rangeEnd,signal),enabled:!!owner,retry:false})
  return query.isPending?<p role="status">Loading measured weights…</p>:query.isError?<><ErrorBox message="Measured weights could not be loaded. Energy records and their saved estimates are unchanged." /><Button onClick={()=>void query.refetch()}>Retry measured weights</Button></>:<WeightChart days={summary.days} weights={query.data} />
}
export function PeriodTrends({summary,unit,charts=true}:{summary:PeriodSummary;unit:EnergyUnit;charts?:boolean}){
  return <><PeriodStatistics summary={summary} unit={unit} />{charts&&<><RecordCharts days={summary.days} unit={unit} /><MeasuredWeights summary={summary} /></>}<RuleSummary facts={summary.facts} /><p className="tiny-note">Reference sources belong to each saved record. Open its details to inspect the dataset and the calculation basis used when it was confirmed.</p></>
}
