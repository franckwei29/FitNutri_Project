import assert from 'node:assert/strict'
const observe=async hooks=>{await hooks?.afterStep?.()}
export async function reviewFoodCorrection(page,{id,date,grams},hooks={}) {
  const destination=(hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173')+`/history?from=${date}&to=${date}&type=food&highlight=food:${id}`
  if(await page.url()!==destination)await page.goto(destination)
  await page.getByRole('dialog',{name:'Saved record details',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('button',{name:'Edit record',exact:true}).click();await observe(hooks)
  await page.getByLabel('Portion in grams 1',{exact:true}).fill(String(grams));await observe(hooks)
  await page.getByRole('button',{name:'Review food changes',exact:true}).click();await observe(hooks)
  await page.getByRole('heading',{name:'Review your changes.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function confirmCorrection(page,hooks={}) {
  await page.getByRole('button',{name:'Confirm changes',exact:true}).click();await observe(hooks)
  await page.getByRole('heading',{name:'Your changes are saved.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function assertVersionConflict(page,hooks={},versions={reviewed:0,latest:1}) {
  await page.getByRole('button',{name:'Confirm changes',exact:true}).click();await observe(hooks)
  await page.getByText('The reviewed version cannot be overwritten. Read and compare the latest saved record before starting again.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
  assert.notEqual(await page.getByRole('button',{name:'Confirm changes',exact:true}).getAttribute('disabled'),null)
  assert.ok((await page.getByText(`Reviewed version: ${versions.reviewed}`,{exact:true}).textContent()).includes(String(versions.reviewed)))
  await page.getByRole('button',{name:'Read latest saved record',exact:true}).click();await observe(hooks)
  await page.getByText(`Latest saved version: ${versions.latest}`,{exact:true}).waitFor({state:'visible'});await observe(hooks)
  assert.notEqual(await page.getByRole('button',{name:'Confirm changes',exact:true}).getAttribute('disabled'),null)
  return {caseId:'T-EDIT-03',conflict:true,reviewedVersion:versions.reviewed,latestVersion:versions.latest}
}
export async function discardCorrection(page,hooks={}) {
  await page.getByRole('button',{name:'Cancel correction',exact:true}).click();await observe(hooks)
  await page.getByRole('button',{name:'Discard correction',exact:true}).click();await observe(hooks)
}
export async function reviewExerciseDateCorrection(page,{id,from,to},hooks={}) {
  await page.goto((hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173')+`/history?from=${from}&to=${from}&type=exercise&highlight=exercise:${id}`)
  await page.getByRole('dialog',{name:'Saved record details',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('button',{name:'Edit record',exact:true}).click();await observe(hooks)
  if(hooks.setDate)await hooks.setDate('Record date',to);else await page.getByLabel('Record date',{exact:true}).fill(to)
  await observe(hooks)
  await page.getByRole('button',{name:'Review exercise changes',exact:true}).click();await observe(hooks)
  await page.getByRole('heading',{name:'Review your changes.',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function openRecordDeletion(page,{id,date,kind},hooks={}) {
  await page.goto((hooks.baseURL??process.env.FITNUTRI_E2E_BASE_URL??'http://localhost:15173')+`/history?from=${date}&to=${date}&type=${kind}&highlight=${kind}:${id}`)
  await page.getByRole('dialog',{name:'Saved record details',exact:true}).waitFor({state:'visible'});await observe(hooks)
  await page.getByRole('button',{name:'Delete record',exact:true}).click();await observe(hooks)
  await page.getByRole('dialog',{name:'Delete this saved record?',exact:true}).waitFor({state:'visible'});await observe(hooks)
}
export async function cancelDeletion(page,hooks={}) {
  await page.getByRole('button',{name:'Cancel',exact:true}).click();await observe(hooks)
  await page.getByRole('dialog',{name:'Delete this saved record?',exact:true}).waitFor({state:'hidden'});await observe(hooks)
}
export async function confirmDeletion(page,hooks={}) {
  await page.getByRole('button',{name:'Confirm deletion',exact:true}).click();await observe(hooks)
  await page.getByText('The record is deleted. It is excluded from lists and summaries.',{exact:true}).waitFor({state:'visible'});await observe(hooks)
}
