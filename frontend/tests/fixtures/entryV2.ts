import type {FoodBatchV2Request,SubmissionReceiptV2} from '../../src/api/contracts'
export const syntheticSubmissionId='11111111-1111-4111-8111-111111111111'
export function foodBatchV2Fixture():FoodBatchV2Request {
 return {submissionId:syntheticSubmissionId,recordDate:'2026-10-17',zone:'Australia/Sydney',source:'MANUAL',previewFingerprint:'a'.repeat(64),entries:[{kind:'DISH',clientEntryId:'22222222-2222-4222-8222-222222222222',name:'Synthetic recipe',mealType:'LUNCH',inputMode:'FULL_RECIPE',portion:{kind:'FRACTION',numerator:1,denominator:3},ingredients:[{clientEntryId:'33333333-3333-4333-8333-333333333333',referenceId:'101',inputGrams:300},{clientEntryId:'44444444-4444-4444-8444-444444444444',referenceId:'102',inputGrams:150}]}]}
}
export function receiptV2Fixture():SubmissionReceiptV2 {
 const batch=foodBatchV2Fixture(),root=batch.entries[0]
 if(root.kind!=='DISH')throw new Error('Fixture must contain a dish')
 return {schemaVersion:2,submissionId:batch.submissionId,kind:'FOOD_BATCH_V2',committedAt:'2026-10-17T01:00:00Z',replayed:false,roots:[{kind:'DISH',id:'1',clientEntryId:root.clientEntryId,recordDate:batch.recordDate,versionAtCreation:0}],leaves:root.ingredients.map((i,index)=>({kind:'FOOD_COMPONENT',id:String(index+1),clientEntryId:i.clientEntryId,rootClientEntryId:root.clientEntryId,recordDate:batch.recordDate,versionAtCreation:0}))}
}
