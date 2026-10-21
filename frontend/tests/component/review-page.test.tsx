import {ApiRequestError} from '../../src/api/errors'
import {UnsavedChangesGuard} from '../../src/components/UnsavedChangesGuard'
import {hasBusinessDraft} from '../../src/features/entry/draftBusinessInput'
import {useEffect} from 'react'
import type {ReactNode} from 'react'
import {act,fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider,Outlet} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {useDraft} from '../../src/hooks/useDraft'
import {ReviewPage} from '../../src/features/entry/ReviewPage'
import {fetchCapabilities} from '../../src/api/capabilities'
import {previewFoodV2} from '../../src/api/foodEntriesV2'
import {createRecordsV2} from '../../src/api/submissionsV2'
import {foodDraft} from '../fixtures/entry'
import {profile,user} from '../fixtures/profile'
import type {SubmissionReceiptV2} from '../../src/api/contracts'
vi.mock('../../src/api/capabilities',()=>({fetchCapabilities:vi.fn()}))
vi.mock('../../src/api/foodEntriesV2',()=>({previewFoodV2:vi.fn()}))
vi.mock('../../src/api/submissionsV2',async original=>({...await original<typeof import('../../src/api/submissionsV2')>(),createRecordsV2:vi.fn(),fetchSubmissionV2:vi.fn()}))
const feature={configured:false,available:false,reason:'Unconfigured'}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(fetchCapabilities).mockResolvedValue({schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'test',catalogues:{food:{ready:true,verified:true,reason:null},exercise:{ready:true,verified:true,reason:null}},features:{image:feature,transcription:feature,speechParsing:feature,qa:feature}})})
async function mount(){const client=createQueryClient(),draft=foodDraft(user.id),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,profileVersion:1,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ'}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
 vi.mocked(previewFoodV2).mockImplementation(async input=>{const nutrients={energyKj:988.5,proteinGrams:4.65,fatGrams:0.3,carbsGrams:52.05};return {entries:input.entries.map(root=>{if(root.kind!=='SINGLE')throw Error('Fixture');return {kind:'SINGLE',clientEntryId:root.clientEntryId,leaves:[{clientEntryId:root.clientEntryId,referenceId:root.referenceId,inputGrams:root.quantityGrams,consumedGrams:root.quantityGrams,nutrients}],totals:nutrients}}),totals:{energyKj:1977,proteinGrams:9.3,fatGrams:0.6,carbsGrams:104.1},fingerprint:'a'.repeat(64),inputSignature:'b'.repeat(64),calculationPolicy:'SYNTHETIC_TEST_ONLY'}})

 function Seed({children}:{children:ReactNode}){const {dispatch,state}=useDraft();useEffect(()=>{dispatch({type:'START_NEW',ownerId:user.id,draft})},[dispatch]);return <>{children}<output data-testid="state">{JSON.stringify(state)}</output></>}
 function Guarded(){const {state}=useDraft();return <><Outlet /><UnsavedChangesGuard dirty={hasBusinessDraft(state.draft)} pending={state.submission.status==='saving'||state.submission.status==='unknown'?state.submission.status:null} entryFlow onDiscard={()=>{}} /></>}
 const router=createMemoryRouter([{element:<Guarded />,children:[{path:'/review',element:<ReviewPage />},{path:'/dashboard',element:<p>Destination dashboard</p>},{path:'/entry',element:<p>Back to entry</p>},{path:'/add',element:<p>Start a record</p>}]}],{initialEntries:['/review']})
 render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><Seed><RouterProvider router={router} /></Seed></DraftProvider></SessionContext.Provider></QueryClientProvider>);return {router,draft,actions}}
describe('F10 real review page wiring',()=>{
 it('shows both meals and authoritative nutrition before confirmation; cancellation writes nothing',async()=>{
  await mount();await screen.findByRole('button',{name:'Confirm & save'});await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false))
  expect(screen.getByRole('heading',{name:'Lunch'})).toBeTruthy();expect(screen.getByRole('heading',{name:'Dinner'})).toBeTruthy();expect(screen.getAllByText('989 kJ')).toHaveLength(2)
  fireEvent.click(screen.getByRole('button',{name:'Cancel entry'}));await screen.findByRole('dialog',{name:'Discard this unsubmitted entry?'});fireEvent.click(screen.getByRole('button',{name:'Discard entry'}));await screen.findByText('Destination dashboard');expect(createRecordsV2).not.toHaveBeenCalled()
 })
 it('keeps controls frozen during POST and shows only verified saved proof afterwards',async()=>{
  let resolve!:(value:SubmissionReceiptV2)=>void;vi.mocked(createRecordsV2).mockImplementation(()=>new Promise(r=>{resolve=r}));const {router}=await mount();await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false))
  fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));expect(createRecordsV2).toHaveBeenCalledOnce();expect((screen.getByRole('button',{name:'Back to entry'}) as HTMLButtonElement).disabled).toBe(true);expect((screen.getByLabelText('Portion in grams 1') as HTMLInputElement).disabled).toBe(true)
  const envelope=vi.mocked(createRecordsV2).mock.calls[0][0];if(envelope.domain!=='FOOD')throw Error('Fixture');const payload=envelope.payload,proof:SubmissionReceiptV2={schemaVersion:2,submissionId:payload.submissionId,kind:'FOOD_BATCH_V2',committedAt:'2026-10-16T01:00:00Z',replayed:false,roots:payload.entries.map((root,index)=>({kind:root.kind,id:String(index+1),clientEntryId:root.clientEntryId,recordDate:payload.recordDate,versionAtCreation:0})),leaves:payload.entries.map((root,index)=>({kind:'FOOD_COMPONENT',id:String(index+1),clientEntryId:root.clientEntryId,rootClientEntryId:root.clientEntryId,recordDate:payload.recordDate,versionAtCreation:0}))};await act(async()=>{resolve(proof)});await screen.findByText('Destination dashboard');expect(router.state.location.pathname).toBe('/dashboard');const state=JSON.parse(screen.getByTestId('state').textContent!);expect(state.lastSaved.receipt).toEqual(proof);expect(state.lastSaved.receipt.roots).toHaveLength(2);expect(state.draft.entries).toEqual([]);expect(screen.queryByRole('button',{name:'Confirm & save'})).toBeNull()

 })
})

it('never navigates for a 2xx body whose receipt omits the reviewed roots',async()=>{vi.mocked(createRecordsV2).mockImplementation(async envelope=>({schemaVersion:2,submissionId:envelope.payload.submissionId,kind:'FOOD_BATCH_V2',committedAt:'2026-10-18T00:00:00Z',replayed:false,roots:[],leaves:[]}));const {router}=await mount();await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));await screen.findByRole('button',{name:'Check original receipt'});expect(router.state.location.pathname).toBe('/review');const state=JSON.parse(screen.getByTestId('state').textContent!);expect(state.submission.status).toBe('unknown');expect(state.lastSaved).toBeUndefined();expect(state.draft.entries).toHaveLength(2)})
it('retains rejected input without producing saved feedback or leaving Review',async()=>{vi.mocked(createRecordsV2).mockRejectedValue(new ApiRequestError({message:'Reference inactive',code:'REFERENCE_INACTIVE',status:409,outcome:'rejected',writeAttempted:true}));const {router}=await mount();await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));await waitFor(()=>expect(JSON.parse(screen.getByTestId('state').textContent!).submission.status).toBe('rejected'));expect(router.state.location.pathname).toBe('/review');expect(JSON.parse(screen.getByTestId('state').textContent!).lastSaved).toBeUndefined();expect(JSON.parse(screen.getByTestId('state').textContent!).draft.entries).toHaveLength(2)})
