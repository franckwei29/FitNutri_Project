import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiPost} from '../../src/api/client'
import {askQuestion,qaRequestProblem} from '../../src/api/qa'
import {qaAnswer} from '../fixtures/qa'
import type {QaRequest} from '../../src/api/contracts'
vi.mock('../../src/api/client',()=>({apiPost:vi.fn()}))
const request=(recordRange:QaRequest['recordRange']=null):QaRequest=>({message:'General walking information',recordRange,zone:'Australia/Sydney'})
beforeEach(()=>vi.resetAllMocks())
describe('A09 stateless Q&A request and truthful range response contract',()=>{
 it('posts exactly one question/context selection with no history/profile/owner, signal and70s deadline',async()=>{vi.mocked(apiPost).mockResolvedValue(qaAnswer);const signal=new AbortController().signal;expect(await askQuestion(request(),signal)).toEqual(qaAnswer);expect(apiPost).toHaveBeenCalledExactlyOnceWith('/qa/messages',request(),{signal,timeoutMs:70000})})
 it('rejects empty/oversized/control/injected/range input before dispatch',async()=>{for(const value of [{...request(),message:'   '},{...request(),message:'x'.repeat(2001)},{...request(),message:'a\u0000b'},{...request(),userId:'2'},request({from:'2020-01-01',to:'2020-02-01'}),request({from:'2999-01-01',to:'2999-01-01'})]){await expect(askQuestion(value)).rejects.toThrow()}expect(apiPost).not.toHaveBeenCalled();expect(qaRequestProblem(request())).toBeNull();expect(qaRequestProblem(request({from:'2020-01-01',to:'2020-01-31'}))).toBeNull()})
 it('requires matching disclosed ranges/scopes and nonempty safe output without fixed fallback',async()=>{const range={from:'2020-01-01',to:'2020-01-02'};for(const value of ['<html>proxy</html>',{...qaAnswer,answer:''},{...qaAnswer,answer:'a\u0000b'},{...qaAnswer,disclaimer:''},{...qaAnswer,scope:'RECORD_SUMMARY'},{...qaAnswer,recordRangeUsed:range}]){vi.mocked(apiPost).mockResolvedValue(value);await expect(askQuestion(request())).rejects.toThrow('Unexpected server response')}for(const scope of ['RECORD_SUMMARY','REFUSED'] as const){vi.mocked(apiPost).mockResolvedValue({...qaAnswer,scope,recordRangeUsed:range});expect((await askQuestion(request(range))).recordRangeUsed).toEqual(range)}vi.mocked(apiPost).mockResolvedValue({...qaAnswer,scope:'REFUSED',recordRangeUsed:null});expect((await askQuestion(request(range))).scope).toBe('REFUSED');vi.mocked(apiPost).mockResolvedValue({...qaAnswer,scope:'RECORD_SUMMARY',recordRangeUsed:{from:'2020-01-02',to:'2020-01-03'}});await expect(askQuestion(request(range))).rejects.toThrow('Unexpected server response')})
})
