import {test,expect} from '@playwright/test'
import assert from 'node:assert/strict'
import {registerAndLogin,completeQuestionnaire,enterTwoFoodsForReview} from './browser-journeys.mjs'
import {confirmFoodWithDoubleClick,enterMissingWeightExerciseReview,confirmExercise,loginExisting} from './core-journeys.mjs'
import {reviewExerciseDateCorrection,confirmCorrection} from './record-management-journeys.mjs'
import {setGoalAndReference,switchEnergyDisplayUnit,clearGoalAndReference,correctDatedWeight,confirmWeightCorrection,backfillEarlierWeight,loginAdmin,searchManagedAccount,confirmAccountStatus} from './profile-admin-journeys.mjs'
import {assertE2EEnvironment,syntheticIdentity,inspectOwnedSyntheticData,inspectSyntheticAccountAccess} from './owned-db-verification.mjs'
test.beforeAll(()=>assertE2EEnvironment())
test('T-PROFILE-04/T-WEIGHT-01/02: null targets, unit changes and dated weights preserve saved records',async({page})=>{
  const identity=syntheticIdentity('q06-profile')
  await registerAndLogin(page,identity);await completeQuestionnaire(page);await enterTwoFoodsForReview(page);await confirmFoodWithDoubleClick(page);await enterMissingWeightExerciseReview(page);await confirmExercise(page)
  const exercise=inspectOwnedSyntheticData(identity.email).exerciseRecords[0];await reviewExerciseDateCorrection(page,{id:exercise.id,from:exercise.recordDate,to:'2020-01-02'});await confirmCorrection(page)
  const before=inspectOwnedSyntheticData(identity.email)
  await page.goto('/profile');await setGoalAndReference(page)
  await expect.poll(()=>inspectOwnedSyntheticData(identity.email).profile.dailyTargetKj).toBe(8400);assert.equal(inspectOwnedSyntheticData(identity.email).profile.goalType,'MAINTAIN')
  await switchEnergyDisplayUnit(page);await expect(page.getByRole('combobox',{name:'Energy display unit',exact:true})).toHaveText('Kilocalories (kcal)');assert.equal(inspectOwnedSyntheticData(identity.email).profile.dailyTargetKj,8400)
  await clearGoalAndReference(page);await expect.poll(()=>inspectOwnedSyntheticData(identity.email).profile.dailyTargetKj).toBeNull();assert.equal(inspectOwnedSyntheticData(identity.email).profile.goalType,null)
  await correctDatedWeight(page);assert.deepEqual(inspectOwnedSyntheticData(identity.email).weights,before.weights)
  await confirmWeightCorrection(page);await expect.poll(()=>inspectOwnedSyntheticData(identity.email).weights[0].weightKg).toBe(71.5)
  const corrected=inspectOwnedSyntheticData(identity.email);assert.equal(corrected.weights.length,1);assert.equal(corrected.weights[0].version,1)
  await backfillEarlierWeight(page);await expect.poll(()=>inspectOwnedSyntheticData(identity.email).weights.length).toBe(2)
  const after=inspectOwnedSyntheticData(identity.email);assert.deepEqual(after.weights,[{recordDate:'2019-12-30',weightKg:69,version:0},{recordDate:'2020-01-01',weightKg:71.5,version:1}]);assert.deepEqual(after.foodRecords,before.foodRecords);assert.deepEqual(after.exerciseRecords,before.exerciseRecords);assert.deepEqual(after.receipts,before.receipts);await expect(page.getByText('Latest weight',{exact:true}).locator('..')).toContainText('71.5 kg')
})
test('T-ADMIN-02: separate administrator session disables and restores access without changing private data',async({page,browser})=>{
  // An explicitly provisioned synthetic administrator is required; no default/admin promotion or skipped test.
  const email=process.env.FITNUTRI_E2E_ADMIN_EMAIL,password=process.env.FITNUTRI_E2E_ADMIN_PASSWORD
  assert.match(email??'',/^synthetic[.a-z0-9-]*@example\.test$/,'Explicit existing synthetic administrator email required.');assert.ok(password,'Explicit synthetic administrator password required.')
  const identity=syntheticIdentity('q06-admin-target')
  await registerAndLogin(page,identity);await completeQuestionnaire(page);await enterTwoFoodsForReview(page);await confirmFoodWithDoubleClick(page)
  const before=inspectOwnedSyntheticData(identity.email),access=inspectSyntheticAccountAccess(identity.email)
  const adminContext=await browser.newContext({baseURL:process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173'})
  try {
    const adminPage=await adminContext.newPage();await adminPage.goto('/login');await loginAdmin(adminPage,{email:email!,password:password!});await searchManagedAccount(adminPage,identity.email)
    await confirmAccountStatus(adminPage,identity.displayName,false);assert.deepEqual(inspectOwnedSyntheticData(identity.email),before);assert.deepEqual(inspectSyntheticAccountAccess(identity.email),{...access,enabled:false,version:access.version+1})
    await page.reload();await expect(page.getByRole('heading',{name:'Welcome back.',exact:true})).toBeVisible()
    await confirmAccountStatus(adminPage,identity.displayName,true);assert.deepEqual(inspectOwnedSyntheticData(identity.email),before);assert.deepEqual(inspectSyntheticAccountAccess(identity.email),{...access,enabled:true,version:access.version+2})
    await loginExisting(page,identity);await page.goto('/dashboard?date=2020-01-02');await expect(page.getByLabel('Energy intake',{exact:true})).toContainText('1,401 kJ');assert.deepEqual(inspectOwnedSyntheticData(identity.email),before)
  } finally {await adminContext.close()}
})
