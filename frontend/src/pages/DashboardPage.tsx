import {useDeletedRootFocus} from '../features/history/useDeletedRootFocus'
import {rootTarget,typedHighlightKey} from '../features/history/historySearch'
import {useEffect} from 'react'
import {SaveFeedbackHost} from '../features/feedback/SaveFeedbackHost'
import {SavedRecordContext} from '../features/feedback/SavedRecordContext'
import {useSaveFeedback} from '../features/feedback/SaveFeedbackContext'
import {EntryRootActions} from '../features/history/EntryRootActions'
import {EntryRootPagination} from '../features/history/EntryRootPagination'
import {useSearchParams,Link} from 'react-router-dom'
import {Alert,Button} from '@mui/material'
import {Add,DirectionsRun,RestaurantOutlined,SpaOutlined,Today,Refresh,History,FlagOutlined} from '@mui/icons-material'
import {Page,DateField,ErrorBox} from '../components/presentation'
import {useSession} from '../hooks/useSession'
import {useBusinessToday} from '../hooks/useBusinessToday'
import {useDaySummary,usePeriodSummary} from '../hooks/useSummary'
import {useEntryRootQueries} from '../hooks/useRecordQueries'
import {isValidLocalDate,shiftCalendarDate} from '../domain/dates'
import {displayEnergy} from '../domain/units'
import {MetricCard} from '../components/MetricCard'
import {EntryRootList} from '../features/history/EntryRootCard'
import {IntakeChart} from '../components/IntakeChart'
export function DashboardPage(){
  const {store:feedback,value:savedFeedback}=useSaveFeedback()
  const savedKey=savedFeedback?.event.key
  useEffect(()=>{if(savedKey)document.getElementById('main-content')?.focus()},[savedKey])
  const {state}=useSession(),today=useBusinessToday(),[params,setParams]=useSearchParams(),date=params.get('date')??today,valid=isValidLocalDate(date)&&date<=today
  const weekFrom=valid?(date<'1900-01-07'?'1900-01-01':shiftCalendarDate(date,-6)):''
  const summary=useDaySummary(date,valid),records=useEntryRootQueries(date,date,{enabled:valid}),week=usePeriodSummary(weekFrom,date,valid)
  useDeletedRootFocus([...(records.foods.data?.results??[]),...(records.exercises.data?.results??[])].map(root=>typedHighlightKey(rootTarget(root))))
  if(state.status!=='authenticated'||!state.profile)return null
  const profile=state.profile,unit=profile.energyUnit,data=summary.data,goals={LOSE_FAT:'Lose fat',GAIN_MUSCLE:'Gain muscle',MAINTAIN:'Maintain',NOT_SURE:'Your own pace'}
  const mismatch=!!data&&records.foods.isSuccess&&records.exercises.isSuccess&&(data.day.foodEntryCount!==records.foods.data.totalMatches||data.day.exerciseEntryCount!==records.exercises.data.totalMatches)
  const readFailed=summary.isError||records.foods.isError||records.exercises.isError||week.isError,refreshing=summary.isFetching||records.foods.isFetching||records.exercises.isFetching||week.isFetching
  const refresh=()=>{const key=feedback.getSnapshot()?.event.key;void Promise.all([summary.refetch(),records.foods.refetch(),records.exercises.refetch(),week.refetch()]).then(results=>{if(key)feedback.refresh(key,results.some(result=>result.isError))}).catch(()=>{if(key)feedback.refresh(key,true)})}
  return <Page title="A little progress, every day." eyebrow="YOUR DAILY PICTURE" subtitle={`Hello, ${state.user.displayName.split(' ')[0]}. Here’s what you’ve recorded for ${date}.`} actions={<div className="actions dashboard-overview-actions"><DateField label="Overview date" today={today} value={date} onChange={value=>setParams({date:value})} /><Button title="Back to today" variant="outlined" startIcon={<Today />} disabled={date===today} onClick={()=>setParams({})}>Today</Button><Button variant="outlined" startIcon={<Refresh className={refreshing?'action-spinning':undefined} />} aria-busy={refreshing} disabled={!valid||refreshing} onClick={refresh}>Refresh</Button></div>}>
    <SaveFeedbackHost /><SavedRecordContext readFailed={readFailed} refreshing={refreshing} onRetry={refresh} />
    {!valid?<ErrorBox message="Choose a valid date that is not in the future." />:<>
      {summary.isPending&&<p role="status">Loading daily summary…</p>}{summary.isError&&<><ErrorBox message="Daily summary could not be loaded." />{data&&<Alert severity="warning">Previously loaded summary values may be out of date. Refresh to check the latest records.</Alert>}</>}
      {(summary.isFetching||records.foods.isFetching||records.exercises.isFetching)&&<p role="status">Updating this date’s overview…</p>}{mismatch&&<Alert severity="warning">Records changed while this view was loading. Refresh the overview to align the lists and totals.</Alert>}
      {data&&<><div className="stats-grid"><MetricCard label="Energy intake" metric={data.day.intake} unit={unit} /><MetricCard label="Exercise expenditure" metric={data.day.expenditure} unit={unit} /><section className="card stat-card" aria-label="Time in motion"><span className="stat-label">Time in motion</span><strong>{data.day.activityMinutes===null?'Not recorded':`${data.day.activityMinutes} min`}</strong><span className="muted">{data.day.exerciseEntryCount} recorded entries · {data.day.exerciseCount} activity periods · Includes activities without an energy estimate.</span></section><section className="card stat-card goal-card"><span className="stat-label">Your focus</span><SpaOutlined /><strong className="goal-title">{profile.goalType?goals[profile.goalType]:'Your own pace'}</strong><Button component={Link} to="/profile" startIcon={<FlagOutlined />}>Goals</Button></section></div><p className="tiny-note">{data.goalComparison?`Current personal daily intake reference: ${displayEnergy(data.goalComparison.dailyTargetKj,unit)}. A personal reference, not a prescribed intake or a historical target snapshot.`:'Recorded values only. No personal intake reference set.'}</p><div className="stats-grid"><MetricCard label="Protein" metric={data.day.protein} unit={unit} grams /><MetricCard label="Carbs" metric={data.day.carbs} unit={unit} grams /><MetricCard label="Fat" metric={data.day.fat} unit={unit} grams /></div></>}
      <section className="card summary-panel"><p className="eyebrow">UP TO SEVEN CALENDAR DAYS</p><h2>Recorded intake</h2>{week.isPending?<p role="status">Loading recorded trend…</p>:week.isError?<ErrorBox message="Recorded trend could not be loaded." />:week.data&&<IntakeChart days={week.data.days} unit={unit} />}<Button variant="outlined" startIcon={<History />} component={Link} to={`/history?from=${weekFrom}&to=${date}&type=all`}>History</Button></section><section className="card summary-panel"><p className="eyebrow">THE BIG PICTURE</p><h2>This date, at a glance</h2>{data?.facts.map(fact=><p key={fact.code}>{fact.text}</p>)}</section><div className="dashboard-records"><section className="card record-panel"><div className="card-heading"><h2><RestaurantOutlined /> Food log</h2><Button component={Link} to="/add?kind=food" variant="contained" startIcon={<Add />}>Add food</Button></div><p className="record-count">{records.foods.data?`${records.foods.data.totalMatches} saved food entries`:""}</p>{records.foods.isPending?<p role="status">Loading food records…</p>:records.foods.isError&&!records.foods.data?<ErrorBox message="Food records could not be loaded. Refresh to retry." />:<EntryRootList actions={root=><EntryRootActions root={root} />} foods={records.foods.data?.results??[]} exercises={[]} unit={unit} emptyLabel="No foods recorded for this date." />}</section><section className="card record-panel"><div className="card-heading"><h2><DirectionsRun /> Movement</h2><Button component={Link} to="/add?kind=exercise" variant="contained" startIcon={<Add />}>Add exercise</Button></div><p className="record-count">{records.exercises.data?`${records.exercises.data.totalMatches} saved exercise entries`:""}</p>{records.exercises.isPending?<p role="status">Loading activity records…</p>:records.exercises.isError&&!records.exercises.data?<ErrorBox message="Activity records could not be loaded. Refresh to retry." />:<EntryRootList actions={root=><EntryRootActions root={root} />} foods={[]} exercises={records.exercises.data?.results??[]} unit={unit} emptyLabel="No activities recorded for this date." />}</section></div>
      <EntryRootPagination foods={records.foods} exercises={records.exercises} />
    </>}
  </Page>
}
