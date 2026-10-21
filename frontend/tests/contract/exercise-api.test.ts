import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {loadExerciseTypes,previewDatedExercise} from '../../src/api/exerciseTypes'
import {exerciseEstimate,exerciseType} from '../fixtures/exercise'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F09 dated exercise API',()=>{
 it('loads structured official metadata and sends decimal duration, date, zone and optional owned record ID',async()=>{
  vi.mocked(apiGet).mockResolvedValue([exerciseType]);expect((await loadExerciseTypes())[0].sourceActivityCode).toBe('17160')
  vi.mocked(apiGet).mockResolvedValue(exerciseEstimate());const signal=new AbortController().signal
  expect((await previewDatedExercise(exerciseType.id,10.5,'2026-10-16',signal,'81')).weightDateUsed).toBe('2026-10-15')
  expect(apiGet).toHaveBeenLastCalledWith('/records/exercise/estimate',{params:{exerciseTypeId:exerciseType.id,durationMinutes:10.5,recordDate:'2026-10-16',zone:'Australia/Sydney',recordId:'81'},signal})
 })
 it('rejects mismatched physical inputs and impossible available/unavailable snapshots',async()=>{
  for(const wrong of [exerciseEstimate({durationMinutes:10}),exerciseEstimate({recordDate:'2026-10-15'}),exerciseEstimate({weightKgUsed:null}),exerciseEstimate({estimateStatus:'MISSING_WEIGHT',unavailableReason:'No weight'})]){
   vi.mocked(apiGet).mockResolvedValue(wrong);await expect(previewDatedExercise(exerciseType.id,10.5,'2026-10-16')).rejects.toThrow('Unexpected server response')
  }
 })
})
