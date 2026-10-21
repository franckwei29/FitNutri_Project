import {acceptedAudioType,audioBaseType,MAX_AUDIO_BYTES,MAX_RECORDING_MS,RECORDING_MIME_TYPES} from '../../../domain/media'
import type {RecordedAudio} from '../../../domain/media'
export type {RecordedAudio} from '../../../domain/media'
export type RecorderState=
 | {status:'idle'|'requestingPermission'|'recording'|'stopping';elapsedMs:number}
 | {status:'ready';elapsedMs:number;recording:RecordedAudio}
 | {status:'error';elapsedMs:number;code:string;message:string}
interface Options {maxDurationMs:number;onState:(state:RecorderState)=>void}
interface Capture {
 onState:Options['onState'];chunks:Blob[];bytes:number;mimeType:string;completion:Promise<RecordedAudio>;resolve:(value:RecordedAudio)=>void;reject:(error:RecorderError)=>void
 stream?:MediaStream;native?:MediaRecorder;startedAt?:number;stoppedAt?:number;stopping:boolean;remove:(()=>void)[];limitTimer?:ReturnType<typeof setTimeout>;flushTimer?:ReturnType<typeof setTimeout>;tickTimer?:ReturnType<typeof setInterval>
}
export class RecorderError extends Error {readonly code:string;constructor(code:string,message:string){super(message);this.name='RecorderError';this.code=code}}
function chooseMime():string|null {if(typeof MediaRecorder==='undefined'||typeof MediaRecorder.isTypeSupported!=='function')return null;try{return RECORDING_MIME_TYPES.find(type=>MediaRecorder.isTypeSupported(type))??null}catch{return null}}
export function isSupported():boolean{return globalThis.isSecureContext===true&&typeof navigator!=='undefined'&&typeof navigator.mediaDevices?.getUserMedia==='function'&&chooseMime()!==null}
/** No automatic microphone request, upload or business draft dependency. Owns one capture at a time. */
export class Recorder {
 private active:Capture|null=null
 private disposed=false
 private last:RecordedAudio|null=null
 private onState:Options['onState']|null=null
 async start(options:Options):Promise<void>{
  if(this.disposed)throw new RecorderError('RECORDER_DISPOSED','This recorder is no longer available.')
  if(this.active)throw new RecorderError('RECORDING_BUSY','Finish or cancel the current recording first.')
  this.last=null;this.onState=options.onState
  if(!isSupported()){const error=new RecorderError('UNSUPPORTED_RECORDING','Audio recording is unavailable in this browser. Continue with manual entry.');options.onState({status:'error',elapsedMs:0,code:error.code,message:error.message});throw error}
  if(!Number.isFinite(options.maxDurationMs)||options.maxDurationMs<=0)throw new RecorderError('INVALID_RECORDING_LIMIT','Choose a positive recording duration.')
  let resolve!:Capture['resolve'],reject!:Capture['reject'];const completion=new Promise<RecordedAudio>((yes,no)=>{resolve=yes;reject=no});void completion.catch(()=>{}) // automatic stop/error may occur before a caller requests stop()
  const capture:Capture={onState:options.onState,chunks:[],bytes:0,mimeType:chooseMime()!,completion,resolve,reject,stopping:false,remove:[]};this.active=capture;options.onState({status:'requestingPermission',elapsedMs:0})
  try{
   const stream=await navigator.mediaDevices.getUserMedia({audio:true})
   if(this.active!==capture||this.disposed){stream.getTracks().forEach(track=>track.stop());return}
   capture.stream=stream
   const native=new MediaRecorder(stream,{mimeType:capture.mimeType});capture.native=native
   if(!acceptedAudioType(native.mimeType))throw new RecorderError('UNSUPPORTED_RECORDING','The browser did not produce supported WebM or MP4 audio. Continue manually.')
   capture.mimeType=native.mimeType
   this.listen(capture,native,'dataavailable',event=>this.chunk(capture,(event as BlobEvent).data))
   this.listen(capture,native,'stop',()=>this.complete(capture))
   this.listen(capture,native,'error',()=>this.fail(capture,new RecorderError('RECORDING_FAILED','Audio recording failed. Check the microphone or continue manually.')))
   for(const track of stream.getTracks())this.listen(capture,track,'ended',()=>{if(!capture.stopping)this.fail(capture,new RecorderError('MICROPHONE_LOST','The microphone became unavailable. Record again or continue manually.'))})
   capture.startedAt=performance.now();native.start(250)
   // Leave room for event-loop scheduling and codec padding before the strict server limit.
   capture.limitTimer=setTimeout(()=>{if(this.active===capture)void this.stop().catch(()=>{})},Math.min(options.maxDurationMs,MAX_RECORDING_MS-250))
   capture.tickTimer=setInterval(()=>{if(this.active===capture&&!capture.stopping)capture.onState({status:'recording',elapsedMs:this.elapsed(capture)})},500)
   capture.onState({status:'recording',elapsedMs:0})
  }catch(cause){if(this.active!==capture||this.disposed)return;const error=this.safeError(cause);this.fail(capture,error);throw error}
 }
 stop():Promise<RecordedAudio>{
  const capture=this.active
  if(!capture)return this.last?Promise.resolve(this.last):Promise.reject(new RecorderError('NOT_RECORDING','Start a recording before stopping.'))
  if(capture.startedAt===undefined){const pending=capture.completion;this.cancel();return pending}
  if(capture.stopping)return capture.completion
  capture.stopping=true;capture.stoppedAt=performance.now();clearTimeout(capture.limitTimer);clearInterval(capture.tickTimer)
  capture.onState({status:'stopping',elapsedMs:this.elapsed(capture)})
  // Native stop enqueues the final dataavailable before stop; retain listeners until that event.
  capture.flushTimer=setTimeout(()=>this.fail(capture,new RecorderError('RECORDING_FAILED','The recording could not be finalized. Try again or continue manually.')),5000)
  try{capture.native!.stop();capture.stream?.getTracks().forEach(track=>track.stop())}catch{this.fail(capture,new RecorderError('RECORDING_FAILED','The recording could not be finalized. Try again or continue manually.'))}
  return capture.completion
 }
 cancel():void{const capture=this.active;this.active=null;this.last=null;if(capture){this.release(capture);capture.reject(new RecorderError('RECORDING_CANCELED','Recording canceled.'))}if(!this.disposed)this.onState?.({status:'idle',elapsedMs:0})}
 dispose():void{this.disposed=true;this.cancel();this.onState=null}
 private listen(capture:Capture,target:EventTarget,type:string,listener:EventListener){target.addEventListener(type,listener);capture.remove.push(()=>target.removeEventListener(type,listener))}
 private elapsed(capture:Capture){return capture.startedAt===undefined?0:Math.max(0,Math.ceil((capture.stoppedAt??performance.now())-capture.startedAt))}
 private chunk(capture:Capture,data:Blob){
  if(this.active!==capture||this.disposed||!data.size)return
  if(data.type&&audioBaseType(data.type)!==audioBaseType(capture.mimeType)){this.fail(capture,new RecorderError('UNSUPPORTED_RECORDING','The browser changed recording format. Record again or continue manually.'));return}
  if(data.size>MAX_AUDIO_BYTES-capture.bytes){this.fail(capture,new RecorderError('RECORDING_TOO_LARGE','The recording exceeds 8 MiB. Record a shorter clip.'));return}
  capture.chunks.push(data);capture.bytes+=data.size
 }
 private complete(capture:Capture){
  if(this.active!==capture||this.disposed)return
  const durationMs=this.elapsed(capture)
  if(durationMs>MAX_RECORDING_MS){this.fail(capture,new RecorderError('RECORDING_TOO_LONG','The recording exceeded 60 seconds. Record a shorter clip.'));return}
  const blob=new Blob(capture.chunks,{type:capture.mimeType})
  if(!blob.size){this.fail(capture,new RecorderError('EMPTY_RECORDING','No audio was recorded. Record again or continue manually.'));return}
  const recording={blob,mimeType:capture.mimeType,durationMs};this.active=null;this.release(capture);this.last=recording;capture.resolve(recording);capture.onState({status:'ready',elapsedMs:durationMs,recording})
 }
 private fail(capture:Capture,error:RecorderError){if(this.active!==capture||this.disposed)return;const elapsedMs=this.elapsed(capture);this.active=null;this.release(capture);this.last=null;capture.reject(error);capture.onState({status:'error',elapsedMs,code:error.code,message:error.message})}
 private release(capture:Capture){
  clearTimeout(capture.limitTimer);clearTimeout(capture.flushTimer);clearInterval(capture.tickTimer);capture.remove.forEach(remove=>remove());capture.remove=[]
  if(capture.native&&capture.native.state!=='inactive')try{capture.native.stop()}catch{/* Native cleanup failure cannot retain tracks. */}
  capture.stream?.getTracks().forEach(track=>{if(track.readyState!=='ended')track.stop()});capture.chunks=[];capture.bytes=0;capture.native=undefined;capture.stream=undefined
 }
 private safeError(cause:unknown):RecorderError{if(cause instanceof RecorderError)return cause;const name=cause instanceof DOMException?cause.name:'';if(name==='NotAllowedError'||name==='SecurityError')return new RecorderError('MICROPHONE_DENIED','Microphone access was not allowed. Enable access or continue manually.');if(name==='NotFoundError')return new RecorderError('MICROPHONE_MISSING','No microphone was found. Connect one or continue manually.');return new RecorderError('RECORDING_FAILED','The microphone could not start. Try again or continue manually.')}
}
