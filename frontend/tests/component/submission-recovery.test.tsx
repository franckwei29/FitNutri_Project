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
import {useSubmitDraft} from '../../src/hooks/useSubmitDraft'
import {createRecords,fetchSubmissionReceipt} from '../../src/api/submissions'
import {ApiRequestError} from '../../src/api/errors'
import {prepareSubmission} from '../../src/features/entry/submissionPayload'
import {foodDraft,foodPayload,receipt} from '../fixtures/entry'
import {profile,user} from '../fixtures/profile'
import type {SubmissionReceipt} from '../../src/api/contracts'
vi.mock('../../src/api/submissions',()=>({createRecords:vi.fn(),fetchSubmissionReceipt:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
async function setup(){
 const client=createQueryClient(),draft=foodDraft(user.id),payload=foodPayload(draft),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
 function Seed({children}:{children:ReactNode}){const {dispatch}=useDraft();useEffect(()=>{dispatch({type:'START_NEW',ownerId:user.id,draft})},[dispatch]);return children}
 function Wrapper({children}:{children:ReactNode}){return <QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><Seed>{children}</Seed></DraftProvider></SessionContext.Provider></QueryClientProvider>}
 const hook=renderHook(()=>({submission:useSubmitDraft(),draft:useDraft()}),{wrapper:Wrapper});await waitFor(()=>expect(hook.result.current.draft.state.draft).not.toBeNull());return {hook,client,actions,payload,draft}
}
describe('F10 frozen submission and recovery',()=>{
 it('synchronously rejects double confirmation and clears only after matching proof',async()=>{
  let resolve!:(r:SubmissionReceipt)=>void;vi.mocked(createRecords).mockImplementation(()=>new Promise(r=>{resolve=r}));const {hook,payload}=await setup()
  act(()=>{void hook.result.current.submission.confirm(payload,0);void hook.result.current.submission.confirm(payload,0)});expect(createRecords).toHaveBeenCalledOnce();expect(hook.result.current.draft.state.submission.status).toBe('saving')
  await act(async()=>{resolve(receipt(payload))});expect(hook.result.current.draft.state.draft).toBeNull();expect(hook.result.current.draft.state.submission.status).toBe('saved')
 })
 it('keeps original IDs/payload frozen after lost response and receipt404, and retries exactly that payload',async()=>{
  vi.mocked(createRecords).mockRejectedValueOnce(new ApiRequestError({message:'Response lost',code:'REQUEST_TIMEOUT',outcome:'unknown',writeAttempted:true}));const {hook,payload}=await setup();await act(async()=>{await hook.result.current.submission.confirm(payload,0)})
  expect(hook.result.current.draft.state.submission.status).toBe('unknown');vi.mocked(fetchSubmissionReceipt).mockRejectedValue(new ApiRequestError({message:'Not yet observed',code:'NOT_FOUND',status:404,outcome:'read-failed',writeAttempted:false}))
  await act(async()=>{await hook.result.current.submission.recover()});expect(hook.result.current.draft.state.submission.status).toBe('unknown');expect(hook.result.current.submission.message).toContain('may still be running')
  vi.mocked(createRecords).mockResolvedValue(receipt(payload));await act(async()=>{await hook.result.current.submission.retry()});expect(createRecords).toHaveBeenCalledTimes(2);expect(createRecords).toHaveBeenNthCalledWith(2,payload);expect(hook.result.current.draft.state.submission.status).toBe('saved')
 })
 it('recovers a committed lost response without a second POST and keeps saved status if refresh fails',async()=>{
  vi.mocked(createRecords).mockRejectedValue(new ApiRequestError({message:'Lost',code:'NETWORK_ERROR',outcome:'unknown',writeAttempted:true}));const {hook,payload,client}=await setup();await act(async()=>{await hook.result.current.submission.confirm(payload,0)})
  vi.spyOn(client,'invalidateQueries').mockRejectedValueOnce(new Error('Read failed'));vi.mocked(fetchSubmissionReceipt).mockResolvedValue(receipt(payload));await act(async()=>{await hook.result.current.submission.recover()})
  expect(createRecords).toHaveBeenCalledOnce();expect(hook.result.current.draft.state.draft).toBeNull();expect(hook.result.current.draft.state.submission).toMatchObject({status:'saved',refreshFailed:true})
  await act(async()=>{await hook.result.current.submission.retryRefresh()});expect(hook.result.current.draft.state.submission).toMatchObject({status:'saved',refreshFailed:false});expect(createRecords).toHaveBeenCalledOnce()
 })
 it('keeps both food rows on known rejection and never auto-retries a different submission',async()=>{
  vi.mocked(createRecords).mockRejectedValue(new ApiRequestError({message:'Second food unavailable',code:'REFERENCE_INACTIVE',status:409,outcome:'rejected',writeAttempted:true}));const {hook,payload,draft}=await setup();await act(async()=>{await hook.result.current.submission.confirm(payload,0)})
  expect(hook.result.current.draft.state.submission.status).toBe('rejected');expect(hook.result.current.draft.state.draft).toEqual(draft);expect(createRecords).toHaveBeenCalledOnce()
 })
 it('rejects mismatched receipt proof and ignores late completion after session expiry',async()=>{
  vi.mocked(createRecords).mockResolvedValue({submissionId:'invalid',kind:'FOOD_BATCH',committedAt:'2026-10-16T01:00:00Z',replayed:false,records:[]});const {hook,payload,actions,client}=await setup();await act(async()=>{await hook.result.current.submission.confirm(payload,0)});expect(hook.result.current.draft.state.submission.status).toBe('unknown')
  let resolve!:(r:SubmissionReceipt)=>void;vi.mocked(fetchSubmissionReceipt).mockImplementation(()=>new Promise(r=>{resolve=r}));const invalidate=vi.spyOn(client,'invalidateQueries');let pending!:Promise<void>;act(()=>{pending=hook.result.current.submission.recover()});actions.expire();await act(async()=>{resolve(receipt(payload));await pending});expect(invalidate).not.toHaveBeenCalled();expect(hook.result.current.draft.state.submission.status).toBe('unknown')
 })
 it('builds only input fields and refuses to silently dispatch an edit through POST',async()=>{
  const {draft}=await setup();const prepared=prepareSubmission(draft,user.id);expect(prepared).toMatchObject({recordDate:draft.recordDate,items:expect.any(Array)});expect(prepared).not.toHaveProperty('nutrients');expect(prepared).not.toHaveProperty('ownerId');expect(()=>prepareSubmission({...draft,edit:{recordId:'1',expectedVersion:0,originalRecordDate:draft.recordDate}},user.id)).toThrow('Existing records require the correction flow')
 })
})
