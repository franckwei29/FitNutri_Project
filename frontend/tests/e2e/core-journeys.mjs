import {expect} from '@playwright/test'
import assert from 'node:assert/strict'
const observe=async hooks=>{await hooks?.afterStep?.()}
async function click(page,name,hooks){const button=page.getByRole('button',{name,exact:true});await button.click();await observe(hooks)}
export async function cancelFoodReview(page,hooks={}) {
  await click(page,'Cancel entry',hooks)
  await page.getByRole('dialog',{name:'Discard this unsubmitted entry?',exact:true}).waitFor({state:'visible'})
  await click(page,'Discard entry',hooks)
  await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  return {caseId:'T-FOOD-01',cancelled:true}
}
export async function confirmFoodWithDoubleClick(page,hooks={}) {
  await expect(page.getByRole('button',{name:'Confirm & save',exact:true})).toBeEnabled()
  await page.getByRole('button',{name:'Confirm & save',exact:true}).evaluate(button=>{button.click();button.click()});await observe(hooks)
  await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('link',{name:'View records',exact:true}).waitFor({state:'visible'})
  return {caseId:'T-FOOD-02',saved:true,count:2}
}
export async function enterMissingWeightExerciseReview(page,hooks={}) {
  await page.goto((hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173')+'/add?kind=exercise')
  await page.getByRole('heading',{name:'Add a record.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await click(page,'Continue',hooks)
  await page.getByRole('heading',{name:'Add exercise',exact:true}).waitFor({state:'visible'});await observe(hooks)
  if(hooks.setDate)await hooks.setDate('Record date','2019-12-31');else await page.getByLabel('Record date',{exact:true}).fill('2019-12-31')
  await observe(hooks)
  const type=page.getByRole('combobox',{name:'Find activity or movement',exact:true});await type.fill('Walking');await observe(hooks)
  const option=page.getByRole('option',{name:'Activity: Walking',exact:true})
  await option.waitFor({state:'visible'});await option.click();await observe(hooks)
  await page.getByLabel('Duration (minutes)',{exact:true}).fill('12.5');await observe(hooks)

  await click(page,'Review exercise entry',hooks)
  await page.getByRole('heading',{name:'Review your record.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('button',{name:'Confirm & save',exact:true}).waitFor({state:'visible'})
  return {caseId:'T-EX-02',minutes:12.5,date:'2019-12-31'}
}
export async function confirmExercise(page,hooks={}) {
  await click(page,'Confirm & save',hooks)
  await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('link',{name:'View records',exact:true}).waitFor({state:'visible'})
  return {caseId:'T-EX-02',saved:true}
}
export async function logout(page,hooks={}) {
  await click(page,'Account menu',hooks)
  await page.getByRole('menuitem',{name:'Log out',exact:true}).click();await observe(hooks)
  await page.getByRole('heading',{name:'Welcome back.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function loginExisting(page,identity,hooks={}) {
  await page.getByLabel('Email address',{exact:true}).fill(identity.email)
  await page.getByLabel('Password',{exact:true}).fill(identity.password);await observe(hooks)
  await click(page,'Sign in',hooks)
  await page.getByRole('button',{name:'Account menu',exact:true}).waitFor({state:'visible'})
  await page.goto((hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173')+'/dashboard')
  await page.getByRole('heading',{name:'A little progress, every day.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
