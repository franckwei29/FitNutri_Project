import {fetchFoodEntriesV2,fetchFoodEntryV2} from '../../src/api/foodEntriesV2'
import {fetchExerciseEntriesV2} from '../../src/api/exerciseEntriesV2'
import {rootPage,foodRecordRootFixture,savedDishV2,savedSingleV2} from '../fixtures/entryRootsV2'
import {act,fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {HistoryPage} from '../../src/pages/HistoryPage'
import * as summaryApi from '../../src/api/summary'
import {fetchWeights} from '../../src/api/weight'
import * as recordsApi from '../../src/api/records'
import {ApiRequestError} from '../../src/api/errors'
import {daySummary,periodSummary} from '../fixtures/summary'
import {profile,user} from '../fixtures/profile'
import type {FoodRecord} from '../../src/api/contracts'
vi.mock('../../src/api/foodEntriesV2',()=>({fetchFoodEntriesV2:vi.fn(),fetchFoodEntryV2:vi.fn()}))
vi.mock('../../src/api/exerciseEntriesV2',()=>({fetchExerciseEntriesV2:vi.fn(),fetchExerciseEntryV2:vi.fn()}))
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn(),fetchWeeklySummary:vi.fn()}))
vi.mock('../../src/api/weight',()=>({fetchWeights:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecords:vi.fn(),fetchExerciseRecords:vi.fn(),fetchFoodRecord:vi.fn(),fetchExerciseRecord:vi.fn()}))
function record(date='2026-09-17'):FoodRecord{return {id:'9007199254740993',clientEntryId:'0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd',recordDate:date,source:'IMAGE',version:2,dataQuality:'LEGACY_UNVERIFIED',createdAt:'2026-10-16T01:00:00Z',updatedAt:'2026-10-16T01:00:00Z',reference:null,foodName:'Saved historical food',quantityGrams:150,mealType:'LUNCH',nutrients:{energyKj:988.5,proteinGrams:null,fatGrams:0.3,carbsGrams:null}}}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(fetchFoodEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(fetchExerciseEntriesV2).mockResolvedValue(rootPage([]));vi.mocked(fetchWeights).mockResolvedValue([]);vi.mocked(summaryApi.fetchDailySummary).mockImplementation(async date=>daySummary(date));vi.mocked(summaryApi.fetchTrends).mockImplementation(async(from,to)=>periodSummary(from,to));vi.mocked(summaryApi.fetchWeeklySummary).mockImplementation(async end=>({current:periodSummary('2026-10-11',end),previous:periodSummary('2026-10-04','2026-10-10')}));vi.mocked(recordsApi.fetchFoodRecords).mockResolvedValue([]);vi.mocked(recordsApi.fetchExerciseRecords).mockResolvedValue([]);vi.mocked(recordsApi.fetchFoodRecord).mockResolvedValue(record())})
async function mount(url='/history'){const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,profileVersion:1,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ',qaConsentGranted:false}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore();const router=createMemoryRouter([{path:'/history',element:<HistoryPage />}],{initialEntries:[url]});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RouterProvider router={router} /></DraftProvider></SessionContext.Provider></QueryClientProvider>);return {router,client}}
describe('F12 URL-driven history and owned snapshot details',()=>{
 it('directly locates a record 30 days earlier, retaining saved source and missing nutrients',async()=>{
  vi.mocked(fetchFoodEntriesV2).mockResolvedValue(rootPage([foodRecordRootFixture(record())]));await mount('/history?from=2026-09-17&to=2026-09-17&type=food&highlight=food:9007199254740993')
  await screen.findByRole('dialog',{name:'Saved record details'});await screen.findByText('Record date: 2026-09-17');expect(recordsApi.fetchFoodRecord).toHaveBeenCalledWith('9007199254740993',expect.any(AbortSignal));expect(fetchExerciseEntriesV2).not.toHaveBeenCalled();expect(screen.getAllByText('Saved historical food').length).toBeGreaterThan(0);expect(screen.getByText('Image · Legacy source')).toBeTruthy();expect(screen.getByRole('link',{name:/Saved historical food/,hidden:true}).getAttribute('aria-current')).toBe('true');expect(screen.getAllByText('Unavailable').length).toBeGreaterThan(0)
 })
 it('restores URL date/type filters with Back and uses the canonical daily/weekly parameters',async()=>{
  const {router}=await mount('/history?from=2026-09-17&to=2026-09-17&type=food');await screen.findByText('No confirmed records match these filters.')
  await act(async()=>{await router.navigate('/history?from=2026-10-16&to=2026-10-17&type=exercise&summary=daily&summaryDate=2026-10-16')});await waitFor(()=>expect(summaryApi.fetchDailySummary).toHaveBeenCalledWith('2026-10-16',expect.any(AbortSignal)))
  await act(async()=>{await router.navigate('/history?from=2026-10-16&to=2026-10-17&type=all&summary=weekly&summaryDate=2026-10-17')});await waitFor(()=>expect(summaryApi.fetchWeeklySummary).toHaveBeenCalledWith('2026-10-17',expect.any(AbortSignal)))
  await act(async()=>{await router.navigate(-1)});expect((screen.getByLabelText('History from') as HTMLInputElement).value).toBe('2026-10-16');expect(screen.getByRole('combobox',{name:'Record type'}).textContent).toContain('Exercise')
 })
 it('uses by-ID current date to explain and relocate a moved record without changing the record',async()=>{
  vi.mocked(recordsApi.fetchFoodRecord).mockResolvedValue(record('2026-10-15'));const {router}=await mount('/history?from=2026-10-16&to=2026-10-16&type=exercise&highlight=food:9007199254740993');await screen.findByText('Record date: 2026-10-15');fireEvent.click(screen.getByRole('button',{name:'View this record’s current date'}));await waitFor(()=>expect(router.state.location.search).toContain('from=2026-10-15'));expect(router.state.location.search).toContain('to=2026-10-15');expect(router.state.location.search).toContain('type=all')
 })
 it('keeps a deleted/foreign detail unavailable and distinguishes list failure from no matches',async()=>{
  vi.mocked(recordsApi.fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Not found',code:'NOT_FOUND',status:404,outcome:'read-failed',writeAttempted:false}));vi.mocked(fetchFoodEntriesV2).mockRejectedValue(new Error('Offline'));await mount('/history?highlight=food:9007199254740993');await screen.findByText('This record was removed or is not available to this account.');expect(screen.getByText('History records could not be loaded.')).toBeTruthy();expect(screen.queryByText('No confirmed records match these filters.')).toBeNull()
 })
 it('rejects invalid URL ranges before records or summary reads',async()=>{
  await mount('/history?from=2026-10-18&to=2026-10-17');await screen.findByRole('button',{name:'Use last 7 days'});expect(fetchFoodEntriesV2).not.toHaveBeenCalled();expect(summaryApi.fetchTrends).not.toHaveBeenCalled()
 })
})

 it('locates every saved typed root at a past date and keeps whole dishes across root pages',async()=>{vi.mocked(fetchFoodEntriesV2).mockImplementation(async(_from,_to,page)=>page===0?rootPage([savedDishV2()],0,2,true):rootPage([savedSingleV2()],1,2,false));await mount('/history?from=2020-01-02&to=2020-01-02&type=food&highlightRoot=dish:7&highlightRoot=single:8');const dish=await screen.findByRole('article',{name:'Saved shared recipe'});expect(dish.getAttribute('data-highlighted')).toBe('true');expect(screen.getAllByRole('article')).toHaveLength(1);fireEvent.click(screen.getByRole('button',{name:'More food'}));const single=await screen.findByRole('article',{name:'Saved rice'});expect(single.getAttribute('data-highlighted')).toBe('true');expect(screen.getAllByRole('article')).toHaveLength(2);expect(fetchFoodEntriesV2).toHaveBeenCalledWith('2020-01-02','2020-01-02',1,expect.any(AbortSignal));expect(recordsApi.fetchFoodRecords).not.toHaveBeenCalled();expect(screen.queryByRole('button',{name:'More food'})).toBeNull()})
 it('reads a typed root snapshot outside the filters and explicitly relocates its current date',async()=>{vi.mocked(fetchFoodEntryV2).mockResolvedValue(savedDishV2());const {router}=await mount('/history?from=2020-01-03&to=2020-01-03&type=exercise&openRoot=dish:7');await screen.findByRole('article',{name:'Saved shared recipe'});expect(fetchFoodEntryV2).toHaveBeenCalledWith('dish','7',expect.any(AbortSignal));expect(screen.getByText('This entry is outside the selected date or type filters.')).toBeTruthy();fireEvent.click(screen.getByRole('button',{name:'View date'}));await waitFor(()=>expect(router.state.location.search).toContain('from=2020-01-02'));expect(router.state.location.search).toContain('type=all');expect(recordsApi.fetchFoodRecord).not.toHaveBeenCalled()})
 it('keeps a removed or foreign typed root unavailable without showing cached private details',async()=>{vi.mocked(fetchFoodEntryV2).mockRejectedValue(new ApiRequestError({message:'Not found',code:'NOT_FOUND',status:404,outcome:'read-failed',writeAttempted:false}));await mount('/history?openRoot=dish:7');await screen.findByText('This entry was removed or is not available to this account.');expect(screen.queryByRole('article',{name:'Saved shared recipe'})).toBeNull()})

 it('retains complete loaded roots when the next page fails and retries only a read',async()=>{vi.mocked(fetchFoodEntriesV2).mockImplementation(async(_from,_to,page)=>{if(page===1)throw new Error('Synthetic page offline');return rootPage([savedDishV2()],0,2,true)});await mount('/history?from=2020-01-02&to=2020-01-02&type=food');await screen.findByRole('article',{name:'Saved shared recipe'});fireEvent.click(screen.getByRole('button',{name:'More food'}));await screen.findByText('More saved entries could not be loaded. Your loaded entries are unchanged.');expect(screen.getByRole('article',{name:'Saved shared recipe'})).toBeTruthy();expect(screen.queryByText('No confirmed records match these filters.')).toBeNull();expect(recordsApi.fetchFoodRecords).not.toHaveBeenCalled()})
