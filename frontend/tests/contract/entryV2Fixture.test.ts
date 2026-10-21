import {describe,expect,it} from 'vitest'
import {FoodBatchV2RequestSchema,SubmissionReceiptV2Schema} from '../../src/api/schemas'
import {foodBatchV2Fixture,receiptV2Fixture} from '../fixtures/entryV2'
describe('UX-Q00 synthetic fixture contracts',()=>{
 it('accepts exact one-third input and typed proof with numeric IDs shared across tables',()=>{
  const request=FoodBatchV2RequestSchema.parse(foodBatchV2Fixture()),proof=SubmissionReceiptV2Schema.parse(receiptV2Fixture())
  expect(request.entries[0].kind).toBe('DISH')
  if(request.entries[0].kind==='DISH')expect(request.entries[0].portion).toEqual({kind:'FRACTION',numerator:1,denominator:3})
  expect(proof.roots[0].id).toBe(proof.leaves[0].id)
  expect(proof.leaves.map(l=>l.rootClientEntryId)).toEqual([proof.roots[0].clientEntryId,proof.roots[0].clientEntryId])
 })
 it('does not share mutable recipe inputs or proofs between tests',()=>{
  const first=foodBatchV2Fixture();first.entries.length=0
  expect(foodBatchV2Fixture().entries).toHaveLength(1)
  expect(receiptV2Fixture().leaves).toHaveLength(2)
 })
})
