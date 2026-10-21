import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {fetchFoodRecords,fetchExerciseRecords,fetchFoodRecord} from '../../src/api/records'
import {savedFood} from '../fixtures/records'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F11 canonical bounded record reads',()=>{
 it('sends only bounded business date/zone filters and preserves empty results',async()=>{
  vi.mocked(apiGet).mockResolvedValue([]);const signal=new AbortController().signal
  expect(await fetchFoodRecords('2026-10-01','2026-10-16',signal,'LUNCH')).toEqual([])
  expect(apiGet).toHaveBeenLastCalledWith('/records/food',{params:{from:'2026-10-01',to:'2026-10-16',zone:'Australia/Sydney',mealType:'LUNCH'},signal})
  await fetchExerciseRecords('2026-10-01','2026-10-16',signal,'9007199254740993');expect(apiGet).toHaveBeenLastCalledWith('/records/exercise',{params:{from:'2026-10-01',to:'2026-10-16',zone:'Australia/Sydney',exerciseTypeId:'9007199254740993'},signal})
 })
 it('rejects malformed snapshots, out-of-range reads and mismatched immutable detail IDs',async()=>{
  await expect(fetchFoodRecords('2025-01-01','2026-10-16')).rejects.toThrow();expect(apiGet).not.toHaveBeenCalled()
  vi.mocked(apiGet).mockResolvedValue([{}]);await expect(fetchExerciseRecords('2026-10-01','2026-10-16')).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockResolvedValue({});await expect(fetchFoodRecord('1')).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockResolvedValue(savedFood({id:'2'}));await expect(fetchFoodRecord('1')).rejects.toThrow('Unexpected server response')
 })
})
