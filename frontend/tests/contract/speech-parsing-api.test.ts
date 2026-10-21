import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiPost} from '../../src/api/client'
import {parseTranscript} from '../../src/api/speech'
import {todayInZone} from '../../src/domain/dates'
import {parsedFood,parsedExercise} from '../fixtures/speech'
vi.mock('../../src/api/client',()=>({apiPost:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('A06 parsing API validates kind/date/candidate authority',()=>{
 it('sends only kind, edited transcript and current explicit Sydney context with cancellation',async()=>{vi.mocked(apiPost).mockResolvedValue(parsedFood());const signal=new AbortController().signal;expect(await parseTranscript('FOOD','Edited real transcript',signal)).toEqual(parsedFood());expect(apiPost).toHaveBeenCalledExactlyOnceWith('/speech/parse',{kind:'FOOD',transcript:'Edited real transcript',baseDate:todayInZone(),zone:'Australia/Sydney'},{signal,timeoutMs:70000})})
 it('rejects empty/oversized/control text before dispatch and never invents parsed results',async()=>{for(const text of ['','   ','x'.repeat(10001),'a\u0000b'])await expect(parseTranscript('FOOD',text)).rejects.toThrow();expect(apiPost).not.toHaveBeenCalled()})
 it('rejects wrong-kind/future/duplicate/auto-authority/precision and malformed candidate shapes',async()=>{const food=parsedFood(),exercise=parsedExercise();for(const value of [exercise,{...food,recordDate:'2999-01-01'},{...food,exercise:exercise.exercise},{...food,items:[food.items[0],food.items[0]]},{...food,items:[{...food.items[0],suggestedGrams:0.0001}]},{...food,items:[{...food.items[0],referenceMatches:[{...food.items[0].referenceMatches[0],active:false}]}]},'<html>proxy</html>']){vi.mocked(apiPost).mockResolvedValue(value);await expect(parseTranscript('FOOD','rice')).rejects.toThrow('Unexpected server response')}vi.mocked(apiPost).mockResolvedValue({...exercise,items:food.items});await expect(parseTranscript('EXERCISE','walking')).rejects.toThrow('Unexpected server response')})
})
