import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet,apiPost} from '../../src/api/client'
import {createRecords,fetchSubmissionReceipt} from '../../src/api/submissions'
import {ApiRequestError} from '../../src/api/errors'
import {foodDraft,foodPayload,receipt} from '../fixtures/entry'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F10 creation and minimal receipt API',()=>{
 it('posts canonical inputs and reads the original immutable submission receipt',async()=>{
  const payload=foodPayload(foodDraft());vi.mocked(apiPost).mockResolvedValue(receipt(payload));await createRecords(payload)
  expect(apiPost).toHaveBeenCalledExactlyOnceWith('/records/food/batch',payload)
  vi.mocked(apiGet).mockResolvedValue(receipt(payload));await fetchSubmissionReceipt(payload.submissionId)
  expect(apiGet).toHaveBeenCalledExactlyOnceWith('/submissions/'+payload.submissionId,{signal:undefined})
 })
 it('does not dispatch malformed input and treats malformed successful writes as unknown',async()=>{
  const payload=foodPayload(foodDraft());await expect(createRecords({...payload,items:[]})).rejects.toMatchObject({outcome:'rejected',writeAttempted:false});expect(apiPost).not.toHaveBeenCalled()
  vi.mocked(apiPost).mockResolvedValue({});await expect(createRecords(payload)).rejects.toBeInstanceOf(ApiRequestError);await expect(createRecords(payload)).rejects.toMatchObject({outcome:'unknown',writeAttempted:true})
 })
})
