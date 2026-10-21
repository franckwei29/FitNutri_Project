import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import {useEffect} from 'react'
import type {ReactNode} from 'react'
import {act,renderHook,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {DraftProvider} from '../../src/app/DraftProvider'
import {SessionContext} from '../../src/app/SessionContext'
import {SessionController} from '../../src/app/sessionEpoch'
import {createQueryClient} from '../../src/app/queryClient'
import {useDraft} from '../../src/hooks/useDraft'
import {useRecordMutations} from '../../src/hooks/useRecordMutations'
import {loadEditDraft,prepareCorrection} from '../../src/features/entry/correctionIntent'
import {updateFoodRecord,deleteRecord} from '../../src/api/recordMutations'
import {fetchFoodRecord} from '../../src/api/records'
import {ApiRequestError} from '../../src/api/errors'
import {savedFood} from '../fixtures/records'
import {profile,user} from '../fixtures/profile'
import type {FoodRecord} from '../../src/api/contracts'
vi.mock('../../src/api/recordMutations',()=>({updateFoodRecord:vi.fn(),updateExerciseRecord:vi.fn(),deleteRecord:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecord:vi.fn(),fetchExerciseRecord:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
async function setup(edit=true){const client=createQueryClient(),record=savedFood(),draft=loadEditDraft(user.id,'food',record);if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].mealType='DINNER';const intent=prepareCorrection(draft,user.id),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
 function Seed({children}:{children:ReactNode}){const {dispatch}=useDraft();useEffect(()=>{if(edit)dispatch({type:'LOAD_EDIT',ownerId:user.id,draft})},[dispatch]);return children}
 function Wrapper({children}:{children:ReactNode}){return <QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><Seed>{children}</Seed></DraftProvider></SessionContext.Provider></QueryClientProvider>}
 const hook=renderHook(()=>({mutation:useRecordMutations(),second:useRecordMutations(),draft:useDraft()}),{wrapper:Wrapper});if(edit)await waitFor(()=>expect(hook.result.current.draft.state.draft).not.toBeNull());return {hook,client,record,intent,actions}}
const lost=()=>new ApiRequestError({message:'Response lost',code:'NETWORK_ERROR',outcome:'unknown',writeAttempted:true})
describe('F13 versioned mutation and observed-state recovery',()=>{
 it('locks duplicate updates and clears the draft only after matching canonical acknowledgement',async()=>{
  let resolve!:(row:FoodRecord)=>void;vi.mocked(updateFoodRecord).mockImplementation(()=>new Promise(r=>{resolve=r}));const {hook,intent}=await setup();act(()=>{void hook.result.current.mutation.update(intent,0);void hook.result.current.mutation.update(intent,0)});expect(updateFoodRecord).toHaveBeenCalledOnce();expect(hook.result.current.draft.state.recordMutation.status).toBe('saving')
  await act(async()=>{resolve(savedFood({version:3,mealType:'DINNER'}))});expect(hook.result.current.draft.state.draft).toBeNull();expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',result:'UPDATED'});expect(hook.result.current.draft.state.submission.status).toBe('idle')
 })
 it('recovers matching later state without another PUT and without claiming writer attribution',async()=>{
  vi.mocked(updateFoodRecord).mockRejectedValue(lost());const {hook,intent}=await setup();await act(async()=>{await hook.result.current.mutation.update(intent,0)});vi.mocked(fetchFoodRecord).mockResolvedValue(savedFood({version:5,mealType:'DINNER'}));await act(async()=>{await hook.result.current.mutation.recover()});expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',result:'MATCHES'});expect(updateFoodRecord).toHaveBeenCalledOnce()
 })
 it('retains original expectedVersion when a read is still old and retries exactly the frozen patch',async()=>{
  vi.mocked(updateFoodRecord).mockRejectedValueOnce(lost());const {hook,intent}=await setup();await act(async()=>{await hook.result.current.mutation.update(intent,0)});vi.mocked(fetchFoodRecord).mockResolvedValue(savedFood());await act(async()=>{await hook.result.current.mutation.recover()});expect(hook.result.current.draft.state.recordMutation.status).toBe('unknown')
  vi.mocked(updateFoodRecord).mockResolvedValue(savedFood({version:3,mealType:'DINNER'}));await act(async()=>{await hook.result.current.mutation.retry()});expect(updateFoodRecord).toHaveBeenNthCalledWith(2,intent.recordId,intent.body);expect(hook.result.current.draft.state.recordMutation.status).toBe('saved')
 })
 it('keeps a conflicting later record for explicit review and never silently promotes its version',async()=>{
  vi.mocked(updateFoodRecord).mockRejectedValue(lost());const {hook,intent}=await setup();await act(async()=>{await hook.result.current.mutation.update(intent,0)});vi.mocked(fetchFoodRecord).mockResolvedValue(savedFood({version:4,mealType:'SNACK'}));await act(async()=>{await hook.result.current.mutation.recover()});expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'rejected',error:{code:'VERSION_CONFLICT'},observedRecord:{version:4}});expect(hook.result.current.draft.state.draft?.edit?.expectedVersion).toBe(2);expect(updateFoodRecord).toHaveBeenCalledOnce()
 })
 it('uses prior owned read context for uncertain DELETE and reports current unavailability instead of attribution',async()=>{
  vi.mocked(deleteRecord).mockRejectedValue(lost());const {hook,record}=await setup(false);await act(async()=>{await hook.result.current.mutation.remove('food',record)});vi.mocked(fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Unavailable',status:404,code:'NOT_FOUND',outcome:'read-failed',writeAttempted:false}));await act(async()=>{await hook.result.current.mutation.recover()});expect(deleteRecord).toHaveBeenCalledExactlyOnceWith('food',record.id,record.version);expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',result:'UNAVAILABLE'});expect(hook.result.current.draft.state.submission.status).toBe('idle')
 })
 it('rejects mismatched acknowledgement and ignores late completion after account expiry',async()=>{
  vi.mocked(updateFoodRecord).mockResolvedValue(savedFood({version:3,mealType:'DINNER',source:'VOICE'}));const {hook,intent,actions,client}=await setup();await act(async()=>{await hook.result.current.mutation.update(intent,0)});expect(hook.result.current.draft.state.recordMutation.status).toBe('unknown');expect(hook.result.current.draft.state.draft).not.toBeNull()
  let resolve!:(row:FoodRecord)=>void;vi.mocked(fetchFoodRecord).mockImplementation(()=>new Promise(r=>{resolve=r}));const invalidate=vi.spyOn(client,'invalidateQueries');let pending!:Promise<void>;act(()=>{pending=hook.result.current.mutation.recover()});actions.expire();await act(async()=>{resolve(savedFood({version:3,mealType:'DINNER'}));await pending});expect(invalidate).not.toHaveBeenCalled();expect(hook.result.current.draft.state.draft).not.toBeNull()
 })
 it('keeps successful mutation status after refresh failure and retries reads only',async()=>{
  vi.mocked(updateFoodRecord).mockResolvedValue(savedFood({version:3,mealType:'DINNER'}));const {hook,intent,client}=await setup();vi.spyOn(client,'invalidateQueries').mockRejectedValueOnce(new Error('Read failed'));await act(async()=>{await hook.result.current.mutation.update(intent,0)});expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',refreshFailed:true});await act(async()=>{await hook.result.current.mutation.retryRefresh()});expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',refreshFailed:false});expect(updateFoodRecord).toHaveBeenCalledOnce()
 })
 it('does not interpret an untyped proxy404 as successful deletion',async()=>{
  vi.mocked(deleteRecord).mockRejectedValue(lost());const {hook,record}=await setup(false);await act(async()=>{await hook.result.current.mutation.remove('food',record)});vi.mocked(fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Unexpected HTML response',status:404,code:'UNEXPECTED_RESPONSE',outcome:'read-failed',writeAttempted:false}));await act(async()=>{await hook.result.current.mutation.recover()});expect(hook.result.current.draft.state.recordMutation.status).toBe('unknown');expect(deleteRecord).toHaveBeenCalledOnce()
 })
 it('locks recovery across two consumers of the same frozen operation',async()=>{
  vi.mocked(updateFoodRecord).mockRejectedValue(lost());const {hook,intent}=await setup();await act(async()=>{await hook.result.current.mutation.update(intent,0)})
  let resolve!:(row:FoodRecord)=>void;vi.mocked(fetchFoodRecord).mockImplementation(()=>new Promise(r=>{resolve=r}));let first!:Promise<void>,second!:Promise<void>
  act(()=>{first=hook.result.current.mutation.recover();second=hook.result.current.second.recover()});expect(fetchFoodRecord).toHaveBeenCalledOnce();await act(async()=>{resolve(savedFood({version:3,mealType:'DINNER'}));await Promise.all([first,second])});expect(hook.result.current.draft.state.recordMutation).toMatchObject({status:'saved',result:'MATCHES'})
 })
})
