import {fetchDailySummary as dashboardDay,fetchTrends as dashboardTrend} from '../../src/api/summary'
import {fetchFoodEntriesV2 as dashboardFoods} from '../../src/api/foodEntriesV2'
import {fetchExerciseEntriesV2 as dashboardExercises} from '../../src/api/exerciseEntriesV2'
import {daySummary,periodSummary} from '../fixtures/summary'
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn(),fetchWeeklySummary:vi.fn()}))
import {fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {routes} from '../../src/app/router'
import {fetchCapabilities} from '../../src/api/capabilities'
import {loadExerciseTypes,previewDatedExercise} from '../../src/api/exerciseTypes'
import {fetchExerciseOptionsV2,fetchMovementsV2,fetchRecentExerciseV2,previewExerciseV2} from '../../src/api/exerciseEntriesV2'
import {createRecordsV2} from '../../src/api/submissionsV2'
import {createRecords} from '../../src/api/submissions'
import {exerciseEstimate,exerciseType} from '../fixtures/exercise'
import {recognizeImage} from '../../src/api/recognition'
import {previewFoodPortion} from '../../src/api/foodPreview'
import {imageRecognition} from '../fixtures/media'
import {previewFoodV2,fetchRecentFoodV2} from '../../src/api/foodEntriesV2'
import {profile,user} from '../fixtures/profile'
vi.mock('../../src/api/recognition',async importOriginal=>({...await importOriginal<typeof import('../../src/api/recognition')>(),recognizeImage:vi.fn()}))
vi.mock('../../src/api/foodPreview',()=>({previewFoodPortion:vi.fn()}))
vi.mock('../../src/api/foodReference',async importOriginal=>({...await importOriginal<typeof import('../../src/api/foodReference')>(),searchSmartFood:vi.fn()}))
vi.mock('../../src/api/capabilities',()=>({fetchCapabilities:vi.fn()}))
vi.mock('../../src/api/exerciseTypes',()=>({loadExerciseTypes:vi.fn(),previewDatedExercise:vi.fn()}))
vi.mock('../../src/api/submissions',()=>({createRecords:vi.fn(),fetchSubmissionReceipt:vi.fn()}))
vi.mock('../../src/api/exerciseEntriesV2',async original=>({...await original<typeof import('../../src/api/exerciseEntriesV2')>(),fetchExerciseOptionsV2:vi.fn(),fetchMovementsV2:vi.fn(),fetchExerciseEntriesV2:vi.fn(),fetchRecentExerciseV2:vi.fn(),previewExerciseV2:vi.fn()}))
vi.mock('../../src/api/submissionsV2',async original=>({...await original<typeof import('../../src/api/submissionsV2')>(),createRecordsV2:vi.fn()}))
vi.mock('../../src/api/foodEntriesV2',async original=>({...await original<typeof import('../../src/api/foodEntriesV2')>(),fetchFoodEntriesV2:vi.fn(),previewFoodV2:vi.fn(),fetchRecentFoodV2:vi.fn()}))
const feature={configured:false,available:false,reason:'This AI feature is not configured.'}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(dashboardDay).mockImplementation(async date=>daySummary(date));vi.mocked(dashboardTrend).mockImplementation(async(from,to)=>periodSummary(from,to));vi.mocked(dashboardFoods).mockResolvedValue({results:[],page:0,size:20,totalMatches:0,hasMore:false});vi.mocked(dashboardExercises).mockResolvedValue({results:[],page:0,size:20,totalMatches:0,hasMore:false});vi.mocked(fetchRecentFoodV2).mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false});vi.mocked(fetchExerciseOptionsV2).mockResolvedValue({results:[{id:'201',optionKey:'synthetic-walking',revision:'r1',displayName:exerciseType.displayName,category:'OUTDOOR_OTHER',periodKind:'ACTIVITY',mappingKind:'SPECIFIC_REFERENCE',allowedDetails:[],referenceId:exerciseType.id,mappingReason:'Synthetic test only'}],totalMatches:1,page:0,size:20,hasMore:false});vi.mocked(fetchMovementsV2).mockResolvedValue({results:[],totalMatches:0,page:0,size:20,hasMore:false});vi.mocked(fetchRecentExerciseV2).mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false});vi.mocked(previewExerciseV2).mockImplementation(async input=>{if(input.entry.kind!=='QUICK')throw Error('Fixture');const period=input.entry.period;return {periods:[{clientEntryId:period.clientEntryId,durationMinutes:period.durationMinutes,status:'AVAILABLE',estimatedKj:500,calculationFingerprint:'a'.repeat(64),unavailableReason:null}],totalMinutes:period.durationMinutes,knownEnergyKj:500,partial:false,fingerprint:'b'.repeat(64),inputSignature:'c'.repeat(64)}});vi.mocked(fetchCapabilities).mockResolvedValue({schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'test',catalogues:{food:{ready:true,verified:true,reason:null},exercise:{ready:true,verified:true,reason:null}},features:{image:feature,transcription:feature,speechParsing:feature,qa:feature}});vi.mocked(loadExerciseTypes).mockResolvedValue([exerciseType]);vi.mocked(previewDatedExercise).mockImplementation(async(_,minutes,date)=>exerciseEstimate({recordDate:date,durationMinutes:minutes}))})
async function mount(){const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,profileVersion:1,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ',qaConsentGranted:false}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore();const router=createMemoryRouter(routes,{initialEntries:['/add']});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RouterProvider router={router} /></DraftProvider></SessionContext.Provider></QueryClientProvider>);return router}
describe('F10 Add → Entry → Review router integration',()=>{
 it('starts real manual food editing without a discard prompt and keeps unavailable AI methods disabled',async()=>{
  await mount();await waitFor(()=>expect((screen.getByRole('button',{name:'Continue'}) as HTMLButtonElement).disabled).toBe(false))
  expect((screen.getByRole('button',{name:/Voice entry Start/}) as HTMLButtonElement).disabled).toBe(true)
  expect((screen.getByRole('button',{name:/Upload food image Review/}) as HTMLButtonElement).disabled).toBe(true)
  fireEvent.click(screen.getByRole('button',{name:'Continue'}));await screen.findByRole('heading',{name:'Add food'}, {timeout:5000});expect(screen.queryByRole('dialog')).toBeNull()
  expect(screen.getByRole('combobox',{name:'Find food to add'})).toBeTruthy();expect(screen.getByRole('combobox',{name:'Default meal'})).toBeTruthy();expect(screen.queryByRole('button',{name:'Remove food 1'})).toBeNull();expect((screen.getByRole('button',{name:'Add to meal'}) as HTMLButtonElement).disabled).toBe(true);expect(createRecords).not.toHaveBeenCalled()
 })
 it('selects a structured activity and reaches shared Review with false Q&A consent',async()=>{
  await mount();fireEvent.click(await screen.findByRole('button',{name:/Exercise Log your physical activity/}));await waitFor(()=>expect((screen.getByRole('button',{name:'Continue'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Continue'}))
  const picker=await screen.findByRole('combobox',{name:'Find activity or movement'}, {timeout:5000});await waitFor(()=>expect((picker as HTMLInputElement).disabled).toBe(false));fireEvent.change(picker,{target:{value:'walking'}});fireEvent.click(await screen.findByRole('option',{name:/Walking for pleasure/}))
  fireEvent.change(screen.getByLabelText('Duration (minutes)'),{target:{value:'10.5'}});await waitFor(()=>expect((screen.getByRole('button',{name:'Review exercise entry'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Review exercise entry'}))
  await screen.findByRole('heading',{name:'Review your record.'});expect(screen.queryByRole('dialog')).toBeNull();expect(createRecords).not.toHaveBeenCalled();expect(createRecordsV2).not.toHaveBeenCalled()
 })
 it('shows capability-read failure and disables starting rather than inventing availability',async()=>{
  vi.mocked(fetchCapabilities).mockRejectedValue(new Error('Offline'));await mount();await screen.findByText('Entry availability could not be checked.');expect((screen.getByRole('button',{name:'Continue'}) as HTMLButtonElement).disabled).toBe(true);expect(createRecords).not.toHaveBeenCalled()
 })
 it('routes image candidates through explicit selection and shared V2 Review before its single IMAGE batch (controlled API)',async()=>{
  const originalUrl=URL;class PreviewURL extends originalUrl {static createObjectURL=()=> 'blob:synthetic-image';static revokeObjectURL=vi.fn()}vi.stubGlobal('URL',PreviewURL)
  try{
   const configured={configured:true,available:true,reason:null};vi.mocked(fetchCapabilities).mockResolvedValue({schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'test',catalogues:{food:{ready:true,verified:true,reason:null},exercise:{ready:true,verified:true,reason:null}},features:{image:configured,transcription:feature,speechParsing:feature,qa:feature}})
   vi.mocked(recognizeImage).mockResolvedValue(imageRecognition());vi.mocked(previewFoodPortion).mockImplementation(async(_id,grams)=>({reference:imageRecognition().candidates[0].referenceMatches[0],quantityGrams:grams,nutrients:{energyKj:988.5,proteinGrams:4.65,fatGrams:0.3,carbsGrams:52.05},calculationFingerprint:'a'.repeat(64)}))
   const nutrients={energyKj:988.5,proteinGrams:4.65,fatGrams:0.3,carbsGrams:52.05};vi.mocked(previewFoodV2).mockImplementation(async input=>({entries:input.entries.map(root=>{if(root.kind!=='SINGLE')throw Error('Media cannot invent recipes');return {kind:'SINGLE',clientEntryId:root.clientEntryId,leaves:[{clientEntryId:root.clientEntryId,referenceId:root.referenceId,inputGrams:root.quantityGrams,consumedGrams:root.quantityGrams,nutrients}],totals:nutrients}}),totals:nutrients,fingerprint:'a'.repeat(64),inputSignature:'b'.repeat(64),calculationPolicy:'Controlled test only'}));vi.mocked(createRecordsV2).mockImplementation(async envelope=>{if(envelope.domain!=='FOOD')throw Error('Expected food');return {schemaVersion:2,submissionId:envelope.payload.submissionId,kind:'FOOD_BATCH_V2',committedAt:'2026-10-16T01:00:00Z',replayed:false,roots:envelope.payload.entries.map((root,i)=>({kind:root.kind,id:String(i+1),clientEntryId:root.clientEntryId,recordDate:envelope.payload.recordDate,versionAtCreation:0})),leaves:envelope.payload.entries.map((root,i)=>({kind:'FOOD_COMPONENT',id:String(i+1),clientEntryId:root.clientEntryId,rootClientEntryId:root.clientEntryId,recordDate:envelope.payload.recordDate,versionAtCreation:0}))}})
   await mount();await waitFor(()=>expect((screen.getByRole('button',{name:/Upload food image Review/}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:/Upload food image Review/}));fireEvent.click(screen.getByRole('button',{name:'Continue'}));await screen.findByRole('heading',{name:'Start with a food image'});expect(screen.queryByRole('combobox',{name:'Reference food 1'})).toBeNull()
   fireEvent.change(screen.getByLabelText('Food image'),{target:{files:[new File(['synthetic'],'fixture.png',{type:'image/png'})]}});expect(recognizeImage).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Process image'}));await screen.findByRole('button',{name:'Use candidates in food entry'});expect(createRecords).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Use candidates in food entry'}));await screen.findByRole('button',{name:'Remove food 2'});fireEvent.click(screen.getByRole('button',{name:'Remove food 2'}));fireEvent.click(screen.getByRole('button',{name:'Choose suggested reference 1 for food 1'}));expect((screen.getByRole('button',{name:'Review food entries'}) as HTMLButtonElement).disabled).toBe(true)
   fireEvent.mouseDown(screen.getByRole('combobox',{name:'Meal occasion 1'}));fireEvent.click(await screen.findByRole('option',{name:'Lunch'}));await waitFor(()=>expect((screen.getByRole('button',{name:'Review food entries'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Review food entries'}));await screen.findByRole('heading',{name:'Review your record.'});expect(createRecords).not.toHaveBeenCalled();await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));await screen.findByRole('heading',{name:'A little progress, every day.'});expect(createRecords).not.toHaveBeenCalled();expect(createRecordsV2).toHaveBeenCalledOnce();const payload=vi.mocked(createRecordsV2).mock.calls[0][0];expect(payload).toMatchObject({domain:'FOOD',payload:{source:'IMAGE',entries:[{kind:'SINGLE',referenceId:'10',quantityGrams:150,mealType:'LUNCH'}]}});expect(JSON.stringify(payload)).not.toContain('candidateKey');expect(PreviewURL.revokeObjectURL).toHaveBeenCalledOnce()
  }finally{vi.unstubAllGlobals()}
 },15000)

})
