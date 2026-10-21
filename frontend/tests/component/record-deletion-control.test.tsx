import type {ReactNode} from 'react'
import {fireEvent,render,screen,waitFor,act} from '@testing-library/react'
import {QueryClientProvider,QueryObserver} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {DraftProvider} from '../../src/app/DraftProvider'
import {SessionContext} from '../../src/app/SessionContext'
import {SessionController} from '../../src/app/sessionEpoch'
import {createQueryClient} from '../../src/app/queryClient'
import {RecordDeletionControl} from '../../src/components/RecordDeletionControl'
import {RecordMutationBoundary} from '../../src/components/RecordMutationBoundary'
import {deleteRecord} from '../../src/api/recordMutations'
import {fetchFoodRecord} from '../../src/api/records'
import {ApiRequestError} from '../../src/api/errors'
import {savedFood} from '../fixtures/records'
import {profile,user} from '../fixtures/profile'
import {queryKeys} from '../../src/app/queryKeys'
vi.mock('../../src/api/recordMutations',()=>({deleteRecord:vi.fn(),updateFoodRecord:vi.fn(),updateExerciseRecord:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecord:vi.fn(),fetchExerciseRecord:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
async function mount(){const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore();const onLeaveDetails=vi.fn(),onReload=vi.fn();function Wrapper({children}:{children:ReactNode}){return <QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RecordMutationBoundary />{children}</DraftProvider></SessionContext.Provider></QueryClientProvider>};const view=render(<Wrapper><RecordDeletionControl kind="food" record={savedFood()} onLeaveDetails={onLeaveDetails} onReload={onReload} /></Wrapper>);return {view,onLeaveDetails,onReload,Wrapper,client,actions}}
describe('F13 confirmed deletion control',()=>{
 it('asks explicitly and cancellation sends no DELETE',async()=>{await mount();fireEvent.click(screen.getByRole('button',{name:'Delete record'}));await screen.findByRole('dialog',{name:'Delete this saved record?'});expect(deleteRecord).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Cancel'}));await waitFor(()=>expect(screen.queryByRole('dialog')).toBeNull());expect(deleteRecord).not.toHaveBeenCalled()})
 it('uses the captured version once and closes details only after acknowledged deletion',async()=>{
  let resolve!:()=>void;vi.mocked(deleteRecord).mockImplementation(()=>new Promise<void>(r=>{resolve=r}));const {onLeaveDetails}=await mount();fireEvent.click(screen.getByRole('button',{name:'Delete record'}));fireEvent.click(await screen.findByRole('button',{name:'Confirm deletion'}));expect(deleteRecord).toHaveBeenCalledExactlyOnceWith('food',savedFood().id,2);expect(onLeaveDetails).not.toHaveBeenCalled();expect((screen.getByRole('button',{name:'Cancel'}) as HTMLButtonElement).disabled).toBe(true);await act(async()=>{resolve()});await waitFor(()=>expect(onLeaveDetails).toHaveBeenCalledOnce())
 })
 it('keeps a conflict reviewable and reloads only after an explicit action without another DELETE',async()=>{
  vi.mocked(deleteRecord).mockRejectedValue(new ApiRequestError({message:'The record changed. Review the latest saved version.',status:409,code:'VERSION_CONFLICT',outcome:'rejected',writeAttempted:true}));const {onReload,onLeaveDetails}=await mount();fireEvent.click(screen.getByRole('button',{name:'Delete record'}));fireEvent.click(await screen.findByRole('button',{name:'Confirm deletion'}));await screen.findByText('The record changed. Review the latest saved version.');expect(onLeaveDetails).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Reload current saved record'}));await waitFor(()=>expect(onReload).toHaveBeenCalledOnce());expect(deleteRecord).toHaveBeenCalledOnce()
 })
 it('keeps uncertain deletion globally recoverable after closing the details overlay',async()=>{
  vi.mocked(deleteRecord).mockRejectedValue(new ApiRequestError({message:'Response lost',code:'NETWORK_ERROR',outcome:'unknown',writeAttempted:true}));const {onLeaveDetails}=await mount();fireEvent.click(screen.getByRole('button',{name:'Delete record'}));fireEvent.click(await screen.findByRole('button',{name:'Confirm deletion'}));await waitFor(()=>expect(onLeaveDetails).toHaveBeenCalledOnce());vi.mocked(fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Not found',status:404,code:'NOT_FOUND',outcome:'read-failed',writeAttempted:false}));fireEvent.click(await screen.findByRole('button',{name:'Check current record'}));await screen.findByText('The previously read record is no longer visible. This does not prove which request removed it.');expect(deleteRecord).toHaveBeenCalledOnce()
 })
 it.each([{code:'NOT_FOUND',status:404,failed:false},{code:'INTERNAL_ERROR',status:500,failed:true}])('distinguishes expected deleted-detail404 from genuine refresh failure: $code',async({code,status,failed})=>{
  vi.mocked(deleteRecord).mockResolvedValue(undefined);vi.mocked(fetchFoodRecord).mockResolvedValue(savedFood());const {client,actions,onLeaveDetails}=await mount()
  const observer=new QueryObserver(client,{queryKey:queryKeys.record(user.id,actions.getSnapshot().epoch,'food',savedFood().id),queryFn:()=>fetchFoodRecord(savedFood().id),retry:false}),unsubscribe=observer.subscribe(()=>{})
  await waitFor(()=>expect(observer.getCurrentResult().isSuccess).toBe(true));vi.mocked(fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Synthetic detail response',status,code,outcome:'read-failed',writeAttempted:false}))
  fireEvent.click(screen.getByRole('button',{name:'Delete record'}));fireEvent.click(await screen.findByRole('button',{name:'Confirm deletion'}));await waitFor(()=>expect(onLeaveDetails).toHaveBeenCalledOnce());expect(observer.getCurrentResult().isError).toBe(true);expect(!!screen.queryByText('Some views could not refresh. The confirmed state is retained.')).toBe(failed);unsubscribe()
 })
})
