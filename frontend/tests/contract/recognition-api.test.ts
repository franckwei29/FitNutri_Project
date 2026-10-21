import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiPost} from '../../src/api/client'
import {recognizeImage} from '../../src/api/recognition'
import {imageRecognition} from '../fixtures/media'
vi.mock('../../src/api/client',()=>({apiPost:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('A02 real image request/response contract',()=>{
 it('uses image FormData with generated filename, signal and bounded AI timeout without JSON/base64',async()=>{const response=imageRecognition();vi.mocked(apiPost).mockResolvedValue(response);const file=new File(['synthetic-image-bytes'],'private-original.png',{type:'image/png'}),signal=new AbortController().signal;expect(await recognizeImage(file,signal)).toEqual(response);const [path,body,options]=vi.mocked(apiPost).mock.calls[0];expect(path).toBe('/food/recognize');expect(body).toBeInstanceOf(FormData);expect([...(body as FormData).keys()]).toEqual(['image']);expect(((body as FormData).get('image') as File).name).toBe('image.png');expect(options).toEqual({signal,timeoutMs:70000})})
 it('rejects invalid/empty/oversized files before dispatch',async()=>{for(const file of [new File([], 'empty.png',{type:'image/png'}),new File(['x'],'x.txt',{type:'text/plain'}),new File([new Uint8Array(8*1024*1024+1)],'large.webp',{type:'image/webp'})])await expect(recognizeImage(file)).rejects.toThrow();expect(apiPost).not.toHaveBeenCalled()})
 it('rejects HTML, old confidence DTOs, duplicate keys and invalid suggestions without saved fallback',async()=>{const response=imageRecognition();for(const value of ['<html>proxy</html>',{candidates:[{name:'old',estimatedGrams:150,confidence:0.9}]},{...response,candidates:[response.candidates[0],response.candidates[0]]},{...response,candidates:[{...response.candidates[0],suggestedGrams:0.0001}]},{...response,candidates:[{...response.candidates[0],referenceMatches:[{...response.candidates[0].referenceMatches[0],active:false}]}]}]){vi.mocked(apiPost).mockResolvedValue(value);await expect(recognizeImage(new File(['x'],'x.png',{type:'image/png'}))).rejects.toThrow('Unexpected server response')}})
})
