import {useQuery} from '@tanstack/react-query'
import {Button} from '@mui/material'
import {fetchWeeklySummary} from '../../api/summary'
import {useSession} from '../../hooks/useSession'
import {useDaySummary,usePeriodSummary} from '../../hooks/useSummary'
import type {HistorySearch} from './historySearch'
import {ErrorBox} from '../../components/presentation'
import {MetricCard} from '../../components/MetricCard'
import {RuleSummary} from '../../components/RuleSummary'
import {queryKeys} from '../../app/queryKeys'
import {PeriodTrends} from './PeriodTrends'
function Period({from,to}:{from:string;to:string}){
  const query=usePeriodSummary(from,to),{state}=useSession(),unit=state.status==='authenticated'?state.profile?.energyUnit??'KJ':'KJ'
  return <section className="card summary-panel"><h2>Period summary</h2><p>{from} to {to} · Inclusive calendar dates</p>{query.isPending?<p role="status">Loading period summary…</p>:query.isError?<><ErrorBox message="Period summary could not be loaded." /><Button onClick={()=>void query.refetch()}>Retry period summary</Button></>:query.data&&<PeriodTrends summary={query.data} unit={unit} />}</section>
}
function Daily({date}:{date:string}){
  const query=useDaySummary(date),{state}=useSession(),unit=state.status==='authenticated'?state.profile?.energyUnit??'KJ':'KJ'
  return <section className="card summary-panel"><h2>Daily summary</h2><p>{date}</p>{query.isPending?<p role="status">Loading daily summary…</p>:query.isError?<><ErrorBox message="Daily summary could not be loaded." /><Button onClick={()=>void query.refetch()}>Retry daily summary</Button></>:query.data&&<><div className="stats-grid"><MetricCard label="Recorded intake" metric={query.data.day.intake} unit={unit} /><MetricCard label="Saved exercise energy" metric={query.data.day.expenditure} unit={unit} /></div><RuleSummary facts={query.data.facts} /></>}</section>
}
function Weekly({end}:{end:string}){
  const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',version=state.status==='authenticated'?state.profile?.profileVersion??null:null,unit=state.status==='authenticated'?state.profile?.energyUnit??'KJ':'KJ'
  const query=useQuery({queryKey:queryKeys.weeklySummary(owner,state.epoch,end,version),queryFn:({signal})=>fetchWeeklySummary(end,signal),enabled:!!owner,retry:false})
  return <section className="card summary-panel"><h2>Weekly summary</h2>{query.isPending?<p role="status">Loading weekly summary…</p>:query.isError?<><ErrorBox message="Weekly summary could not be loaded." /><Button onClick={()=>void query.refetch()}>Retry weekly summary</Button></>:query.data&&<><section aria-label="Current week"><p>Current range: {query.data.current.rangeStart} to {query.data.current.rangeEnd}</p><PeriodTrends summary={query.data.current} unit={unit} /></section><section aria-label="Previous week"><h3>Previous equal-length range</h3><p>Previous equal-length range: {query.data.previous.rangeStart} to {query.data.previous.rangeEnd}</p><PeriodTrends summary={query.data.previous} unit={unit} charts={false} /></section></>}</section>
}
export function HistorySummary({search}:{search:HistorySearch}){return <><p className="tiny-note">Summaries include all confirmed food and activity records for their stated dates.</p>{search.summary==='daily'?<Daily date={search.summaryDate} />:search.summary==='weekly'?<Weekly end={search.summaryDate} />:<Period from={search.from} to={search.to} />}</>}
