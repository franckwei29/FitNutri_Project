import { useEffect } from 'react'
import { useSession } from '../../src/hooks/useSession'
import { beforeEach,describe,expect,it,vi } from 'vitest'
import { fireEvent,render,screen } from '@testing-library/react'
import { QueryClient,QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter,RouterProvider } from 'react-router-dom'
import { SessionController } from '../../src/app/sessionEpoch'
import { SessionProvider } from '../../src/app/SessionProvider'
import { routes } from '../../src/app/router'
import { ApiRequestError } from '../../src/api/errors'
import * as auth from '../../src/api/auth'
import * as api from '../../src/api/profile'
import * as weightApi from '../../src/api/weight'
import { todayInZone } from '../../src/domain/dates'
import { user,profile } from '../fixtures/profile'
vi.mock('../../src/api/auth',()=>({fetchCurrentUser:vi.fn(),login:vi.fn(),register:vi.fn(),logout:vi.fn()}))
vi.mock('../../src/api/profile',()=>({fetchProfile:vi.fn(),completeOnboarding:vi.fn(),updateSettings:vi.fn()}))
vi.mock('../../src/api/weight',()=>({fetchWeights:vi.fn(),upsertWeight:vi.fn()}))
vi.mock('../../src/api/csrf',()=>({ensureCsrfToken:vi.fn(),clearCsrfToken:vi.fn()}))
const today=todayInZone(),weight={id:'42',recordDate:today,weightKg:70.25,version:0}
const initial=()=>profile({profileVersion:1,user,birthYear:1990,adultConfirmed:true,sex:'OTHER',heightCm:170,activityLevel:'LIGHT',goalType:'MAINTAIN',dailyTargetKj:8400,healthSafetyAnswer:'NO',qaConsentGranted:false,onboardingCompleted:true,currentWeight:weight})
function Capture({onSession}:{onSession:(actions:SessionController)=>void}) {const {actions}=useSession();useEffect(()=>onSession(actions),[actions,onSession]);return null}
function mount(onSession?:(actions:SessionController)=>void,client=new QueryClient({defaultOptions:{queries:{retry:false,gcTime:Infinity}}})) {const router=createMemoryRouter(routes,{initialEntries:['/profile']});render(<QueryClientProvider client={client}><SessionProvider>{onSession&&<Capture onSession={onSession} />}<RouterProvider router={router} /></SessionProvider></QueryClientProvider>);return router}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);vi.mocked(api.fetchProfile).mockResolvedValue(initial());vi.mocked(api.updateSettings).mockResolvedValue({...initial(),profileVersion:2});vi.mocked(weightApi.fetchWeights).mockResolvedValue([weight]);vi.mocked(weightApi.upsertWeight).mockResolvedValue({weight:{...weight,weightKg:71.25,version:1},profileVersion:2,currentWeight:{...weight,weightKg:71.25,version:1}})})
async function ready() {await screen.findByRole('heading',{name:'Your profile, your preferences.'});await screen.findByRole('button',{name:`Correct ${today}`})}
describe('F14 separate profile settings and dated weight actions',()=>{
  it('explicit goal/reference removal sends null and never writes weight',async()=>{
    vi.mocked(api.updateSettings).mockResolvedValue({...initial(),profileVersion:2,goalType:null,dailyTargetKj:null});mount();await ready();fireEvent.click(screen.getByRole('button',{name:'Remove goal'}));fireEvent.click(screen.getByRole('button',{name:'Remove energy reference'}));fireEvent.click(screen.getByRole('button',{name:'Save settings'}));await screen.findByText('Your profile is saved.');expect(api.updateSettings).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({goalType:null,dailyTargetKj:null,expectedUserVersion:0,expectedProfileVersion:1,qaConsentGranted:false}));expect(weightApi.upsertWeight).not.toHaveBeenCalled();expect(screen.queryByRole('button',{name:'Remove goal'})).toBeNull()
  })
  it('display-unit switch preserves the physical target in the submitted snapshot',async()=>{
    mount();await ready();fireEvent.mouseDown(screen.getByRole('combobox',{name:'Energy display unit'}));fireEvent.click(await screen.findByRole('option',{name:'Kilocalories (kcal)'}));fireEvent.click(screen.getByRole('button',{name:'Save settings'}));await screen.findByText('Your profile is saved.');expect(api.updateSettings).toHaveBeenCalledWith(expect.objectContaining({energyUnit:'KCAL',dailyTargetKj:8400}))
  })
  it('a weight refresh never replaces an unsaved settings baseline or typed name',async()=>{
    mount();await ready();fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Unsaved name'}});vi.mocked(api.fetchProfile).mockResolvedValue({...initial(),profileVersion:2});fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.click(screen.getByRole('button',{name:'Save weight'}));await screen.findByText('Weight saved. Existing activity records keep their saved estimates.');expect((screen.getByLabelText('Display name') as HTMLInputElement).value).toBe('Unsaved name');vi.mocked(api.updateSettings).mockRejectedValue(new ApiRequestError({message:'Reload the latest settings.',code:'VERSION_CONFLICT',status:409,outcome:'rejected',writeAttempted:true}));fireEvent.click(screen.getByRole('button',{name:'Save settings'}));await screen.findByRole('button',{name:'Check saved settings'});expect(api.updateSettings).toHaveBeenCalledWith(expect.objectContaining({expectedProfileVersion:1,displayName:'Unsaved name'}));fireEvent.click(screen.getByRole('button',{name:'Check saved settings'}));await screen.findByRole('button',{name:'Load latest saved settings'});expect((screen.getByLabelText('Display name') as HTMLInputElement).value).toBe('Unsaved name');fireEvent.click(screen.getByRole('button',{name:'Load latest saved settings'}));expect((screen.getByLabelText('Display name') as HTMLInputElement).value).toBe('Synthetic')
  })
  it('same-date conflict requires explicit review before sending the current version',async()=>{
    vi.mocked(weightApi.upsertWeight).mockRejectedValueOnce(new ApiRequestError({message:'Review the saved measurement.',code:'VERSION_CONFLICT',status:409,outcome:'rejected',writeAttempted:true,currentItem:weight}));mount();await ready();fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.click(screen.getByRole('button',{name:'Save weight'}));await screen.findByRole('dialog',{name:"Review this day's saved weight"});expect(weightApi.upsertWeight).toHaveBeenCalledOnce();fireEvent.click(screen.getByRole('button',{name:'Confirm correction'}));await screen.findByText('Weight saved. Existing activity records keep their saved estimates.');expect(weightApi.upsertWeight).toHaveBeenNthCalledWith(1,expect.objectContaining({expectedVersion:null}));expect(weightApi.upsertWeight).toHaveBeenNthCalledWith(2,expect.objectContaining({expectedVersion:0,weightKg:71.25}));expect(api.updateSettings).not.toHaveBeenCalled()
  })
  it('selecting a history measurement carries its version and invalid weight never dispatches',async()=>{
    mount();await ready();fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'0'}});fireEvent.click(screen.getByRole('button',{name:'Save weight'}));await screen.findByText('Enter 20 to 400 kg with up to 2 decimal places and a valid non-future date.');expect(weightApi.upsertWeight).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:`Correct ${today}`}));fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.click(screen.getByRole('button',{name:'Save correction'}));await screen.findByText('Weight saved. Existing activity records keep their saved estimates.');expect(weightApi.upsertWeight).toHaveBeenCalledWith(expect.objectContaining({recordDate:today,expectedVersion:0}))
  })
  it('unknown weight outcome is checked by GET without repeating the save',async()=>{
    vi.mocked(weightApi.upsertWeight).mockRejectedValue(new ApiRequestError({message:'Unknown save result.',code:'REQUEST_TIMEOUT',outcome:'unknown',writeAttempted:true}));const client=new QueryClient({defaultOptions:{queries:{retry:false}}}),estimateKey=['owner',user.id,1,'exercise-estimate','synthetic'];mount(undefined,client);await ready();client.setQueryData(estimateKey,{snapshot:'old dated weight'});fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.click(screen.getByRole('button',{name:'Save weight'}));await screen.findByRole('button',{name:'Check saved weight'});vi.mocked(weightApi.fetchWeights).mockResolvedValue([{...weight,weightKg:71.25,version:1}]);fireEvent.click(screen.getByRole('button',{name:'Check saved weight'}));await screen.findByText('The saved measurement is confirmed. No second save was sent.');expect(weightApi.upsertWeight).toHaveBeenCalledOnce();expect(client.getQueryState(estimateKey)?.isInvalidated).toBe(true)
  })
  it('saving settings does not clear another dirty weight form navigation warning',async()=>{
    mount();await ready();fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Saved new name'}});fireEvent.click(screen.getByRole('button',{name:'Save settings'}));await screen.findByText('Your profile is saved.');fireEvent.click(screen.getByRole('link',{name:'History'}));await screen.findByRole('dialog',{name:'Discard unsaved changes?'});fireEvent.click(screen.getByRole('button',{name:'Cancel'}));expect((screen.getByLabelText('Weight (kg)') as HTMLInputElement).value).toBe('71.25');expect(weightApi.upsertWeight).not.toHaveBeenCalled()
  })
  it('failed history request shows an error and retry rather than invented empty rows',async()=>{
    vi.mocked(weightApi.fetchWeights).mockRejectedValue(new Error('Synthetic outage'));mount();await screen.findByRole('button',{name:'Retry weight history'});expect(screen.getByText('Weight history could not be loaded.')).toBeTruthy();expect(screen.queryByText('No weights recorded in this date range.')).toBeNull();expect(screen.queryByRole('button',{name:`Correct ${today}`})).toBeNull()
  })
  it('failure in local refresh after a verified write never offers another save as if it failed',async()=>{
    let instance:SessionController|undefined;mount(actions=>{instance=actions});await ready();if(!instance) throw Error('Expected session controller');const cleanup=vi.spyOn(instance,'clearWeightRecovery').mockImplementationOnce(()=>{throw Error('Synthetic stale local module')});fireEvent.change(screen.getByLabelText('Weight (kg)'),{target:{value:'71.25'}});fireEvent.click(screen.getByRole('button',{name:'Save weight'}));await screen.findByText('Weight saved. Existing activity records keep their saved estimates.');await screen.findByText('Weight was saved, but the latest profile could not be refreshed. Retry the profile read before changing settings.');expect((screen.getByLabelText('Weight (kg)') as HTMLInputElement).value).toBe('');expect(weightApi.upsertWeight).toHaveBeenCalledOnce();cleanup.mockRestore()
  })

})
