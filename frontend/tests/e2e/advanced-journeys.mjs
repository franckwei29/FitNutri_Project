import assert from 'node:assert/strict'
/** Actual unconfigured application path. No request interception/provider fixture or synthetic answer. */
export async function verifyUnavailableHelpers(page,hooks={}) {
 const base=hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173',observe=async()=>{await hooks.afterStep?.()}
 await page.goto(base+'/add');await page.getByRole('heading',{name:'Add a record.',exact:true}).waitFor({state:'visible'});await observe()
 for(const name of [/Voice entry Start/,/Upload food image Review/])assert.notEqual(await page.getByRole('button',{name}).getAttribute('disabled'),null)
 assert.equal(await page.getByRole('button',{name:'Continue',exact:true}).getAttribute('disabled'),null)
 await page.goto(base+'/chat');await page.getByRole('heading',{name:'Q&A needs your consent and an eligible profile.',exact:true}).waitFor({state:'visible'});await observe()
 assert.equal(await page.getByRole('link',{name:'Review Profile',exact:true}).getAttribute('href'),'/profile')
 await page.getByRole('link',{name:'Continue recording',exact:true}).click();await page.getByRole('heading',{name:'Add a record.',exact:true}).waitFor({state:'visible'});await observe()
 return {caseId:'T-AI-01',serverUnconfigured:true,qaConsentRestricted:true,manualRecordingAccessible:true}
}
