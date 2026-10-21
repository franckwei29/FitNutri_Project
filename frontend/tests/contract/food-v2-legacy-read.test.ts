import {describe,it,expect,vi} from 'vitest'
import {FoodRootV2Schema,FoodRootsV2Schema} from '../../src/api/schemas'
import {apiGet} from '../../src/api/client'
import {fetchFoodEntryV2} from '../../src/api/foodEntriesV2'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn(),apiPut:vi.fn(),apiDelete:vi.fn()}))
const client='22222222-2222-4222-8222-222222222222'
const nutrients={energyKj:null,proteinGrams:null,fatGrams:null,carbsGrams:null}
const legacy={id:'7',version:0,recordDate:'2020-01-02',source:'IMAGE',createdAt:'2020-01-02T00:00:00Z',updatedAt:'2020-01-02T00:00:00Z',componentCount:1,dataQuality:'LEGACY_UNVERIFIED',displayName:'Preserved synthetic legacy label',input:{kind:'SINGLE',clientEntryId:client,referenceId:null,quantityGrams:123.456,mealType:'SNACK'},preview:{kind:'SINGLE',clientEntryId:client,leaves:[{clientEntryId:client,referenceId:null,inputGrams:123.456,consumedGrams:123.456,nutrients}],totals:nutrients}}
describe('UX-B08 preserved legacy root read boundary',()=>{
 it('retains missing reference/nutrients and quality without inventing verified input',()=>{expect(FoodRootV2Schema.parse(legacy)).toEqual(legacy);expect(FoodRootsV2Schema.parse({results:[legacy],totalMatches:1,page:0,size:20,hasMore:false}).results).toEqual([legacy]);expect(FoodRootV2Schema.safeParse({...legacy,dataQuality:'CURRENT'}).success).toBe(false)})
 it('preserves large historical quantities but never silently accepts them as current input',()=>{const large={...legacy,input:{...legacy.input,quantityGrams:20000},preview:{...legacy.preview,leaves:[{...legacy.preview.leaves[0],inputGrams:20000,consumedGrams:20000}]}};expect(FoodRootV2Schema.parse(large).input).toMatchObject({quantityGrams:20000});expect(FoodRootV2Schema.safeParse({...large,dataQuality:'CURRENT'}).success).toBe(false)})
 it('rejects invalid identities, extra private fields, and negative historical metrics',()=>{for(const value of [{...legacy,id:'0'},{...legacy,userId:'99'},{...legacy,preview:{...legacy.preview,totals:{...nutrients,energyKj:-1}}}])expect(FoodRootV2Schema.safeParse(value).success).toBe(false)})
 it('checks explicit component count against stored ordered leaves before displaying a root',async()=>{vi.mocked(apiGet).mockResolvedValue(legacy);await expect(fetchFoodEntryV2('single','7')).resolves.toEqual(legacy);vi.mocked(apiGet).mockResolvedValue({...legacy,componentCount:2});await expect(fetchFoodEntryV2('single','7')).rejects.toThrow()})

})

 it('accepts original stored component labels and refuses a mismatched ordered component identity',async()=>{const root={...legacy,componentDetails:[{clientEntryId:client,name:'Original saved ingredient name',referenceId:null}]};vi.mocked(apiGet).mockResolvedValue(root);await expect(fetchFoodEntryV2('single','7')).resolves.toEqual(root);vi.mocked(apiGet).mockResolvedValue({...root,componentDetails:[{...root.componentDetails[0],clientEntryId:'33333333-3333-4333-8333-333333333333'}]});await expect(fetchFoodEntryV2('single','7')).rejects.toThrow()})
