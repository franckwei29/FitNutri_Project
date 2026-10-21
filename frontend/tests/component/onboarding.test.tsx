import { beforeEach,describe,expect,it,vi } from 'vitest'
import { fireEvent,render,screen,waitFor } from '@testing-library/react'
import { QueryClient,QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter,RouterProvider } from 'react-router-dom'
import { SessionProvider } from '../../src/app/SessionProvider'
import { routes } from '../../src/app/router'
import { ApiRequestError } from '../../src/api/errors'
import * as auth from '../../src/api/auth'
import * as api from '../../src/api/profile'
import * as summaryApi from '../../src/api/summary'
import * as recordApi from '../../src/api/records'
import {daySummary,periodSummary} from '../fixtures/summary'
import { todayInZone } from '../../src/domain/dates'
import { user,profile } from '../fixtures/profile'
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecords:vi.fn(),fetchExerciseRecords:vi.fn()}))
vi.mock('../../src/api/auth',()=>({fetchCurrentUser:vi.fn(),login:vi.fn(),register:vi.fn(),logout:vi.fn()}))
vi.mock('../../src/api/profile',()=>({fetchProfile:vi.fn(),completeOnboarding:vi.fn(),updateSettings:vi.fn()}))
vi.mock('../../src/api/csrf',()=>({ensureCsrfToken:vi.fn(),clearCsrfToken:vi.fn()}))
const today=todayInZone()
const saved=()=>profile({profileVersion:1,user:{...user,displayName:'Changed synthetic',version:1},birthYear:1990,adultConfirmed:true,sex:'OTHER',heightCm:170,activityLevel:'LIGHT',healthSafetyAnswer:'NO',qaConsentGranted:false,onboardingCompleted:true,currentWeight:{id:'42',recordDate:today,weightKg:70.25,version:0}})
function mount() {const router=createMemoryRouter(routes,{initialEntries:['/onboarding']});render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false,gcTime:Infinity}}})}><SessionProvider><RouterProvider router={router} /></SessionProvider></QueryClientProvider>);return router}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(summaryApi.fetchDailySummary).mockImplementation(async date=>daySummary(date));vi.mocked(summaryApi.fetchTrends).mockImplementation(async(from,to)=>periodSummary(from,to));vi.mocked(recordApi.fetchFoodRecords).mockResolvedValue([]);vi.mocked(recordApi.fetchExerciseRecords).mockResolvedValue([]);vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);vi.mocked(api.fetchProfile).mockResolvedValue(profile());vi.mocked(api.completeOnboarding).mockResolvedValue(saved())})
async function choose(label:string,option:string) {fireEvent.mouseDown(screen.getByRole('combobox',{name:label}));fireEvent.click(await screen.findByRole('option',{name:option}))}
async function basics() {
  await screen.findByRole('heading',{name:'Let’s set up your profile.'});fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Changed synthetic'}});fireEvent.click(screen.getByLabelText('I confirm I am 18 years of age or older.'));fireEvent.change(screen.getByLabelText('Year of birth'),{target:{value:'1990'}});await choose('Sex','Other');fireEvent.change(screen.getByLabelText('Height (cm)'),{target:{value:'170'}});fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'70.25'}})
}
async function toFinalStep() {await basics();fireEvent.click(screen.getByRole('button',{name:'Continue'}));await choose('Activity level','Lightly active');fireEvent.click(screen.getByRole('button',{name:'Continue'}));fireEvent.click(screen.getByRole('button',{name:'No'}))}
describe('F06 atomic three-step onboarding',()=>{
  it('back navigation preserves fields and no writes occur before final confirmation',async()=>{
    mount();await basics();fireEvent.click(screen.getByRole('button',{name:'Continue'}));await choose('Activity level','Lightly active');fireEvent.click(screen.getByRole('button',{name:'Back'}));expect((screen.getByLabelText('Weight (kg)') as HTMLInputElement).value).toBe('70.25');expect((screen.getByLabelText('Display name') as HTMLInputElement).value).toBe('Changed synthetic');expect(api.completeOnboarding).not.toHaveBeenCalled()
  })
  it('false consent completes via exactly one complete POST and routes using server completion',async()=>{
    const router=mount();await toFinalStep();expect((screen.getByLabelText('Allow FitNutri to use my confirmed records to answer general health questions.') as HTMLInputElement).checked).toBe(false);fireEvent.click(screen.getByRole('button',{name:'Complete setup'}));await screen.findByRole('heading',{name:'A little progress, every day.'});expect(router.state.location.pathname).toBe('/dashboard');expect(api.completeOnboarding).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({expectedUserVersion:0,expectedProfileVersion:null,displayName:'Changed synthetic',adultConfirmed:true,qaConsentGranted:false,goalType:null,dailyTargetKj:null,initialWeight:{recordDate:today,weightKg:70.25}}));expect(api.updateSettings).not.toHaveBeenCalled()
  })
  it('server validation returns to the affected step and keeps the unsaved data',async()=>{
    vi.mocked(api.completeOnboarding).mockRejectedValue(new ApiRequestError({message:'Check the highlighted fields.',status:400,code:'VALIDATION_ERROR',outcome:'rejected',writeAttempted:true,fieldErrors:[{field:'birthYear',clientEntryId:null,message:'Review the birth year.'}]}));const router=mount();await toFinalStep();fireEvent.click(screen.getByRole('button',{name:'Complete setup'}));await screen.findByText('Review the birth year.');await waitFor(()=>expect(document.activeElement).toBe(screen.getByLabelText('Year of birth')));expect((screen.getByLabelText('Weight (kg)') as HTMLInputElement).value).toBe('70.25');expect(router.state.location.pathname).toBe('/onboarding')
  })
  it('unknown outcome freezes writes and recovers only by reading and reviewing saved settings',async()=>{
    vi.mocked(api.completeOnboarding).mockRejectedValue(new ApiRequestError({message:'The request timed out.',status:0,code:'REQUEST_TIMEOUT',outcome:'unknown',writeAttempted:true}));const router=mount();await toFinalStep();fireEvent.click(screen.getByRole('button',{name:'Complete setup'}));await screen.findByRole('button',{name:'Check saved settings'});expect(screen.getByRole('button',{name:'Complete setup'}).hasAttribute('disabled')).toBe(true);vi.mocked(api.fetchProfile).mockResolvedValue(saved());fireEvent.click(screen.getByRole('button',{name:'Check saved settings'}));await screen.findByRole('button',{name:'Use saved profile'});expect(router.state.location.pathname).toBe('/onboarding');expect(api.completeOnboarding).toHaveBeenCalledOnce();fireEvent.click(screen.getByRole('button',{name:'Use saved profile'}));await screen.findByRole('heading',{name:'A little progress, every day.'});expect(api.completeOnboarding).toHaveBeenCalledOnce()
  })
  it('already-completed conflict checks the server and never sends another initial weight',async()=>{
    vi.mocked(api.completeOnboarding).mockRejectedValue(new ApiRequestError({message:'Setup is already complete.',status:409,code:'ONBOARDING_ALREADY_COMPLETED',outcome:'rejected',writeAttempted:true}));mount();await toFinalStep();fireEvent.click(screen.getByRole('button',{name:'Complete setup'}));await screen.findByRole('button',{name:'Check saved settings'});vi.mocked(api.fetchProfile).mockResolvedValue(saved());fireEvent.click(screen.getByRole('button',{name:'Check saved settings'}));await screen.findByText('Changed synthetic');expect(api.completeOnboarding).toHaveBeenCalledOnce();expect(screen.getByRole('button',{name:'Use saved profile'})).toBeTruthy()
  })
  it('incomplete inputs and leaving dirty form require review without writing',async()=>{
    mount();await screen.findByRole('heading',{name:'Let’s set up your profile.'});fireEvent.click(screen.getByRole('button',{name:'Continue'}));expect(screen.getByText('Confirm that you are 18 or older.')).toBeTruthy();fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Unsaved'}});fireEvent.click(screen.getByRole('link',{name:'FitNutri Assistant'}));await screen.findByRole('dialog',{name:'Discard unsaved changes?'});fireEvent.click(screen.getByRole('button',{name:'Cancel'}));expect((screen.getByLabelText('Display name') as HTMLInputElement).value).toBe('Unsaved');expect(api.completeOnboarding).not.toHaveBeenCalled()
  })
  it('an unconfirmed server read keeps onboarding read-only without retrying POST',async()=>{
    vi.mocked(api.completeOnboarding).mockRejectedValue(new ApiRequestError({message:'Unknown save result.',code:'REQUEST_TIMEOUT',outcome:'unknown',writeAttempted:true}));mount();await toFinalStep();fireEvent.click(screen.getByRole('button',{name:'Complete setup'}));await screen.findByRole('button',{name:'Check saved settings'});fireEvent.click(screen.getByRole('button',{name:'Check saved settings'}));await screen.findByText('Setup is not confirmed yet. Check again; this page will not submit another initial weight.');expect(screen.getByRole('button',{name:'Complete setup'}).hasAttribute('disabled')).toBe(true);expect(api.completeOnboarding).toHaveBeenCalledOnce()
  })
  it('repeated final clicks cannot dispatch a second pending transaction',async()=>{
    vi.mocked(api.completeOnboarding).mockReturnValue(new Promise(()=>{}));mount();await toFinalStep();const button=screen.getByRole('button',{name:'Complete setup'});fireEvent.click(button);fireEvent.click(button);expect(api.completeOnboarding).toHaveBeenCalledOnce();expect(button.hasAttribute('disabled')).toBe(true)
  })

})
