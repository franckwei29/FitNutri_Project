import {test,expect} from '@playwright/test'
import assert from 'node:assert/strict'
import {registerAndLogin,completeQuestionnaire,enterTwoFoodsForReview} from './browser-journeys.mjs'
import {confirmFoodWithDoubleClick,loginExisting,enterMissingWeightExerciseReview,confirmExercise,logout} from './core-journeys.mjs'
import {reviewFoodCorrection,confirmCorrection,assertVersionConflict,discardCorrection,reviewExerciseDateCorrection,openRecordDeletion,cancelDeletion,confirmDeletion} from './record-management-journeys.mjs'
import {assertE2EEnvironment,syntheticIdentity,inspectOwnedSyntheticData} from './owned-db-verification.mjs'
test.beforeAll(()=>assertE2EEnvironment())
test('T-EDIT-03: independent real sessions cannot silently overwrite the same food version',async({browser})=>{
  const contextA=await browser.newContext({baseURL:process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173'}),contextB=await browser.newContext({baseURL:process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173'})
  try {
    const a=await contextA.newPage(),b=await contextB.newPage(),identity=syntheticIdentity('q06-conflict')
    await registerAndLogin(a,identity);await completeQuestionnaire(a);await enterTwoFoodsForReview(a);await confirmFoodWithDoubleClick(a)
    const before=inspectOwnedSyntheticData(identity.email),rice=before.foodRecords[0]
    await b.goto('/login');await loginExisting(b,identity)
    await reviewFoodCorrection(a,{id:rice.id,date:rice.recordDate,grams:200})
    await reviewFoodCorrection(b,{id:rice.id,date:rice.recordDate,grams:250})
    assert.deepEqual(inspectOwnedSyntheticData(identity.email),before)
    await confirmCorrection(b);const winner=inspectOwnedSyntheticData(identity.email)
    assert.equal(winner.foodRecords[0].version,1);assert.equal(winner.foodRecords[0].quantityGrams,250);assert.equal(winner.foodRecords[0].energyKj,1647.5);assert.deepEqual(winner.receipts,before.receipts)
    await assertVersionConflict(a);assert.deepEqual(inspectOwnedSyntheticData(identity.email),winner)
    await discardCorrection(a);await expect(a.getByRole('dialog',{name:'Saved record details',exact:true})).toContainText('250 g');assert.deepEqual(inspectOwnedSyntheticData(identity.email),winner)
  } finally {await contextA.close();await contextB.close()}
})

test('T-EDIT-02/T-DELETE-01: dated activity correction and confirmed deletion preserve original proofs',async({page})=>{
  const identity=syntheticIdentity('q06-manage')
  await registerAndLogin(page,identity);await completeQuestionnaire(page);await enterTwoFoodsForReview(page);await confirmFoodWithDoubleClick(page);await enterMissingWeightExerciseReview(page);await confirmExercise(page)
  const before=inspectOwnedSyntheticData(identity.email),exercise=before.exerciseRecords[0],chicken=before.foodRecords[1]
  await reviewExerciseDateCorrection(page,{id:exercise.id,from:exercise.recordDate,to:'2020-01-02'});assert.deepEqual(inspectOwnedSyntheticData(identity.email),before)
  await confirmCorrection(page);const moved=inspectOwnedSyntheticData(identity.email);assert.equal(moved.exerciseRecords[0].recordDate,'2020-01-02');assert.equal(moved.exerciseRecords[0].version,1);assert.equal(moved.exerciseRecords[0].estimatedKj,213.558);assert.equal(moved.exerciseRecords[0].estimateStatus,'AVAILABLE');assert.deepEqual(moved.receipts,before.receipts)
  await openRecordDeletion(page,{id:chicken.id,date:chicken.recordDate,kind:'food'});await cancelDeletion(page);assert.deepEqual(inspectOwnedSyntheticData(identity.email),moved)
  await page.getByRole('button',{name:'Delete record',exact:true}).click();await confirmDeletion(page)
  const deleted=inspectOwnedSyntheticData(identity.email);assert.equal(deleted.foodRecords.length,2);assert.equal(deleted.foodRecords[1].deleted,true);assert.equal(deleted.foodRecords[1].version,1);assert.equal(deleted.foodRecords[1].energyKj,412);assert.deepEqual(deleted.receipts,before.receipts)
  await page.goto('/dashboard?date=2020-01-02');await expect(page.getByLabel('Energy intake',{exact:true})).toContainText('989 kJ');await expect(page.getByLabel('Exercise expenditure',{exact:true})).toContainText('214 kJ');await expect(page.getByLabel('Time in motion',{exact:true})).toContainText('12.5 min')
  await page.goto('/dashboard?date=2019-12-31');await expect(page.getByLabel('Time in motion',{exact:true})).toContainText('Not recorded');assert.deepEqual(inspectOwnedSyntheticData(identity.email),deleted)
})

test('T-AUTH-07: switching accounts cannot expose the previous owner record',async({page})=>{
  const owner=syntheticIdentity('q06-owner'),other=syntheticIdentity('q06-other')
  await registerAndLogin(page,owner);await completeQuestionnaire(page);await enterTwoFoodsForReview(page);await confirmFoodWithDoubleClick(page)
  const privateState=inspectOwnedSyntheticData(owner.email),rice=privateState.foodRecords[0]
  await logout(page);await registerAndLogin(page,other);await completeQuestionnaire(page)
  await page.goto(`/history?from=${rice.recordDate}&to=${rice.recordDate}&type=food&highlight=food:${rice.id}`)
  await expect(page.getByText('This record was removed or is not available to this account.',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Edit record',exact:true})).toHaveCount(0)
  assert.deepEqual(inspectOwnedSyntheticData(owner.email),privateState);const otherState=inspectOwnedSyntheticData(other.email);assert.equal(otherState.foodRecords.length,0);assert.equal(otherState.exerciseRecords.length,0);assert.equal(otherState.receipts.length,0)
})
