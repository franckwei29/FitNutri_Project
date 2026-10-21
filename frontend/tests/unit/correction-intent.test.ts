import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import {describe,expect,it} from 'vitest'
import {loadEditDraft,isCorrectionDirty,prepareCorrection,matchesCorrectionIntent,foodCalculationChanged} from '../../src/features/entry/correctionIntent'
import {savedFood,savedExercise} from '../fixtures/records'
import {exerciseEstimate} from '../fixtures/exercise'
describe('F13 versioned correction intent',()=>{
 it('loads the current saved identity, source and version without minting a new client entry',()=>{
  const record=savedFood({source:'IMAGE'}),draft=loadEditDraft('1','food',record)
  expect(draft.source).toBe('IMAGE');expect(draft.edit).toMatchObject({recordId:record.id,expectedVersion:2,originalRecordDate:record.recordDate});expect(draft.kind).toBe('food');if(draft.kind!=='food')throw Error('Fixture')
  expect(foodRows(draft)[0].clientEntryId).toBe(record.clientEntryId);expect(foodRows(draft)[0].rowKey).not.toBe(record.clientEntryId);record.quantityGrams=999;expect(draft.edit?.original&&'nutrients' in draft.edit.original?draft.edit.original.quantityGrams:null).toBe(150)
 })
 it('treats numerically equivalent input as unchanged and forbids a no-dirty confirmation',()=>{
  const draft=loadEditDraft('1','food',savedFood());if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].quantityGramsInput='150.000'
  expect(isCorrectionDirty(draft)).toBe(false);expect(foodCalculationChanged(draft)).toBe(false);expect(()=>prepareCorrection(draft,'1')).toThrow('No changes')
 })
 it('changes only meal/date while retaining original inactive reference and saved nutrition intent',()=>{
  const original=savedFood(),draft=loadEditDraft('1','food',original);if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].mealType='DINNER';draft.recordDate='2026-10-15'
  expect(isCorrectionDirty(draft)).toBe(true);expect(foodCalculationChanged(draft)).toBe(false);const intent=prepareCorrection(draft,'1')
  expect(intent).toMatchObject({kind:'food',recordId:original.id,body:{expectedVersion:2,referenceId:'10',quantityGrams:150,mealType:'DINNER',recordDate:'2026-10-15',zone:'Australia/Sydney'}});expect(intent.body).not.toHaveProperty('source');expect(intent.body).not.toHaveProperty('nutrients')
 })
 it('accepts valid manual gram changes without an extra checkbox and never switches owner or source',()=>{
  const draft=loadEditDraft('1','food',savedFood());if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].quantityGramsInput='200';
  expect(foodCalculationChanged(draft)).toBe(true);expect(prepareCorrection(draft,'1').body).toHaveProperty('quantityGrams',200);expect(prepareCorrection(draft,'1').body).toHaveProperty('quantityGrams',200);expect(()=>prepareCorrection(draft,'2')).toThrow();draft.source='VOICE';expect(()=>prepareCorrection(draft,'1')).toThrow('original record identity')
 })
 it('binds changed exercise inputs to the new dated fingerprint, including an owned inactive original type',()=>{
  const original=savedExercise(),draft=loadEditDraft('1','exercise',original);if(draft.kind!=='exercise'||draft.entryKind!=='QUICK')throw Error('Fixture');draft.period.durationMinutesInput='12.5'
  expect(()=>prepareCorrection(draft,'1')).toThrow('current activity estimate');expect(()=>prepareCorrection(draft,'1',exerciseEstimate())).toThrow('current activity estimate')
  const preview=exerciseEstimate({exerciseType:original.exerciseType!,recordDate:draft.recordDate,durationMinutes:12.5});expect(prepareCorrection(draft,'1',preview).body).toMatchObject({expectedVersion:2,durationMinutes:12.5,calculationFingerprint:preview.calculationFingerprint})
 })
 it('compares read-back intent without claiming which writer caused a matching later version',()=>{
  const draft=loadEditDraft('1','food',savedFood());if(draft.kind!=='food')throw Error('Fixture');foodRows(draft)[0].mealType='DINNER';const intent=prepareCorrection(draft,'1')
  expect(matchesCorrectionIntent(intent,savedFood({version:4,mealType:'DINNER'}))).toBe(true);expect(matchesCorrectionIntent(intent,savedFood({version:4,mealType:'LUNCH'}))).toBe(false);expect(matchesCorrectionIntent(intent,savedFood({version:4,mealType:'DINNER',source:'VOICE'}))).toBe(false);expect(matchesCorrectionIntent(intent,savedFood({id:'2',version:4,mealType:'DINNER'}))).toBe(false)
 })
})
