import {beforeEach,describe,expect,it,vi} from 'vitest'
import {apiPost} from '../../src/api/client'
import {transcribeAudio} from '../../src/api/speech'
vi.mock('../../src/api/client',()=>({apiPost:vi.fn()}))
const response={transcript:'Synthetic controlled transcript only.',notices:['Review before extraction.'],requestId:'f077c20a-60bc-4ea2-8231-34abcf1da968'}
const recording=(mimeType='audio/webm;codecs=opus')=>({blob:new Blob(['synthetic bytes'],{type:mimeType}),mimeType,durationMs:1000})
beforeEach(()=>vi.resetAllMocks())
describe('A05 actual transcription API contract with controlled transport',()=>{
 it('posts exactly the actual audio Blob with matching extension, signal and bounded decoding+provider deadline',async()=>{vi.mocked(apiPost).mockResolvedValue(response);const signal=new AbortController().signal;for(const mime of ['audio/webm;codecs=opus','audio/mp4']){expect(await transcribeAudio(recording(mime),signal)).toEqual(response);const [path,body,options]=vi.mocked(apiPost).mock.calls.at(-1)!;expect(path).toBe('/speech/transcribe');expect(body).toBeInstanceOf(FormData);const data=body as FormData;expect([...data.keys()]).toEqual(['audio']);expect((data.get('audio') as File).name).toBe(mime.includes('webm')?'audio.webm':'audio.mp4');expect((data.get('audio') as File).type).toBe(mime);expect(options).toEqual({signal,timeoutMs:80000})}})
 it('rejects unsupported/empty/oversized/overlong or mismatched audio before dispatch',async()=>{for(const value of [recording('audio/wav'),{...recording(),blob:new Blob([],{type:'audio/webm'})},{...recording(),blob:new Blob([new Uint8Array(8*1024*1024+1)],{type:'audio/webm'})},{...recording(),durationMs:60001},{...recording(),durationMs:0},{...recording(),blob:new Blob(['x'],{type:'audio/mp4'})}])await expect(transcribeAudio(value)).rejects.toThrow();expect(apiPost).not.toHaveBeenCalled()})
 it('rejects malformed or empty transcript and never substitutes an example phrase',async()=>{for(const value of ['<html>proxy</html>',{...response,transcript:''},{...response,transcript:'a\u0000b'},{...response,transcript:4},{}]){vi.mocked(apiPost).mockResolvedValue(value);await expect(transcribeAudio(recording())).rejects.toThrow('Unexpected server response')}})
})
