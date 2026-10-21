import {beforeEach,it,expect,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {fetchExerciseOptionsV2,fetchMovementsV2} from '../../src/api/exerciseEntriesV2'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn(),apiPut:vi.fn(),apiDelete:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
it('binds catalogue pagination to the requested page and refuses duplicate immutable IDs',async()=>{vi.mocked(apiGet).mockResolvedValue({results:[],totalMatches:0,page:1,size:20,hasMore:false});await expect(fetchExerciseOptionsV2('row',0)).rejects.toThrow();await expect(fetchMovementsV2('row',0)).rejects.toThrow();const option={id:'201',optionKey:'synthetic',revision:'r1',displayName:'Synthetic',category:'CARDIO',periodKind:'ACTIVITY',mappingKind:'UNAVAILABLE',allowedDetails:[],referenceId:null,mappingReason:'Synthetic only'};vi.mocked(apiGet).mockResolvedValue({results:[option,option],totalMatches:2,page:0,size:20,hasMore:false});await expect(fetchExerciseOptionsV2('row',0)).rejects.toThrow()})
