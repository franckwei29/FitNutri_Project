import {beforeEach,describe,expect,it,vi} from 'vitest'
import {createRecordsV2,verifyReceiptV2} from '../../src/api/submissionsV2'
import {apiPost} from '../../src/api/client'
import {foodBatchV2Fixture,receiptV2Fixture} from '../fixtures/entryV2'
vi.mock('../../src/api/client',()=>({apiPost:vi.fn(),apiGet:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('UX-F03 V2 write/proof contract',()=>{
 it('accepts exact typed roots/leaves while allowing equal numeric IDs in different tables',()=>{
  expect(verifyReceiptV2({apiVersion:2,domain:'FOOD',payload:foodBatchV2Fixture()},receiptV2Fixture())).toEqual(receiptV2Fixture())
 })
 it.each(['missing','extra','wrong-parent','wrong-date','wrong-kind','wrong-submission','wrong-version','private-extra'] as const)('rejects %s proof without claiming a committed save',kind=>{
  const proof=receiptV2Fixture()
  if(kind==='missing')proof.leaves.pop()
  if(kind==='extra')proof.leaves.push({...proof.leaves[0],id:'99',clientEntryId:'99999999-9999-4999-8999-999999999999'})
  if(kind==='wrong-parent')proof.leaves[0].rootClientEntryId='99999999-9999-4999-8999-999999999999'
  if(kind==='wrong-date')proof.leaves[0].recordDate='2026-10-16'
  if(kind==='wrong-kind')proof.roots[0].kind='WORKOUT'
  if(kind==='wrong-submission')proof.submissionId='99999999-9999-4999-8999-999999999999'
  if(kind==='wrong-version')Object.assign(proof.leaves[0],{versionAtCreation:1})
  if(kind==='private-extra')Object.assign(proof,{recipe:'must not be in proof'})
  expect(()=>verifyReceiptV2({apiVersion:2,domain:'FOOD',payload:foodBatchV2Fixture()},proof)).toThrow()
 })
 it('uses /v2 under the existing /api base and treats unverified write response as unknown',async()=>{
  vi.mocked(apiPost).mockResolvedValue(receiptV2Fixture())
  const envelope={apiVersion:2 as const,domain:'FOOD' as const,payload:foodBatchV2Fixture()}
  await expect(createRecordsV2(envelope)).resolves.toEqual(receiptV2Fixture())
  expect(apiPost).toHaveBeenCalledWith('/v2/food/batches',envelope.payload)
  vi.mocked(apiPost).mockResolvedValue({})
  await expect(createRecordsV2(envelope)).rejects.toMatchObject({outcome:'unknown',writeAttempted:true})
 })
 it('rejects invalid input before any write',async()=>{
  const payload=foodBatchV2Fixture();payload.entries=[]
  await expect(createRecordsV2({apiVersion:2,domain:'FOOD',payload})).rejects.toMatchObject({outcome:'rejected',writeAttempted:false})
  expect(apiPost).not.toHaveBeenCalled()
 })
})
