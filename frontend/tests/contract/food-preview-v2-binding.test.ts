import {beforeEach,it,expect,vi} from 'vitest'
import {apiPost} from '../../src/api/client'
import {previewFoodV2} from '../../src/api/foodEntriesV2'
import {foodBatchV2Fixture} from '../fixtures/entryV2'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn(),apiPut:vi.fn(),apiDelete:vi.fn()}))
const batch=foodBatchV2Fixture(),input={recordDate:batch.recordDate,zone:batch.zone,source:batch.source,entries:batch.entries},nutrients={energyKj:1200,proteinGrams:0,fatGrams:0,carbsGrams:0}
function response(){const root=input.entries[0];if(root.kind!=='DISH')throw Error('Fixture');return {entries:[{kind:root.kind,clientEntryId:root.clientEntryId,leaves:root.ingredients.map(i=>({clientEntryId:i.clientEntryId,referenceId:i.referenceId,inputGrams:i.inputGrams,consumedGrams:i.inputGrams/3,nutrients})),totals:nutrients}],totals:nutrients,fingerprint:'a'.repeat(64),calculationPolicy:'FOOD_ENTRY_V2_REFERENCE_RATIONAL_V1',inputSignature:'b'.repeat(64)}}
beforeEach(()=>vi.resetAllMocks())
it('binds ordered roots and ingredient identities, references and original grams to each preview',async()=>{const good=response();vi.mocked(apiPost).mockResolvedValue(good);await expect(previewFoodV2(input)).resolves.toEqual(good);for(const change of [(v:ReturnType<typeof response>)=>{v.entries[0].leaves.reverse()},(v:ReturnType<typeof response>)=>{v.entries[0].clientEntryId=crypto.randomUUID()},(v:ReturnType<typeof response>)=>{v.entries[0].leaves[0].referenceId='999'},(v:ReturnType<typeof response>)=>{v.entries[0].leaves[0].inputGrams=999},(v:ReturnType<typeof response>)=>{v.entries[0].leaves.pop()}]){const bad=response();change(bad);vi.mocked(apiPost).mockResolvedValue(bad);await expect(previewFoodV2(input)).rejects.toThrow()}})

it('rejects unavailable totals in a new authoritative preview',async()=>{const good=response();vi.mocked(apiPost).mockResolvedValue({...good,totals:{...good.totals,energyKj:null}});await expect(previewFoodV2(input)).rejects.toThrow()})
