import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {searchActiveFood} from '../../src/api/foodReference'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F07 remote reference contract',()=>{
 it('uses the server query and page with cancellation, keeping immutable string IDs',async()=>{
  const page={results:[],totalMatches:0,page:2,size:20,hasMore:false};vi.mocked(apiGet).mockResolvedValue(page)
  const signal=new AbortController().signal
  expect(await searchActiveFood(' rice ',2,signal)).toEqual(page)
  expect(apiGet).toHaveBeenCalledExactlyOnceWith('/food-reference/search',{params:{query:'rice',page:2,size:20},signal})
 })
 it('rejects invalid pagination and malformed reads rather than inventing a seed result',async()=>{
  await expect(searchActiveFood('rice',-1)).rejects.toThrow();expect(apiGet).not.toHaveBeenCalled()
  vi.mocked(apiGet).mockResolvedValue({results:[],totalMatches:0});await expect(searchActiveFood('rice',0)).rejects.toThrow('Unexpected server response')
  vi.mocked(apiGet).mockRejectedValue(new Error('Offline'));await expect(searchActiveFood('rice',0)).rejects.toThrow('Offline')
 })
})
