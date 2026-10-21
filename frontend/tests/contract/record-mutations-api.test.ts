import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiDelete,apiPut} from '../../src/api/client'
import {updateFoodRecord,updateExerciseRecord,deleteRecord} from '../../src/api/recordMutations'
import {savedFood,savedExercise} from '../fixtures/records'
vi.mock('../../src/api/client',()=>({apiDelete:vi.fn(),apiPut:vi.fn()}))
const foodBody={expectedVersion:2,referenceId:'10',quantityGrams:150,mealType:'DINNER' as const,recordDate:'2026-10-15',zone:'Australia/Sydney'}
beforeEach(()=>vi.resetAllMocks())
describe('F13 versioned record mutation boundary',()=>{
 it('PUTs only editable fields and exact expected version, returning the saved snapshot',async()=>{
  const original=savedFood(),updated=savedFood({version:3,mealType:'DINNER',recordDate:'2026-10-15'});vi.mocked(apiPut).mockResolvedValue(updated)
  expect(await updateFoodRecord(original.id,foodBody)).toEqual(updated);expect(apiPut).toHaveBeenCalledExactlyOnceWith('/records/food/'+original.id,foodBody)
  expect(vi.mocked(apiPut).mock.calls[0][1]).not.toHaveProperty('source');expect(vi.mocked(apiPut).mock.calls[0][1]).not.toHaveProperty('nutrients')
 })
 it('allows same-version unchanged exercise acknowledgement without a new fingerprint or recalculation',async()=>{
  const record=savedExercise(),body={expectedVersion:2,exerciseTypeId:record.exerciseType!.id,durationMinutes:10.5,recordDate:record.recordDate,zone:'Australia/Sydney'};vi.mocked(apiPut).mockResolvedValue(record)
  expect(await updateExerciseRecord(record.id,body)).toEqual(record);expect(apiPut).toHaveBeenCalledExactlyOnceWith('/records/exercise/'+record.id,body)
 })
 it('rejects missing/stale-shaped version input before dispatch and treats mismatched acknowledgements as unknown',async()=>{
  await expect(updateFoodRecord('1',{...foodBody,expectedVersion:-1})).rejects.toMatchObject({outcome:'rejected',writeAttempted:false});expect(apiPut).not.toHaveBeenCalled()
  for(const response of [savedFood({id:'2',version:3,mealType:'DINNER',recordDate:'2026-10-15'}),savedFood({version:5,mealType:'DINNER',recordDate:'2026-10-15'}),savedFood({version:3,mealType:'LUNCH',recordDate:'2026-10-15'}),{}]){
   vi.mocked(apiPut).mockResolvedValue(response);await expect(updateFoodRecord('9007199254740993',foodBody)).rejects.toMatchObject({outcome:'unknown',writeAttempted:true})
  }
 })
 it('uses owner-scoped soft-delete with the exact version and does not infer success from malformed acknowledgement',async()=>{
  vi.mocked(apiDelete).mockResolvedValue(undefined);await deleteRecord('food','9007199254740993',2);expect(apiDelete).toHaveBeenCalledExactlyOnceWith('/records/food/9007199254740993',{params:{expectedVersion:2}})
  vi.mocked(apiDelete).mockResolvedValue({});await expect(deleteRecord('exercise','9007199254740993',2)).rejects.toMatchObject({outcome:'unknown',writeAttempted:true})
 })
})
