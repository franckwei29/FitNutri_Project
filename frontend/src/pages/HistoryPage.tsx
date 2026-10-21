import {useDeletedRootFocus} from '../features/history/useDeletedRootFocus'
import {rootTarget,typedHighlightKey} from '../features/history/historySearch'
import {useRef} from 'react'
import type {RootHighlight} from '../features/history/historySearch'
import {EntryRootActions} from '../features/history/EntryRootActions'
import {EntryRootPagination} from '../features/history/EntryRootPagination'
import {EntryRootDetailsDialog} from '../features/history/EntryRootDetailsDialog'
import {Link,useSearchParams} from 'react-router-dom'
import {Button} from '@mui/material'
import {Add} from '@mui/icons-material'
import {Page,DateField,ErrorBox,SelectField} from '../components/presentation'
import {EntryRootList} from '../features/history/EntryRootCard'
import {RecordDetailsDialog} from '../components/RecordDetailsDialog'
import {useSession} from '../hooks/useSession'
import {useBusinessToday} from '../hooks/useBusinessToday'
import {useEntryRootQueries} from '../hooks/useRecordQueries'
import {parseHistorySearch} from '../features/history/historySearch'
import {HistorySummary} from '../features/history/HistorySummary'
import {shiftCalendarDate} from '../domain/dates'
export function HistoryPage(){
  const closedRoot=useRef<RootHighlight|null>(null)
  const {state}=useSession(),today=useBusinessToday(),[params,setParams]=useSearchParams(),search=parseHistorySearch(params,today)
  const records=useEntryRootQueries(search.from,search.to,{enabled:search.valid,kind:search.type})
  useDeletedRootFocus([...(records.foods.data?.results??[]),...(records.exercises.data?.results??[])].map(root=>typedHighlightKey(rootTarget(root))),!search.openRoot)
  if(state.status!=='authenticated'||!state.profile)return null
  const foods=search.type!=='exercise'?records.foods.data?.results??[]:[],exercises=search.type!=='food'?records.exercises.data?.results??[]:[],dates=[...new Set([...foods,...exercises].map(r=>r.recordDate))].sort().reverse()
  const pending=(search.type!=='exercise'&&records.foods.isPending)||(search.type!=='food'&&records.exercises.isPending),error=(search.type!=='exercise'&&records.foods.isError&&!records.foods.data)||(search.type!=='food'&&records.exercises.isError&&!records.exercises.data)
  function update(patch:Record<string,string>,clearHighlight=true){const next=new URLSearchParams(params);Object.entries(patch).forEach(([key,value])=>next.set(key,value));if(clearHighlight){next.delete('highlight');next.delete('highlightRoot');next.delete('openRoot');}setParams(next)}
  function closeRootDetails(){closedRoot.current=search.openRoot;const next=new URLSearchParams(params);next.delete('openRoot');setParams(next)}
  function restoreRootFocus(){const target=closedRoot.current,cards=Array.from(document.querySelectorAll<HTMLElement>('[data-record-id]')),card=cards.find(el=>el.getAttribute('data-record-id')===`${target?.kind}:${target?.id}`);(card?.querySelector<HTMLElement>('.record-actions a')??card?.querySelector<HTMLElement>('h3 a')??document.querySelector<HTMLElement>('.history-day h2')??document.getElementById('main-content'))?.focus()}

  const refresh=()=>{if(search.type!=='exercise')void records.foods.refetch();if(search.type!=='food')void records.exercises.refetch()}
  return <Page title="Your history, in perspective." eyebrow="HISTORY & TRENDS" subtitle="Explore your confirmed records, one day at a time." actions={<Button component={Link} to="/add" startIcon={<Add />} variant="contained">Add a record</Button>}>
    <section className="card filter-bar"><SelectField label="Record type" value={search.type} onChange={type=>update({type})} options={{all:'All records',food:'Food',exercise:'Exercise'}} /><DateField label="History from" today={today} value={search.from} onChange={from=>update({from})} /><DateField label="History to" today={today} value={search.to} onChange={to=>update({to})} /><Button onClick={()=>setParams({from:shiftCalendarDate(today,-6),to:today,type:'all'})}>Last 7 days</Button><Button onClick={()=>setParams({from:shiftCalendarDate(today,-29),to:today,type:'all'})}>Last 30 days</Button><Button disabled={!search.valid} onClick={refresh}>Refresh history</Button></section>
    {!search.valid?<><ErrorBox message={search.errors.join(' ')} /><Button onClick={()=>setParams({from:shiftCalendarDate(today,-6),to:today,type:'all'})}>Use last 7 days</Button></>:<>
      <section className="card filter-bar"><SelectField label="Summary view" value={search.summary} onChange={summary=>update({summary},false)} options={{period:'Selected period',daily:'Daily',weekly:'Weekly'}} />{search.summary!=='period'&&<DateField label={search.summary==='daily'?'Summary date':'Week ending'} today={today} value={search.summaryDate} onChange={summaryDate=>update({summaryDate},false)} />}</section>
      <HistorySummary search={search} />
      {pending?<p role="status">Loading history records…</p>:error?<ErrorBox message="History records could not be loaded." />:dates.length?dates.map(date=><section className="card history-day" key={date}><div className="card-heading"><h2 tabIndex={-1}>{date}</h2></div><EntryRootList actions={root=><EntryRootActions root={root} />} foods={foods.filter(r=>r.recordDate===date)} exercises={exercises.filter(r=>r.recordDate===date)} unit={state.profile!.energyUnit} highlights={[...search.rootHighlights,...(search.highlight?[{kind:search.highlight.kind==='food'?'single' as const:'quick' as const,id:search.highlight.id}]:[])]} /></section>):<section className="card empty-state"><p>No confirmed records match these filters.</p></section>}
      <EntryRootPagination foods={records.foods} exercises={records.exercises} />
      <EntryRootDetailsDialog target={search.openRoot} from={search.from} to={search.to} type={search.type} onClose={closeRootDetails} onExited={restoreRootFocus} onLocated={(target,date)=>update({from:date,to:date,type:'all',openRoot:`${target.kind}:${target.id}`},false)} />
      <RecordDetailsDialog target={search.highlight} from={search.from} to={search.to} type={search.type} onClose={()=>{const next=new URLSearchParams(params);next.delete('highlight');setParams(next)}} onLocated={(target,date)=>update({from:date,to:date,type:'all',highlight:`${target.kind}:${target.id}`},false)} />
    </>}
  </Page>
}
