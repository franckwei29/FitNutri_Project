import {fetchFoodEntriesV2} from '../../src/api/foodEntriesV2'
import {fetchExerciseEntriesV2} from '../../src/api/exerciseEntriesV2'
import {rootPage,foodRecordRootFixture} from '../fixtures/entryRootsV2'
import {act,render,screen,waitFor,within} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {HistoryPage} from '../../src/pages/HistoryPage'
import * as summaryApi from '../../src/api/summary'
import * as recordApi from '../../src/api/records'
import {fetchWeights} from '../../src/api/weight'
import {periodSummary,metric,daySummary} from '../fixtures/summary'
import {profile,user} from '../fixtures/profile'
import {savedFood} from '../fixtures/records'
import type {DayAggregate,EnergyUnit,WeightEntry} from '../../src/api/contracts'
vi.mock('../../src/api/foodEntriesV2',()=>({fetchFoodEntriesV2:vi.fn(),fetchFoodEntryV2:vi.fn()}))
vi.mock('../../src/api/exerciseEntriesV2',()=>({fetchExerciseEntriesV2:vi.fn(),fetchExerciseEntryV2:vi.fn()}))
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn(),fetchWeeklySummary:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecords:vi.fn(),fetchExerciseRecords:vi.fn(),fetchFoodRecord:vi.fn(),fetchExerciseRecord:vi.fn()}))
vi.mock('../../src/api/weight',()=>({fetchWeights:vi.fn()}))
// Hook integration tests isolate browser layout; real Recharts props have separate paired component tests.
vi.mock('../../src/components/RecordCharts',()=>({RecordCharts:({days,unit}:{days:DayAggregate[];unit:EnergyUnit})=><p data-testid="canonical-chart">{JSON.stringify({days,unit})}</p>,WeightChart:({weights}:{weights:WeightEntry[]})=><p>{weights.length?weights.map(w=>`${w.recordDate}: ${w.weightKg} kg`).join(', '):'No recorded weight measurements in this period.'}</p>}))
let moved=false
function summary(from:string,to:string){const value=periodSummary(from,to);if(!moved&&from==='2020-01-01'){value.days[0].intake=metric({knownSum:100,hasData:true,recordCount:1,knownValueCount:1});value.days[0].foodCount=1;value.totals.intake={...value.days[0].intake};value.totals.foodCount=1;value.averages.intake={value:100,knownDayCount:1}}value.facts=[{code:'RECORDED_COVERAGE',text:moved?'No food or activity records in this range.':'Server-recorded facts remain available without Q&A consent.',values:{}}];return value}
beforeEach(()=>{moved=false;vi.resetAllMocks();vi.mocked(fetchFoodEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(fetchExerciseEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(summaryApi.fetchTrends).mockImplementation(async(from,to)=>summary(from,to));vi.mocked(summaryApi.fetchDailySummary).mockImplementation(async date=>daySummary(date));vi.mocked(fetchFoodEntriesV2).mockImplementation(async(from,to)=>rootPage(!moved&&from<='2020-01-01'&&to>='2020-01-01'?[foodRecordRootFixture({...savedFood(),recordDate:'2020-01-01'})]:[]));vi.mocked(recordApi.fetchExerciseRecords).mockResolvedValue([]);vi.mocked(fetchWeights).mockResolvedValue([])})
async function mount(){const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,profileVersion:1,adultConfirmed:true,onboardingCompleted:true,qaConsentGranted:false}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore();const router=createMemoryRouter([{path:'/history',element:<HistoryPage />}],{initialEntries:['/history?from=2020-01-01&to=2020-01-03']});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RouterProvider router={router} /></DraftProvider></SessionContext.Provider></QueryClientProvider>);return {client,router}}
describe('F15 canonical range/query integration',()=>{
 it('loads exact-range weights and server facts even with Q&A consent refused',async()=>{await mount();await screen.findByText('No recorded weight measurements in this period.');expect(fetchWeights).toHaveBeenCalledWith('2020-01-01','2020-01-03',expect.any(AbortSignal));expect(screen.getByText('Server-recorded facts remain available without Q&A consent.')).toBeTruthy();expect(screen.getByTestId('canonical-chart').textContent).toContain('2020-01-03');expect(within(screen.getByRole('row',{name:/Recorded intake/})).getByText('1 of 3 calendar days')).toBeTruthy()})
 it('orders filters and statistics before trends, then saved record cards',async()=>{
  await mount();await screen.findByTestId('canonical-chart')
  const filter=screen.getByLabelText('History from'),statistics=screen.getByRole('table',{name:'Recorded totals and averages'}),chart=screen.getByTestId('canonical-chart')
  const listDate=screen.getByRole('heading',{name:'2020-01-01'})
  for(const [before,after] of [[filter,statistics],[statistics,chart],[chart,listDate]])expect(before.compareDocumentPosition(after)&Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
 })
 it('does not call failed weight reads empty or hide available energy statistics',async()=>{vi.mocked(fetchWeights).mockRejectedValue(new Error('Offline'));await mount();await screen.findByText('Measured weights could not be loaded. Energy records and their saved estimates are unchanged.');expect(screen.queryByText('No recorded weight measurements in this period.')).toBeNull();expect(screen.getByRole('table',{name:'Recorded totals and averages'})).toBeTruthy();expect(screen.getByText('Server-recorded facts remain available without Q&A consent.')).toBeTruthy()})
 it('changes weight and summary query range together and never carries old measurements forward',async()=>{vi.mocked(fetchWeights).mockImplementation(async from=>from==='2020-01-01'?[{id:'1',recordDate:'2020-01-02',weightKg:71.5,version:2}]:[]);const {router}=await mount();await screen.findByText('2020-01-02: 71.5 kg');await act(async()=>{await router.navigate('/history?from=2020-02-01&to=2020-02-03')});await screen.findByText('No recorded weight measurements in this period.');expect(screen.queryByText('2020-01-02: 71.5 kg')).toBeNull();expect(fetchWeights).toHaveBeenCalledWith('2020-02-01','2020-02-03',expect.any(AbortSignal));expect(summaryApi.fetchTrends).toHaveBeenCalledWith('2020-02-01','2020-02-03',expect.any(AbortSignal))})
 it('requeries list, graph, average and facts after owner cache invalidation for a confirmed date move',async()=>{const {client}=await mount();await screen.findByText('No recorded weight measurements in this period.');expect(screen.queryByText('No confirmed records match these filters.')).toBeNull();expect(JSON.parse(screen.getByTestId('canonical-chart').textContent!).days[0].intake.knownSum).toBe(100);moved=true;await act(async()=>{await client.invalidateQueries({queryKey:['owner',user.id]})});await screen.findByText('No confirmed records match these filters.');expect(screen.getByText('No food or activity records in this range.')).toBeTruthy();expect(JSON.parse(screen.getByTestId('canonical-chart').textContent!).days[0].intake.knownSum).toBeNull();await waitFor(()=>expect(within(screen.getByRole('row',{name:/Recorded intake/})).getByText('0 of 3 calendar days')).toBeTruthy());expect(fetchFoodEntriesV2).toHaveBeenCalledTimes(2);expect(summaryApi.fetchTrends).toHaveBeenCalledTimes(2)})
})
