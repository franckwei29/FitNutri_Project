import {test} from '@playwright/test'
import assert from 'node:assert/strict'
import {registerAndLogin,completeQuestionnaire} from './browser-journeys.mjs'
import {verifyUnavailableHelpers} from './advanced-journeys.mjs'
import {assertE2EEnvironment,syntheticIdentity,inspectOwnedSyntheticData} from './owned-db-verification.mjs'
test.beforeAll(()=>assertE2EEnvironment())
test('unconfigured helpers fail closed while real manual recording stays available without record writes',async({page})=>{
 const identity=syntheticIdentity('q07-unconfigured');await registerAndLogin(page,identity);await completeQuestionnaire(page)
 const before=inspectOwnedSyntheticData(identity.email);assert.equal(before.profile.qaConsentGranted,false);assert.equal(before.foodRecords.length,0);assert.equal(before.exerciseRecords.length,0)
 await verifyUnavailableHelpers(page);assert.deepEqual(inspectOwnedSyntheticData(identity.email),before)
})
