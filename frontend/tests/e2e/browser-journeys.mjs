import assert from 'node:assert/strict'
const defaultBase=process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173'
const text=locator=>locator.evaluate(node=>node.textContent??'')
const observe=async hooks=>{await hooks?.afterStep?.()}
async function click(page,name,hooks){const target=page.getByRole('button',{name,exact:true});for(let n=0;n<20;n++){if(await target.getAttribute('disabled')===null)break;await observe(hooks)}await target.click();await observe(hooks)}
async function select(page,label,option,hooks){await page.getByRole('combobox',{name:label,exact:true}).click();await observe(hooks);await page.getByRole('option',{name:option,exact:true}).click();await observe(hooks)}
/** Works with Playwright Page or the Codex IAB's documented Playwright facade; no route.fulfill. */
export async function registerAndLogin(page,identity,hooks={}){
  if(await page.url()!==(hooks.baseURL??defaultBase)+'/login')await page.goto((hooks.baseURL??defaultBase)+'/login')
  await page.getByRole('heading',{name:'Welcome back.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await click(page,'Create an account',hooks)
  await page.getByLabel('Display name',{exact:true}).fill(identity.displayName)
  await page.getByLabel('Email address',{exact:true}).fill(identity.email)
  await page.getByLabel('Password',{exact:true}).fill(identity.password)
  await page.getByLabel('Confirm password',{exact:true}).fill(identity.password);await observe(hooks)
  await click(page,'Create account',hooks)
  await page.getByText('Account created. Sign in to set up your profile.',{exact:true}).waitFor({state:'visible'})
  await page.getByLabel('Email address',{exact:true}).fill(identity.email);await page.getByLabel('Password',{exact:true}).fill(identity.password);await observe(hooks)
  await click(page,'Sign in',hooks);await page.getByRole('heading',{name:'Let’s set up your profile.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  return {caseId:'T-AUTH-04',registered:true,onboardingVisible:true}
}
async function setDate(page,label,value,hooks){if(hooks.setDate)await hooks.setDate(label,value);else await page.getByLabel(label,{exact:true}).fill(value);await observe(hooks)}
export async function completeQuestionnaire(page,hooks={}){
  await page.getByLabel('I confirm I am 18 years of age or older.',{exact:true}).click()
  await page.getByLabel('Year of birth',{exact:true}).fill('1990')
  await page.getByLabel('Height (cm)',{exact:true}).fill('170')
  await page.getByLabel('Weight (kg)',{exact:true}).fill('70');await observe(hooks)
  await select(page,'Sex','Prefer not to say',hooks);await setDate(page,'Weight date','2020-01-01',hooks)
  await click(page,'Continue',hooks);await page.getByRole('heading',{name:'Goals & activity',exact:true}).waitFor({state:'visible'})
  await select(page,'Activity level','Lightly active',hooks)
  // Default goal/reference remain genuinely unset; no Q&A consent is checked.
  await click(page,'Continue',hooks);await click(page,'No',hooks)
  assert.equal(await page.getByLabel('Allow FitNutri to use my confirmed records to answer general health questions.',{exact:true}).evaluate(node=>node.checked),false)
  await click(page,'Complete setup',hooks);await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  return {caseId:'T-PROFILE-01',qaConsent:false,goal:null,weightDate:'2020-01-01'}
}
export async function beginManualFood(page,hooks={}){
  await page.goto((hooks.baseURL??defaultBase)+'/add');await page.getByRole('heading',{name:'Add a record.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  for(const name of [/Voice entry Start/,/Upload food image Review/])assert.notEqual(await page.getByRole('button',{name}).getAttribute('disabled'),null)
  await click(page,'Continue',hooks);await page.getByRole('heading',{name:'Add food',exact:true}).waitFor({state:'visible'});await setDate(page,'Record date','2020-01-02',hooks)
}
export async function fillFood(page,number,{query,name,grams,meal},hooks={}){
  await select(page,'Default meal',meal,hooks)
  await page.getByRole('combobox',{name:'Find food to add',exact:true}).fill(query);await observe(hooks)
  const option=page.getByRole('option').filter({has:page.getByText(name,{exact:true})});await option.waitFor({state:'visible'});await option.click();await observe(hooks)
  await page.getByLabel('Grams to add',{exact:true}).fill(String(grams));await click(page,'Add to meal',hooks)
  assert.equal(await page.getByLabel('Grams to add',{exact:true}).inputValue(),'')
}
export async function enterTwoFoodsForReview(page,hooks={}){
  await beginManualFood(page,hooks)
  await fillFood(page,1,{query:'rice',name:'White rice, cooked',grams:150,meal:'Lunch'},hooks)
  await fillFood(page,2,{query:'chicken',name:'Chicken breast, lean, raw',grams:100,meal:'Dinner'},hooks)
  await click(page,'Review food entries',hooks);await page.getByRole('heading',{name:'Review your record.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  return {caseId:'T-FOOD-02',count:2,recordDate:'2020-01-02'}
}
export async function confirmToUnknown(page,hooks={}){
  await click(page,'Confirm & save',hooks)
  await page.getByText('The save result is unknown. It may already be committed. Details stay frozen until the original submission is resolved.',{exact:true}).waitFor({state:'visible',...hooks.waitOptions});await observe(hooks)
  const reference=await text(page.getByText(/^Submission reference:/));const id=reference.match(/[0-9a-f]{8}-[0-9a-f-]{27}/i)?.[0];assert.ok(id,'Unknown state must retain its original submission UUID')
  assert.equal(await page.getByRole('button',{name:'Edit details',exact:true}).count(),0)
  return {caseId:'T-IDEM-04',submissionId:id,unknown:true}
}
export async function assertFrozenNavigation(page,submissionId,hooks={}){
  await page.getByRole('link',{name:'Add a record',exact:true}).click();await page.getByRole('dialog',{name:'Confirmation is still pending',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await click(page,'Stay on this page',hooks);await page.getByRole('dialog',{name:'Confirmation is still pending',exact:true}).waitFor({state:'hidden'})
  assert.ok((await text(page.getByText(/^Submission reference:/))).includes(submissionId));await observe(hooks)
  return {caseId:'T-IDEM-05',frozen:true}
}
export async function recoverOriginalReceipt(page,submissionId,hooks={}){
  assert.ok((await text(page.getByText(/^Submission reference:/))).includes(submissionId))
  await click(page,'Check original receipt',hooks);await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('link',{name:'View records',exact:true}).waitFor({state:'visible'})
  return {caseId:'T-IDEM-04',submissionId,recovered:true}
}
