import { describe,expect,it } from 'vitest'
import { AxiosError } from 'axios'
import type { AxiosResponse } from 'axios'
import { normaliseApiError } from '../../src/api/errors'
import { shiftCalendarDate } from '../../src/domain/dates'
const item={id:'9007199254740993',recordDate:'2020-01-01',weightKg:70.25,version:2}
function error(currentItem:unknown,path='/api/profile/weight') {
  return new AxiosError('Private request data','ERR_BAD_REQUEST',undefined,undefined,{status:409,data:{status:409,code:'VERSION_CONFLICT',message:'Review the current measurement.',path,requestId:'11111111-1111-4111-8111-111111111111',fieldErrors:[],retryable:false,currentItem}} as AxiosResponse)
}
describe('F14 validated conflict and calendar boundaries',()=>{
  it('preserves only validated owner-endpoint current-item snapshots through normalization',()=>{
    const valid=normaliseApiError(error(item),'POST',true);expect(valid.currentItem).toEqual(item);expect(normaliseApiError(valid,'POST',true).currentItem).toEqual(item);expect(valid.outcome).toBe('rejected');expect(normaliseApiError(error({...item,id:123}),'POST',true).currentItem).toBeUndefined();expect(normaliseApiError(error(item,'/api/other'),'POST',true).currentItem).toBeUndefined();expect(JSON.stringify(valid)).not.toContain('Private request data')
  })
  it('shifts calendar dates across leap days and Sydney DST without changing days',()=>{
    expect(shiftCalendarDate('2024-03-01',-1)).toBe('2024-02-29');expect(shiftCalendarDate('2026-10-04',1)).toBe('2026-10-05');expect(shiftCalendarDate('2026-01-01',-1)).toBe('2025-12-31');expect(()=>shiftCalendarDate('2026-02-30',1)).toThrow()
  })
})
