import {DraftProvider} from '../../src/app/DraftProvider'
import {fetchFoodEntriesV2} from '../../src/api/foodEntriesV2'
import {fetchExerciseEntriesV2} from '../../src/api/exerciseEntriesV2'
import {rootPage,savedDishV2,savedSingleV2,savedWorkoutV2} from '../fixtures/entryRootsV2'
import {fireEvent,render,screen,waitFor,within} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {createQueryClient} from '../../src/app/queryClient'
import {DashboardPage} from '../../src/pages/DashboardPage'
import {fetchDailySummary,fetchTrends} from '../../src/api/summary'
import {fetchFoodRecords,fetchExerciseRecords} from '../../src/api/records'
import {daySummary,metric,periodSummary} from '../fixtures/summary'
import {profile,user} from '../fixtures/profile'
import {todayInZone} from '../../src/domain/dates'
vi.mock('../../src/api/foodEntriesV2',()=>({fetchFoodEntriesV2:vi.fn(),fetchFoodEntryV2:vi.fn()}))
vi.mock('../../src/api/exerciseEntriesV2',()=>({fetchExerciseEntriesV2:vi.fn(),fetchExerciseEntryV2:vi.fn()}))
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecords:vi.fn(),fetchExerciseRecords:vi.fn()}))
beforeEach(()=>{vi.resetAllMocks();vi.mocked(fetchFoodEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(fetchExerciseEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(fetchDailySummary).mockImplementation(async date=>daySummary(date));vi.mocked(fetchTrends).mockImplementation(async(from,to)=>periodSummary(from,to));vi.mocked(fetchFoodRecords).mockResolvedValue([]);vi.mocked(fetchExerciseRecords).mockResolvedValue([])})
async function mount(url='/dashboard'){const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,profileVersion:1,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ',qaConsentGranted:false}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore();const router=createMemoryRouter([{path:'/dashboard',element:<DashboardPage />}],{initialEntries:[url]});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RouterProvider router={router} /></DraftProvider></SessionContext.Provider></QueryClientProvider>);return {router,client}}
describe('F11 server-driven dashboard',()=>{
 it('keeps a genuine empty day distinct from zero and allows factual summaries without Q&A consent',async()=>{
  await mount();await screen.findByText('No confirmed records in this date range.');const intake=screen.getByRole('region',{name:'Energy intake'});expect(within(intake).getByText('Not recorded')).toBeTruthy();expect(within(intake).queryByText('0 kJ')).toBeNull();expect(screen.getByText('No foods recorded for this date.')).toBeTruthy();expect(fetchDailySummary).toHaveBeenCalledWith(todayInZone(),expect.any(AbortSignal))
 })
 it('renders true zero, all-missing and partial metrics separately without deriving energy balance',async()=>{
  const summary=daySummary('2026-10-16');summary.day.intake=metric({knownSum:0,recordCount:1,knownValueCount:1,hasData:true});summary.day.protein=metric({recordCount:1,missingValueCount:1,hasData:true});summary.day.expenditure=metric({knownSum:100,recordCount:2,knownValueCount:1,missingValueCount:1,hasData:true,partial:true});summary.day.activityMinutes=25.5;summary.day.exerciseCount=2;summary.goalComparison={dailyTargetKj:8400,comparedDays:1,knownIntakeKj:0,targetForComparedDaysKj:8400};vi.mocked(fetchDailySummary).mockResolvedValue(summary)
  await mount('/dashboard?date=2026-10-16');await screen.findByText('0 kJ');expect(within(screen.getByRole('region',{name:'Protein'})).getByText('Unavailable')).toBeTruthy();expect(within(screen.getByRole('region',{name:'Exercise expenditure'})).getByText('Partial: 1 of 2 values known')).toBeTruthy();expect(screen.getByText('25.5 min')).toBeTruthy();expect(screen.getByText(/Current personal daily intake reference/)).toBeTruthy();expect(screen.queryByText(/remaining|net energy/i)).toBeNull()
 })
 it('uses the same selected past date for summary and lists, then returns to current today explicitly',async()=>{
  await mount('/dashboard?date=2026-10-16');await screen.findByText('No foods recorded for this date.');expect(fetchFoodEntriesV2).toHaveBeenCalledWith('2026-10-16','2026-10-16',0,expect.any(AbortSignal));expect(fetchExerciseEntriesV2).toHaveBeenCalledWith('2026-10-16','2026-10-16',0,expect.any(AbortSignal))
  fireEvent.click(screen.getByRole('button',{name:'Today'}));await waitFor(()=>expect(fetchDailySummary).toHaveBeenCalledWith(todayInZone(),expect.any(AbortSignal)))
 })
 it('shows read failures and retries without claiming an empty day',async()=>{
  vi.mocked(fetchDailySummary).mockRejectedValue(new Error('Offline'));vi.mocked(fetchFoodEntriesV2).mockRejectedValue(new Error('Offline'));vi.mocked(fetchExerciseEntriesV2).mockRejectedValue(new Error('Offline'));await mount();await screen.findByText('Daily summary could not be loaded.');expect(screen.queryByText('No foods recorded for this date.')).toBeNull();expect(screen.queryByRole('region',{name:'Energy intake'})).toBeNull();fireEvent.click(screen.getByRole('button',{name:'Refresh'}));await waitFor(()=>expect(fetchDailySummary).toHaveBeenCalledTimes(2))
 })
 it('does not silently replace an invalid requested date with today',async()=>{
  await mount('/dashboard?date=2026-02-30');await screen.findByText('Choose a valid date that is not in the future.');expect(fetchDailySummary).not.toHaveBeenCalled();expect(fetchFoodEntriesV2).not.toHaveBeenCalled()
 })
 it('places the full-width trend after metrics and before facts and logs',async()=>{
  await mount();await screen.findByRole('table',{name:'Recorded intake values'})
  const metric=screen.getByRole('region',{name:'Protein'}),trend=screen.getByRole('heading',{name:'Recorded intake'}),facts=screen.getByRole('heading',{name:'This date, at a glance'}),log=screen.getByRole('heading',{name:'Food log'})
  for(const [before,after] of [[metric,trend],[trend,facts],[facts,log]])expect(before.compareDocumentPosition(after)&Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
 })
 it('bounds the small chart at the earliest supported record date',async()=>{
  await mount('/dashboard?date=1900-01-01');await waitFor(()=>expect(fetchTrends).toHaveBeenCalledWith('1900-01-01','1900-01-01',expect.any(AbortSignal)));expect(fetchFoodEntriesV2).toHaveBeenCalledWith('1900-01-01','1900-01-01',0,expect.any(AbortSignal))
 })
})

 it('compares root entry counts rather than five food components or two activity periods',async()=>{const value=daySummary('2020-01-02');value.day.foodCount=5;value.day.foodEntryCount=2;value.day.exerciseCount=2;value.day.exerciseEntryCount=1;value.day.activityMinutes=70;vi.mocked(fetchDailySummary).mockResolvedValue(value);vi.mocked(fetchFoodEntriesV2).mockResolvedValue(rootPage([savedDishV2(),savedSingleV2()]));vi.mocked(fetchExerciseEntriesV2).mockResolvedValue(rootPage([savedWorkoutV2()]));await mount('/dashboard?date=2020-01-02');await screen.findByRole('article',{name:'Saved gym session'});expect(screen.getAllByRole('article')).toHaveLength(3);expect(screen.queryByText(/Records changed while/)).toBeNull();expect(screen.getByText(/1 recorded entries · 2 activity periods/)).toBeTruthy();expect(screen.getByText('70 min')).toBeTruthy()})

it('v3.2 keeps concise icon actions and the Add links, marks today inactive, and restores invalid dates',async()=>{
 const {router}=await mount('/dashboard?date=invalid');await screen.findByText('Choose a valid date that is not in the future.');
 expect(screen.getByRole('button',{name:'Refresh'}).hasAttribute('disabled')).toBe(true);
 fireEvent.click(screen.getByRole('button',{name:'Today'}));await screen.findByText('No foods recorded for this date.');
 expect(router.state.location.search).toBe('');expect(screen.getByRole('button',{name:'Today'}).hasAttribute('disabled')).toBe(true);
 expect(screen.getByRole('button',{name:'Refresh'}).querySelector('svg')).toBeTruthy();
 expect(screen.getByRole('link',{name:'Add food'}).getAttribute('href')).toBe('/add?kind=food');
 expect(screen.getByRole('link',{name:'Add exercise'}).getAttribute('href')).toBe('/add?kind=exercise');
 expect(screen.getByRole('link',{name:'History'}).getAttribute('href')).toContain('type=all');
 expect(screen.getByRole('link',{name:'Goals'}).getAttribute('href')).toBe('/profile');
 const tableToggle=screen.getByText('Data table').closest('summary')!;fireEvent.click(tableToggle);expect(tableToggle.parentElement?.hasAttribute('open')).toBe(true);
})
