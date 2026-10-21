import {legacyExerciseDraft} from '../fixtures/exercise'
import {useEffect} from 'react'
import type {ReactNode} from 'react'
import {fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {useDraft} from '../../src/hooks/useDraft'
import {ReviewPage} from '../../src/features/entry/ReviewPage'
import {fetchCapabilities} from '../../src/api/capabilities'
import {previewDatedExercise} from '../../src/api/exerciseTypes'
import {createRecords} from '../../src/api/submissions'
import {ApiRequestError} from '../../src/api/errors'
import {exerciseEstimate,exerciseType} from '../fixtures/exercise'
import {profile,user} from '../fixtures/profile'
import type {ExerciseCreateRequest} from '../../src/api/contracts'
vi.mock('../../src/api/capabilities',()=>({fetchCapabilities:vi.fn()}))
vi.mock('../../src/api/exerciseTypes',()=>({previewDatedExercise:vi.fn()}))
vi.mock('../../src/api/submissions',()=>({createRecords:vi.fn(),fetchSubmissionReceipt:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F10 create estimate re-review',()=>{
 it('shows old/new basis after409, freezes confirmation until reviewed and uses a new submission ID only after known rejection',async()=>{
  const feature={configured:false,available:false,reason:'Not configured'};vi.mocked(fetchCapabilities).mockResolvedValue({schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'r1',catalogues:{food:{ready:true,verified:true,reason:null},exercise:{ready:true,verified:true,reason:null}},features:{image:feature,transcription:feature,speechParsing:feature,qa:feature}})
  let changed=false;const preview=(minutes:number,date:string)=>exerciseEstimate({durationMinutes:minutes,recordDate:date,weightKgUsed:changed?80:71.5,estimatedKj:changed?244.067:218.135,calculationFingerprint:(changed?'b':'a').repeat(64)})
  vi.mocked(previewDatedExercise).mockImplementation(async(_,minutes,date)=>preview(minutes,date));vi.mocked(createRecords).mockImplementationOnce(async()=>{changed=true;throw new ApiRequestError({message:'The activity basis changed.',status:409,code:'CALCULATION_CHANGED',outcome:'rejected',writeAttempted:true,currentPreview:preview(12.5,'2026-10-16')})}).mockImplementationOnce(async payload=>{if('items' in payload)throw Error('Exercise fixture');return {submissionId:payload.submissionId,kind:'EXERCISE_CREATE',committedAt:'2026-10-17T01:00:00Z',replayed:false,records:[{id:'501',clientEntryId:payload.clientEntryId,recordDate:payload.recordDate,versionAtCreation:0}]}})
  const client=createQueryClient(),draft=legacyExerciseDraft(user.id,'12.5'),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ'}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
  function Seed({children}:{children:ReactNode}){const {dispatch}=useDraft();useEffect(()=>{dispatch({type:'START_NEW',ownerId:user.id,draft})},[dispatch]);return children}
  const router=createMemoryRouter([{path:'/review',element:<ReviewPage />}],{initialEntries:['/review']});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><Seed><RouterProvider router={router} /></Seed></DraftProvider></SessionContext.Provider></QueryClientProvider>)
  await waitFor(()=>expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));await screen.findByText(/Previously reviewed basis: 71.5 kg/);await screen.findByText(/Current server basis: 80 kg/);expect((screen.getByRole('button',{name:'Confirm & save'}) as HTMLButtonElement).disabled).toBe(true);expect(createRecords).toHaveBeenCalledOnce();fireEvent.click(screen.getByLabelText('I reviewed the changed exercise estimate.'));fireEvent.click(screen.getByRole('button',{name:'Confirm & save'}));await screen.findByRole('heading',{name:'Your record is saved.'})
  const first=vi.mocked(createRecords).mock.calls[0][0],second=vi.mocked(createRecords).mock.calls[1][0];expect(second.submissionId).not.toBe(first.submissionId);expect(second).toMatchObject({clientEntryId:draft.clientEntryId,calculationFingerprint:'b'.repeat(64)});expect(second).not.toHaveProperty('reviewedEstimate')
 })
 it('shows the originally reviewed basis while the request is unknown instead of silently displaying a newer weight',async()=>{
  const feature={configured:false,available:false,reason:'Not configured'};vi.mocked(fetchCapabilities).mockResolvedValue({schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'r1',catalogues:{food:{ready:true,verified:true,reason:null},exercise:{ready:true,verified:true,reason:null}},features:{image:feature,transcription:feature,speechParsing:feature,qa:feature}});vi.mocked(previewDatedExercise).mockResolvedValue(exerciseEstimate({durationMinutes:12.5,weightKgUsed:99,calculationFingerprint:'b'.repeat(64)}))
  const client=createQueryClient(),draft=legacyExerciseDraft(user.id,'12.5'),estimate=exerciseEstimate({durationMinutes:12.5}),payload:ExerciseCreateRequest={submissionId:'27d4db90-dc34-4010-a2dc-eaf902c74181',clientEntryId:draft.clientEntryId,exerciseTypeId:exerciseType.id,durationMinutes:12.5,recordDate:draft.recordDate,zone:'Australia/Sydney',source:'MANUAL',calculationFingerprint:estimate.calculationFingerprint}
  const actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true,energyUnit:'KJ'}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
  function Seed({children}:{children:ReactNode}){const {dispatch,beginSubmission}=useDraft();useEffect(()=>{dispatch({type:'START_NEW',ownerId:user.id,draft});beginSubmission(payload,actions.getSnapshot().epoch,0,estimate);dispatch({type:'SUBMIT_UNKNOWN',ownerId:user.id,ownerEpoch:actions.getSnapshot().epoch,submissionId:payload.submissionId})},[dispatch,beginSubmission]);return children}
  const router=createMemoryRouter([{path:'/review',element:<ReviewPage />}],{initialEntries:['/review']});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><Seed><RouterProvider router={router} /></Seed></DraftProvider></SessionContext.Provider></QueryClientProvider>)
  await screen.findByRole('button',{name:'Check original receipt'});expect(await screen.findByText('Based on 71.5 kg recorded on 2026-10-15; MET 3.5.')).toBeTruthy();expect(screen.queryByText(/Based on 99 kg/)).toBeNull();expect(previewDatedExercise).not.toHaveBeenCalled();expect(createRecords).not.toHaveBeenCalled()
 })
})
