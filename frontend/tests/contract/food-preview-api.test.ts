import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {previewFoodPortion} from '../../src/api/foodPreview'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
const preview={reference:{referenceId:'10',publicFoodKey:'F007661',displayName:'Rice',active:true,dataset:{id:'1',name:'AUSNUT',release:'2023',revision:'r1',sourceUrl:null,nutrientPolicy:'per100g',verified:true}},quantityGrams:150,nutrients:{energyKj:988.5,proteinGrams:4.65,fatGrams:0.3,carbsGrams:52.05},calculationFingerprint:'a'.repeat(64)}
beforeEach(()=>vi.resetAllMocks())
describe('F08 authoritative food preview contract',()=>{
 it('uses immutable ID and owner-gated record context only when explicitly editing',async()=>{
  vi.mocked(apiGet).mockResolvedValue(preview);const signal=new AbortController().signal
  expect(await previewFoodPortion('10',150,signal,'99')).toEqual(preview)
  expect(apiGet).toHaveBeenCalledExactlyOnceWith('/food-reference/by-id/10/nutrition',{params:{grams:150,recordId:'99'},signal})
 })
 it('rejects mismatched, missing or malformed authoritative values',async()=>{
  vi.mocked(apiGet).mockResolvedValue({...preview,quantityGrams:100});await expect(previewFoodPortion('10',150)).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockResolvedValue({...preview,reference:{...preview.reference,referenceId:'11'}});await expect(previewFoodPortion('10',150)).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockResolvedValue({...preview,nutrients:{...preview.nutrients,energyKj:null}});await expect(previewFoodPortion('10',150)).rejects.toThrow('Unexpected server response')
 })
})
