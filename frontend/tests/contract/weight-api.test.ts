import { beforeEach,describe,expect,it,vi } from 'vitest'
import { apiGet,apiPost } from '../../src/api/client'
import { fetchWeights,upsertWeight } from '../../src/api/weight'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn()}))
const weight={id:'9007199254740993',recordDate:'2020-01-01',weightKg:70.25,version:0}
beforeEach(()=>vi.resetAllMocks())
describe('F14 dated weight contract',()=>{
  it('rejects out-of-range or duplicate measurement dates and invalid ranges',async()=>{for(const rows of [[{...weight,recordDate:'2019-12-31'}],[weight,{...weight,id:'2'}]]){vi.mocked(apiGet).mockResolvedValue(rows);await expect(fetchWeights('2020-01-01','2020-01-31')).rejects.toThrow()}vi.mocked(apiGet).mockClear();await expect(fetchWeights('2020-02-01','2020-01-01')).rejects.toThrow();expect(apiGet).not.toHaveBeenCalled()})

  it('sends explicit date/null version and decodes string ID/current profile version',async()=>{
    const body={recordDate:'2020-01-01',weightKg:70.25,expectedVersion:null,zone:'Australia/Sydney'};vi.mocked(apiPost).mockResolvedValue({weight,profileVersion:1,currentWeight:weight});const result=await upsertWeight(body);expect(result.weight.id).toBe('9007199254740993');expect(apiPost).toHaveBeenCalledExactlyOnceWith('/profile/weight',body)
  })
  it('malformed write acknowledgement stays unknown and history passes bounded date parameters/signal',async()=>{
    vi.mocked(apiPost).mockResolvedValue({});await expect(upsertWeight({recordDate:'2020-01-01',weightKg:70,expectedVersion:null,zone:'Australia/Sydney'})).rejects.toMatchObject({outcome:'unknown',writeAttempted:true});vi.mocked(apiGet).mockResolvedValue([weight]);const controller=new AbortController();expect(await fetchWeights('2020-01-01','2020-01-31',controller.signal)).toEqual([weight]);expect(apiGet).toHaveBeenCalledWith('/profile/weight',{params:{from:'2020-01-01',to:'2020-01-31',zone:'Australia/Sydney'},signal:controller.signal})
  })
})
