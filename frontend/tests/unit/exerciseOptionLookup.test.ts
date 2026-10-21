import {beforeEach,it,expect,vi} from 'vitest'
import {fetchExerciseOptionsV2} from '../../src/api/exerciseEntriesV2'
import {loadExerciseOptionV2} from '../../src/api/exerciseOptionLookup'
vi.mock('../../src/api/exerciseEntriesV2',()=>({fetchExerciseOptionsV2:vi.fn()}))
const option={id:'201',optionKey:'synthetic',revision:'r1',displayName:'Synthetic',category:'STRENGTH' as const,periodKind:'STRENGTH' as const,mappingKind:'GENERAL_ACTIVITY_ESTIMATE' as const,allowedDetails:[],referenceId:'301',mappingReason:'Synthetic test only'}
beforeEach(()=>vi.resetAllMocks())
it('follows real pages to the exact immutable option and never returns the first unrelated option',async()=>{vi.mocked(fetchExerciseOptionsV2).mockResolvedValueOnce({results:[{...option,id:'200'}],page:0,size:20,totalMatches:2,hasMore:true}).mockResolvedValueOnce({results:[option],page:1,size:20,totalMatches:2,hasMore:false});const signal=new AbortController().signal;await expect(loadExerciseOptionV2('201',signal,'STRENGTH')).resolves.toEqual(option);expect(fetchExerciseOptionsV2).toHaveBeenNthCalledWith(2,'',1,signal,'STRENGTH')})
it('explains an absent original option instead of substituting a different reference',async()=>{vi.mocked(fetchExerciseOptionsV2).mockResolvedValue({results:[{...option,id:'200'}],page:0,size:20,totalMatches:1,hasMore:false});await expect(loadExerciseOptionV2('201')).rejects.toThrow('no longer available')})
it('rejects invalid identity and aborted work before a directory request',async()=>{await expect(loadExerciseOptionV2('0')).rejects.toThrow();const controller=new AbortController();controller.abort();await expect(loadExerciseOptionV2('201',controller.signal)).rejects.toThrow();expect(fetchExerciseOptionsV2).not.toHaveBeenCalled()})
