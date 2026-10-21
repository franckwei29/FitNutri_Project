import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiGet} from '../../src/api/client'
import {fetchCapabilities} from '../../src/api/capabilities'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn()}))
const unavailable={configured:false,available:false,reason:'This AI feature is not configured.'}
const response={schemaVersion:'mvp-1',defaultZone:'Australia/Sydney',catalogueRevision:'synthetic-revision',catalogues:{food:{ready:false,verified:false,reason:'Not configured.'},exercise:{ready:true,verified:true,reason:null}},features:{image:unavailable,transcription:unavailable,speechParsing:unavailable,qa:unavailable}}
beforeEach(()=>vi.resetAllMocks())
describe('B15 capability boundary',()=>{
 it('preserves configured versus available and per-directory readiness with cancellation',async()=>{const signal=new AbortController().signal;vi.mocked(apiGet).mockResolvedValue(response);expect(await fetchCapabilities(signal)).toEqual(response);expect(apiGet).toHaveBeenCalledExactlyOnceWith('/capabilities',{signal})})
 it('rejects incompatible schema or incomplete feature flags rather than enabling defaults',async()=>{vi.mocked(apiGet).mockResolvedValue({...response,schemaVersion:'unknown'});await expect(fetchCapabilities()).rejects.toThrow('Unexpected server response');vi.mocked(apiGet).mockResolvedValue({...response,features:{...response.features,image:{available:true,reason:null}}});await expect(fetchCapabilities()).rejects.toThrow('Unexpected server response')})
 it('keeps a network failure distinct from a successful unavailable response',async()=>{vi.mocked(apiGet).mockRejectedValue(new Error('Offline'));await expect(fetchCapabilities()).rejects.toThrow('Offline');vi.mocked(apiGet).mockResolvedValue(response);expect((await fetchCapabilities()).features.image.available).toBe(false)})
})
