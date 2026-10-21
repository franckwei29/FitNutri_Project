import {test} from '@playwright/test'
import assert from 'node:assert/strict'
import {registerAndLogin,completeQuestionnaire,enterTwoFoodsForReview,confirmToUnknown,assertFrozenNavigation,recoverOriginalReceipt} from './browser-journeys.mjs'
import {assertE2EEnvironment,syntheticIdentity,inspectOwnedSyntheticData,acquireSyntheticOwnerLock,assertOriginalPostIsWaiting} from './owned-db-verification.mjs'
test.beforeAll(()=>{assertE2EEnvironment()})
test('T-IDEM-04: real committed lost response recovers the original two-food receipt once',async({page,request})=>{
  const identity=syntheticIdentity('q02-drop')
  await registerAndLogin(page,identity);await completeQuestionnaire(page)
  const setup=inspectOwnedSyntheticData(identity.email);assert.equal(setup.foodRecords.length,0);assert.equal(setup.receipts.length,0);assert.equal(setup.weights.length,1);assert.equal(setup.profile.qaConsentGranted,false);assert.equal(setup.profile.goalType,null)
  await enterTwoFoodsForReview(page)
  assert.equal(inspectOwnedSyntheticData(identity.email).foodRecords.length,0,'Review must not write records')
  const arm=await request.post((process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:18082')+'/__test__/control',{data:{action:'DROP_NEXT_ACK',path:'/api/v2/food/batches'}});assert.equal(arm.status(),200)
  const unknown=await confirmToUnknown(page);await assertFrozenNavigation(page,unknown.submissionId)
  const committed=inspectOwnedSyntheticData(identity.email);assert.equal(committed.foodRecords.length,2);assert.equal(committed.receipts.length,1);assert.equal(committed.receipts[0].submissionId,unknown.submissionId)
  const result=await recoverOriginalReceipt(page,unknown.submissionId);assert.equal(result.recovered,true)
  const after=inspectOwnedSyntheticData(identity.email);assert.deepEqual(after.foodRecords,committed.foodRecords);assert.deepEqual(after.receipts,committed.receipts)
  const status=await(await request.get((process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:18082')+'/__test__/status')).json() as {attempts:{submissionId:string;dropped:boolean;status:number;clientEntryIds:string[];ackRecordIds:string[]}[]};const attempts=status.attempts.filter(x=>x.submissionId===unknown.submissionId);assert.equal(attempts.length,1);assert.equal(attempts[0].status,201);assert.equal(attempts[0].dropped,true);assert.deepEqual(new Set(attempts[0].ackRecordIds),new Set(after.foodRecords.map(x=>x.id)));assert.deepEqual(new Set(attempts[0].clientEntryIds),new Set(after.foodRecords.map(x=>x.clientEntryId)))
})

test('T-IDEM-05: receipt 404 while original POST waits never creates another batch',async({page,request})=>{
  const identity=syntheticIdentity('q02-late')
  await registerAndLogin(page,identity);await completeQuestionnaire(page);await enterTwoFoodsForReview(page)
  const before=inspectOwnedSyntheticData(identity.email);assert.equal(before.foodRecords.length,0);assert.equal(before.receipts.length,0)
  const lock=await acquireSyntheticOwnerLock(identity.email)
  let unknown:{submissionId:string}|undefined
  try {
    unknown=await confirmToUnknown(page)
    assertOriginalPostIsWaiting(lock.pid)
    await page.getByRole('button',{name:'Check original receipt',exact:true}).click()
    await page.getByText('No committed receipt is visible yet. The original request may still be running. Keep its original ID and payload.',{exact:true}).waitFor({state:'visible'})
    assertOriginalPostIsWaiting(lock.pid);assert.deepEqual(inspectOwnedSyntheticData(identity.email),before)
    await assertFrozenNavigation(page,unknown.submissionId)
  } finally {await lock.release()}
  assert.ok(unknown)
  // Poll only the real transaction's own receipt, never submit a replacement POST.
  await test.expect.poll(()=>inspectOwnedSyntheticData(identity.email).receipts.length,{timeout:15000}).toBe(1)
  const committed=inspectOwnedSyntheticData(identity.email);assert.equal(committed.foodRecords.length,2);assert.equal(committed.receipts[0].submissionId,unknown.submissionId)
  await recoverOriginalReceipt(page,unknown.submissionId);assert.deepEqual(inspectOwnedSyntheticData(identity.email),committed)
  const status=await(await request.get((process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:18082')+'/__test__/status')).json() as {attempts:{submissionId:string;status:number;clientGone:boolean}[]}
  const attempts=status.attempts.filter(x=>x.submissionId===unknown.submissionId);assert.equal(attempts.length,1);assert.equal(attempts[0].status,201);assert.equal(attempts[0].clientGone,true)
})
