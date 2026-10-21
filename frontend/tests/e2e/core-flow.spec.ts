import {test,expect} from '@playwright/test'
import assert from 'node:assert/strict'
import {registerAndLogin,completeQuestionnaire,enterTwoFoodsForReview} from './browser-journeys.mjs'
import {cancelFoodReview,confirmFoodWithDoubleClick,enterMissingWeightExerciseReview,confirmExercise,logout,loginExisting} from './core-journeys.mjs'
import {assertE2EEnvironment,syntheticIdentity,inspectOwnedSyntheticData} from './owned-db-verification.mjs'
test.beforeAll(()=>assertE2EEnvironment())
test('M1 real register-to-dashboard flow preserves cancellation, snapshots and missing energy',async({page,request})=>{
  const identity=syntheticIdentity('q05-core')
  await registerAndLogin(page,identity);await completeQuestionnaire(page)
  const setup=inspectOwnedSyntheticData(identity.email);assert.equal(setup.foodRecords.length,0);assert.equal(setup.exerciseRecords.length,0);assert.equal(setup.weights.length,1);assert.equal(setup.profile.goalType,null);assert.equal(setup.profile.qaConsentGranted,false)
  await enterTwoFoodsForReview(page);assert.deepEqual(inspectOwnedSyntheticData(identity.email),setup)
  await cancelFoodReview(page);assert.deepEqual(inspectOwnedSyntheticData(identity.email),setup)
  await enterTwoFoodsForReview(page);await confirmFoodWithDoubleClick(page)
  const food=inspectOwnedSyntheticData(identity.email);assert.equal(food.foodRecords.length,2);assert.equal(food.receipts.length,1);assert.deepEqual(food.foodRecords.map(x=>[x.recordDate,x.quantityGrams,x.energyKj,x.mealType]),[['2020-01-02',150,988.5,'LUNCH'],['2020-01-02',100,412,'DINNER']])
  await page.getByRole('link',{name:'View records',exact:true}).click();await expect(page).toHaveURL(/from=2020-01-02&to=2020-01-02/);await expect(page.locator('article.saved-entry-root[data-highlighted="true"]')).toHaveCount(2);await page.getByRole('link',{name:'Rice, white, cooked',exact:true}).click();await expect(page.getByRole('dialog',{name:'Saved entry details',exact:true})).toBeVisible();await page.reload();await expect(page.getByRole('dialog',{name:'Saved entry details',exact:true}).getByRole('heading',{name:'Rice, white, cooked',exact:true})).toBeVisible();await page.getByRole('button',{name:'Close',exact:true}).click()
  await enterMissingWeightExerciseReview(page);assert.equal(inspectOwnedSyntheticData(identity.email).exerciseRecords.length,0)
  await confirmExercise(page)
  const committed=inspectOwnedSyntheticData(identity.email);assert.equal(committed.exerciseRecords.length,1);assert.equal(committed.receipts.length,2);assert.equal(committed.exerciseRecords[0].durationMinutes,12.5);assert.equal(committed.exerciseRecords[0].estimateStatus,'MISSING_WEIGHT');assert.equal(committed.exerciseRecords[0].estimatedKj,null)
  await page.goto('/dashboard?date=2019-12-31');await expect(page.getByLabel('Time in motion',{exact:true})).toContainText('12.5 min');await expect(page.getByLabel('Exercise expenditure',{exact:true})).toContainText('Unavailable');await expect(page.getByLabel('Exercise expenditure',{exact:true})).not.toContainText('0 kJ');await expect(page.getByText('Recorded values only. No personal intake reference set.',{exact:true})).toBeVisible()
  await page.reload();await expect(page.getByLabel('Time in motion',{exact:true})).toContainText('12.5 min')
  await logout(page);await loginExisting(page,identity)
  await page.goto('/dashboard?date=2019-12-31');await expect(page.getByLabel('Time in motion',{exact:true})).toContainText('12.5 min');assert.deepEqual(inspectOwnedSyntheticData(identity.email),committed)
  await page.goto('/review');await expect(page.getByText('Start an entry to review its details before saving.',{exact:true})).toBeVisible();assert.deepEqual(inspectOwnedSyntheticData(identity.email),committed)
  const status=await(await request.get((process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:18082')+'/__test__/status')).json() as {attempts:{clientEntryIds:string[];status:number}[]}
  const allIds=new Set([...committed.foodRecords,...committed.exerciseRecords].map(x=>x.clientEntryId)),attempts=status.attempts.filter(x=>x.clientEntryIds.some(id=>allIds.has(id)));assert.equal(attempts.length,2);assert.ok(attempts.every(x=>x.status===201))
})
