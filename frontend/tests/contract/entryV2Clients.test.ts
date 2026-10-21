import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet,apiPost,apiPut,apiDelete} from '../../src/api/client'
import {fetchFoodEntryV2,fetchFoodEntriesV2,previewFoodV2,deleteFoodEntryV2} from '../../src/api/foodEntriesV2'
import {fetchExerciseEntryV2,fetchExerciseEntriesV2,fetchExerciseOptionsV2,fetchMovementsV2,deleteExerciseEntryV2} from '../../src/api/exerciseEntriesV2'
import {foodBatchV2Fixture} from '../fixtures/entryV2'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn(),apiPut:vi.fn(),apiDelete:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('UX-F03 strict V2 transport adapters',()=>{
 it('bounds ranges and IDs before sending private reads',async()=>{
  await expect(fetchFoodEntriesV2('2026-02-30','2026-10-17')).rejects.toThrow()
  await expect(fetchFoodEntryV2('dish','1/path')).rejects.toThrow()
  await expect(fetchExerciseEntryV2('workout','0')).rejects.toThrow()
  expect(apiGet).not.toHaveBeenCalled()
 })
 it('requires a decoded catalogue response and correct distinct option/movement routes',async()=>{
  const page={results:[],page:0,size:20,totalMatches:0,hasMore:false}
  vi.mocked(apiGet).mockResolvedValue(page)
  await expect(fetchExerciseOptionsV2('rowing')).resolves.toEqual(page)
  await expect(fetchMovementsV2('row')).resolves.toEqual(page)
  expect(apiGet).toHaveBeenCalledWith('/v2/exercise/options',expect.objectContaining({params:{query:'rowing',page:0,size:20}}))
  expect(apiGet).toHaveBeenCalledWith('/v2/exercise/movements',expect.objectContaining({params:{query:'row',page:0,size:20}}))
  vi.mocked(apiGet).mockResolvedValue({...page,privateData:'unexpected'})
  await expect(fetchMovementsV2('row')).rejects.toThrow()
 })
 it('passes expected versions for entire-root deletes and never calls child create',async()=>{
  await deleteFoodEntryV2('dish','1',2);await deleteExerciseEntryV2('workout','1',3)
  expect(apiDelete).toHaveBeenCalledWith('/v2/food/entries/dish/1',{params:{expectedVersion:2}})
  expect(apiDelete).toHaveBeenCalledWith('/v2/exercise/entries/workout/1',{params:{expectedVersion:3}})
  expect(apiPost).not.toHaveBeenCalled();expect(apiPut).not.toHaveBeenCalled()
 })
 it('does not send a malformed or empty preview request',async()=>{
  const batch=foodBatchV2Fixture()
  await expect(previewFoodV2({recordDate:batch.recordDate,zone:batch.zone,source:batch.source,entries:[]})).rejects.toThrow()
  expect(apiPost).not.toHaveBeenCalled()
 })
})

 it('rejects root pages with wrong requested paging metadata and invalid page commands',async()=>{for(const fetch of [fetchFoodEntriesV2,fetchExerciseEntriesV2]){vi.mocked(apiGet).mockResolvedValue({results:[],page:2,size:20,totalMatches:0,hasMore:false});await expect(fetch('2020-01-02','2020-01-02',0)).rejects.toThrow();await expect(fetch('2020-01-02','2020-01-02',-1)).rejects.toThrow()}})
