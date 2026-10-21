import {test,expect} from '@playwright/test'
import {newOwner,beginFood,addFood,foodReview,baseURL} from './v3-journeys'
import {inspectOwnedSyntheticData,acquireSyntheticOwnerLock,assertOriginalPostIsWaiting} from './owned-db-verification.mjs'
const proxy=process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:18082'
async function unknown(page:import('@playwright/test').Page){await page.getByRole('button',{name:'Confirm & save',exact:true}).click();await expect(page.getByText('The save result is unknown. It may already be committed. Details stay frozen until the original submission is resolved.',{exact:true})).toBeVisible({timeout:45000});const text=await page.getByText(/^Submission reference:/).textContent();const id=text?.match(/[0-9a-f]{8}-[0-9a-f-]{27}/i)?.[0];expect(id).toBeTruthy();return id!}
async function setup(page:import('@playwright/test').Page,prefix:string){const owner=await newOwner(page,prefix);await beginFood(page);await addFood(page);await addFood(page,'chicken','Chicken breast, lean, raw','100');await foodReview(page);expect(inspectOwnedSyntheticData(owner.email).foodRecords).toHaveLength(0);return owner}
test('TUX106: real post-commit response loss recovers by GET without another POST',async({page,request})=>{
 const owner=await setup(page,'v3-lost');expect((await request.post(proxy+'/__test__/control',{data:{action:'DROP_NEXT_ACK',path:'/api/v2/food/batches'}})).status()).toBe(200)
 const id=await unknown(page),committed=inspectOwnedSyntheticData(owner.email);expect(committed.foodRecords).toHaveLength(2)
 await page.getByRole('link',{name:'Dashboard',exact:true}).click();const guard=page.getByRole('dialog');await expect(guard).toContainText("The save may already have completed. Leaving won't cancel it. Check History before recording it again.");await guard.getByRole('button',{name:'Stay on this page',exact:true}).click()
 await expect(page.getByText('Submission reference: '+id,{exact:true})).toBeVisible();await page.getByRole('button',{name:'Check original receipt',exact:true}).click();await expect(page).toHaveURL(baseURL+'/dashboard')
 expect(inspectOwnedSyntheticData(owner.email)).toEqual(committed)
 const state=await(await request.get(proxy+'/__test__/status')).json();const attempts=state.attempts.filter((a:{submissionId:string})=>a.submissionId===id);expect(attempts).toHaveLength(1);expect(attempts[0]).toMatchObject({status:201,dropped:true});expect(attempts[0].ackRootKeys).toHaveLength(2)
})
test('TUX108: real receipt404 while the original transaction waits retains its only identity',async({page,request})=>{
 const owner=await setup(page,'v3-late'),before=inspectOwnedSyntheticData(owner.email),lock=await acquireSyntheticOwnerLock(owner.email);let id=''
 try{id=await unknown(page);assertOriginalPostIsWaiting(lock.pid);await page.getByRole('button',{name:'Check original receipt',exact:true}).click();await expect(page.getByText('No committed receipt is visible yet. The original request may still be running. Keep its original ID and payload.',{exact:true})).toBeVisible();assertOriginalPostIsWaiting(lock.pid);expect(inspectOwnedSyntheticData(owner.email)).toEqual(before);await expect(page.getByText('Submission reference: '+id,{exact:true})).toBeVisible()}finally{await lock.release()}
 await expect.poll(()=>inspectOwnedSyntheticData(owner.email).foodRecords.length).toBe(2);await page.getByRole('button',{name:'Retry the same submission',exact:true}).click();await expect(page).toHaveURL(baseURL+'/dashboard')
 const state=await(await request.get(proxy+'/__test__/status')).json(),attempts=state.attempts.filter((a:{submissionId:string})=>a.submissionId===id);expect(attempts).toHaveLength(2);expect(attempts[0].clientGone).toBe(true);expect(attempts[1].bodySha256).toBe(attempts[0].bodySha256);expect(attempts[1].clientEntryIds).toEqual(attempts[0].clientEntryIds);expect(attempts[1].ackRootKeys).toEqual(attempts[0].ackRootKeys);expect(inspectOwnedSyntheticData(owner.email).foodRecords).toHaveLength(2)
})
