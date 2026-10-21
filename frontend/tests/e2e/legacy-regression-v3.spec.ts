import {randomUUID} from 'node:crypto'
import {test,expect} from '@playwright/test'
import type {APIRequestContext} from '@playwright/test'
import {newOwner,beginFood,makeDish,foodReview,confirmFood,foodRoots,baseURL} from './v3-journeys'
import {beginExercise,activity,exerciseReview,confirmExercise,exerciseRoots} from './v3-exercise-journeys'
import {inspectOwnedSyntheticData} from './owned-db-verification.mjs'
import {inspectGroups} from './owned-v3-verification'
import {loginAdmin} from './profile-admin-journeys.mjs'
async function csrf(api:APIRequestContext){const response=await api.get(baseURL+'/api/auth/csrf');expect(response.status()).toBe(200);return (await response.json() as {token:string}).token}
async function write(api:APIRequestContext,method:'POST'|'PUT'|'DELETE',path:string,data?:unknown){return api.fetch(baseURL+'/api'+path,{method,data,headers:{'X-CSRF-TOKEN':await csrf(api)}})}
test('TUX118/119: real USER and ADMIN sessions cannot access another owner groups,preview,mutation or proof',async({page,browser})=>{
 const owner=await newOwner(page,'v3-private');await beginFood(page);await makeDish(page);await foodReview(page);const foodProof=await confirmFood(page),dish=(await foodRoots(page)).results[0]
 await beginExercise(page,'2020-01-02',true);await activity(page.getByRole('group',{name:'Strength period 1',exact:true}),'Strength activity','Strength training','Strength training');await page.getByLabel('Duration (minutes)',{exact:true}).fill('30');await exerciseReview(page,true);const workoutProof=await confirmExercise(page),workout=(await exerciseRoots(page)).results[0],before=inspectOwnedSyntheticData(owner.email),groups=inspectGroups(owner.email)
 expect((await page.request.get(baseURL+'/api/admin/users')).status()).toBe(403)
 expect((await page.request.delete(baseURL+`/api/v2/food/entries/dish/${dish.id}?expectedVersion=0`)).status()).toBe(403)
 const child=foodProof.leaves[0].id,blocked=await write(page.request,'DELETE',`/records/food/${child}?expectedVersion=0`);expect(blocked.status()).toBe(409);expect(await blocked.json()).toMatchObject({code:'GROUP_EDIT_REQUIRED'})
 const exerciseChild=await write(page.request,'DELETE',`/records/exercise/${workoutProof.leaves[0].id}?expectedVersion=0`);expect(exerciseChild.status()).toBe(409);expect(await exerciseChild.json()).toMatchObject({code:'GROUP_EDIT_REQUIRED'})
 const context=await browser.newContext({baseURL}),other=await context.newPage()
 try{
  await newOwner(other,'v3-private-other')
  for(const role of ['USER','ADMIN']){
   if(role==='ADMIN'){expect((await write(other.request,'POST','/auth/logout')).status()).toBe(204);await other.goto(baseURL+'/login');await loginAdmin(other,{email:process.env.FITNUTRI_E2E_ADMIN_EMAIL!,password:process.env.FITNUTRI_E2E_ADMIN_PASSWORD!})}
   for(const [domain,kind,root,proof] of [['food','dish',dish,foodProof],['exercise','workout',workout,workoutProof]] as const){
    const path=`/v2/${domain}/entries/${kind}/${root.id}`,input={expectedVersion:0,recordDate:root.recordDate,zone:'Australia/Sydney',entry:root.input}
    expect((await other.request.get(baseURL+'/api'+path)).status()).toBe(role==='ADMIN'?403:404)
    expect((await write(other.request,'POST',path+'/preview',input)).status()).toBe(role==='ADMIN'?403:404)
    expect((await write(other.request,'PUT',path,input)).status()).toBe(role==='ADMIN'?403:404)
    expect((await write(other.request,'DELETE',path+'?expectedVersion=0')).status()).toBe(role==='ADMIN'?403:404)
    expect((await other.request.get(baseURL+'/api/v2/submissions/'+proof.submissionId)).status()).toBe(role==='ADMIN'?403:404)
    const recent=await other.request.get(baseURL+`/api/v2/${domain}/recent`);expect(recent.status()).toBe(role==='ADMIN'?403:200);if(role==='USER')expect((await recent.json() as {results:unknown[]}).results).toEqual([])
   }
  }
 }finally{await context.close()}
 expect(inspectOwnedSyntheticData(owner.email)).toEqual(before);expect(inspectGroups(owner.email)).toEqual(groups)
})
test('TUX116/129: V1 creation/replay/correction retain minimal immutable proof alongside V2 records',async({page})=>{
 const owner=await newOwner(page,'v3-v1');await beginFood(page);await makeDish(page);await foodReview(page);await confirmFood(page);const dish=(await foodRoots(page)).results[0];if(dish.input.kind!=='DISH')throw Error('Expected real dish')
 const submissionId=randomUUID(),clientEntryId=randomUUID(),referenceId=dish.input.ingredients[0].referenceId,payload={submissionId,recordDate:'2020-01-02',zone:'Australia/Sydney',source:'MANUAL',items:[{clientEntryId,referenceId,quantityGrams:50,mealType:'LUNCH'}]}
 const saved=await write(page.request,'POST','/records/food/batch',payload);expect(saved.status()).toBe(201);const proof=await saved.json();expect(proof.kind).toBe('FOOD_BATCH');expect(proof.records).toHaveLength(1);expect(proof.records[0].clientEntryId).toBe(clientEntryId)
 const replay=await write(page.request,'POST','/records/food/batch',payload);expect(replay.status()).toBe(200);expect(await replay.json()).toEqual({...proof,replayed:true})
 const id=proof.records[0].id,updated=await write(page.request,'PUT','/records/food/'+id,{expectedVersion:0,recordDate:'2020-01-02',zone:'Australia/Sydney',referenceId,quantityGrams:75,mealType:'DINNER'});expect(updated.status()).toBe(200);expect(await updated.json()).toMatchObject({id,version:1,quantityGrams:75,mealType:'DINNER'})
 const receipt=await page.request.get(baseURL+'/api/submissions/'+submissionId);expect(receipt.status()).toBe(200);expect(await receipt.json()).toEqual({...proof,replayed:true});const before=inspectOwnedSyntheticData(owner.email)
 const qa=await write(page.request,'POST','/qa/messages',{message:'How are my records?',zone:'Australia/Sydney',recordRange:null});expect(qa.status()).toBe(403)
 expect(inspectOwnedSyntheticData(owner.email)).toEqual(before);expect(inspectGroups(owner.email).ledger.foodRoots).toBe(2)
})
