import {vi} from 'vitest'
import type {RecorderState} from '../../src/features/entry/media/Recorder'
export const recorderInstances:ControlledRecorder[]=[]
export const recordingSupported=vi.fn(()=>true)
/** Explicit fake lifecycle for component tests; not a real microphone sample. */
export class ControlledRecorder {
 callback:((state:RecorderState)=>void)|null=null
 constructor(){recorderInstances.push(this)}
 start=vi.fn(async(options:{maxDurationMs:number;onState:(state:RecorderState)=>void})=>{this.callback=options.onState;this.callback({status:'recording',elapsedMs:0})})
 stop=vi.fn(async()=>{const recording={blob:new Blob(['synthetic-audio'],{type:'audio/webm;codecs=opus'}),mimeType:'audio/webm;codecs=opus',durationMs:1000};this.callback?.({status:'ready',elapsedMs:1000,recording});return recording})
 cancel=vi.fn(()=>this.callback?.({status:'idle',elapsedMs:0}))
 dispose=vi.fn()
}
