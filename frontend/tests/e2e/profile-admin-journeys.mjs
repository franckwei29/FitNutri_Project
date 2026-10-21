const observe=async hooks=>{await hooks?.afterStep?.()}
export async function setGoalAndReference(page,hooks={}) {
  await page.getByRole('button',{name:'Maintain',exact:true}).click();await observe(hooks)
  await page.getByLabel('Daily intake reference (kJ, optional)',{exact:true}).fill('8400');await observe(hooks)
  await page.getByRole('button',{name:'Save settings',exact:true}).click();await observe(hooks)
  await page.getByText('Your profile is saved.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function switchEnergyDisplayUnit(page,hooks={}) {
  await page.getByRole('combobox',{name:'Energy display unit',exact:true}).click();await observe(hooks)
  await page.getByRole('option',{name:'Kilocalories (kcal)',exact:true}).click();await observe(hooks)
  await page.getByRole('button',{name:'Save settings',exact:true}).click();await observe(hooks)
  await page.getByText('Your profile is saved.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function clearGoalAndReference(page,hooks={}) {
  await page.getByRole('button',{name:'Remove goal',exact:true}).click();await observe(hooks)
  await page.getByLabel('Daily intake reference (kcal, optional)',{exact:true}).fill('');await observe(hooks)
  await page.getByRole('button',{name:'Save settings',exact:true}).click();await observe(hooks)
  await page.getByText('Your profile is saved.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function correctDatedWeight(page,hooks={}) {
  await page.getByLabel('Weight (kg)',{exact:true}).fill('71.5')
  if(hooks.setDate)await hooks.setDate('Weight date','2020-01-01');else await page.getByLabel('Weight date',{exact:true}).fill('2020-01-01')
  await observe(hooks);await page.getByRole('button',{name:'Save weight',exact:true}).click();await observe(hooks)
  await page.getByRole('dialog',{name:"Review this day's saved weight",exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function confirmWeightCorrection(page,hooks={}) {
  await page.getByRole('button',{name:'Confirm correction',exact:true}).click();await observe(hooks)
  await page.getByText('Weight saved. Existing activity records keep their saved estimates.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function backfillEarlierWeight(page,hooks={}) {
  await page.getByLabel('Weight (kg)',{exact:true}).fill('69')
  if(hooks.setDate)await hooks.setDate('Weight date','2019-12-30');else await page.getByLabel('Weight date',{exact:true}).fill('2019-12-30')
  await observe(hooks);await page.getByRole('button',{name:'Save weight',exact:true}).click();await observe(hooks)
  await page.getByText('Weight saved. Existing activity records keep their saved estimates.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function loginAdmin(page,identity,hooks={}) {
  await page.getByLabel('Email address',{exact:true}).fill(identity.email)
  await page.getByLabel('Password',{exact:true}).fill(identity.password)
  await observe(hooks);await page.getByRole('button',{name:'Sign in',exact:true}).click();await observe(hooks)
  await page.getByRole('heading',{name:'User management.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function searchManagedAccount(page,email,hooks={}) {
  await page.getByLabel('Search name or email',{exact:true}).fill(email);await observe(hooks)
  await page.getByRole('button',{name:'Search accounts',exact:true}).click();await observe(hooks)
  await page.getByRole('cell',{name:email,exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function confirmAccountStatus(page,displayName,enabled,hooks={}) {
  await page.getByRole('button',{name:`${enabled?'Reactivate':'Deactivate'} ${displayName}`,exact:true}).click();await observe(hooks)
  await page.getByRole('dialog',{name:enabled?'Reactivate this account?':'Deactivate this account?',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('button',{name:enabled?'Confirm reactivation':'Confirm deactivation',exact:true}).click();await observe(hooks)
  await page.getByText(`${displayName} was ${enabled?'reactivated':'deactivated'}. Saved personal records are retained.`,{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
