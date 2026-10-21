import {describe,expect,it,vi,beforeEach} from 'vitest'
import {apiGet} from '../../src/api/client'
import {fetchDailySummary,fetchTodaySummary,fetchWeeklySummary,fetchTrends} from '../../src/api/summary'
import {periodSummary} from '../fixtures/summary'
import examples from '../../../docs/api/examples/read-responses.json'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
const metric={knownSum:null,recordCount:0,knownValueCount:0,missingValueCount:0,legacyValueCount:0,hasData:false,partial:false}
const day={recordDate:'2020-01-02',intake:metric,protein:metric,fat:metric,carbs:metric,expenditure:metric,activityMinutes:null,foodCount:0,exerciseCount:0,foodEntryCount:0,exerciseEntryCount:0}
beforeEach(()=>vi.resetAllMocks())
describe('B13 canonical recorded summary API boundary',()=>{
 it('rejects missing, duplicated or mismatched calendar points before charting',async()=>{const base=periodSummary('2020-01-01','2020-01-03');for(const value of [{...base,days:base.days.slice(1)},{...base,days:[base.days[0],base.days[0],base.days[2]]},{...base,calendarDayCount:2}]){vi.mocked(apiGet).mockResolvedValue(value);await expect(fetchTrends(base.rangeStart,base.rangeEnd)).rejects.toThrow()}})

 it('passes explicit business dates/zone/signal and preserves null versus real zero',async()=>{
  const known={...metric,knownSum:0,recordCount:1,knownValueCount:1,hasData:true};vi.mocked(apiGet).mockResolvedValue({day:{...day,intake:known},goalComparison:null,facts:[]});const abort=new AbortController();const result=await fetchDailySummary('2020-01-02',abort.signal);expect(result.day.intake.knownSum).toBe(0);expect(result.day.protein.knownSum).toBeNull();expect(apiGet).toHaveBeenCalledExactlyOnceWith('/summary/daily',{params:{date:'2020-01-02',zone:'Australia/Sydney'},signal:abort.signal})
 })
 it('never supplies a default empty summary for malformed or rejected reads',async()=>{
  vi.mocked(apiGet).mockResolvedValue({});await expect(fetchTodaySummary()).rejects.toThrow('Unexpected server response');vi.mocked(apiGet).mockRejectedValue(new Error('Read unavailable'));await expect(fetchDailySummary('2020-01-02')).rejects.toThrow('Read unavailable')
 })
 it('refuses a different business date or period than the selected overview',async()=>{
  vi.mocked(apiGet).mockResolvedValue({day,goalComparison:null,facts:[]});await expect(fetchDailySummary('2020-01-03')).rejects.toThrow('Unexpected server response')
  const period=examples.find(x=>x.schema==='PeriodSummary')?.value;vi.mocked(apiGet).mockResolvedValue(period);await expect(fetchTrends('2020-02-01','2020-02-10')).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockResolvedValue(examples.find(x=>x.schema==='WeeklySummary')?.value);await expect(fetchWeeklySummary('2020-01-08')).rejects.toThrow('Unexpected server response')
 })
 it('uses complete range and actual weekly end parameters without a second frontend aggregation',async()=>{
  const periodExample=examples.find(x=>x.schema==='PeriodSummary')?.value
  if(!periodExample)throw new Error('Missing canonical period fixture')
  vi.mocked(apiGet).mockResolvedValue(periodExample);await fetchTrends('2020-01-01','2020-01-10');expect(apiGet).toHaveBeenCalledWith('/summary/trends',{params:{from:'2020-01-01',to:'2020-01-10',zone:'Australia/Sydney'},signal:undefined});vi.mocked(apiGet).mockResolvedValue(examples.find(x=>x.schema==='WeeklySummary')?.value);await fetchWeeklySummary('2020-01-07');expect(apiGet).toHaveBeenCalledWith('/summary/weekly',{params:{end:'2020-01-07',zone:'Australia/Sydney'},signal:undefined})
 })
})
