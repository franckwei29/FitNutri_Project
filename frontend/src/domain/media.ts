/** Input intersection actually validated by the backend, not a browser support promise. */
export const MAX_AUDIO_BYTES=8*1024*1024
export const MAX_RECORDING_MS=60000
export const RECORDING_MIME_TYPES=['audio/webm;codecs=opus','audio/webm','audio/mp4;codecs=mp4a.40.2','audio/mp4'] as const
export function audioBaseType(type:string):string{return type.split(';',1)[0].trim().toLowerCase()}
export function acceptedAudioType(type:string):boolean{return ['audio/webm','audio/mp4'].includes(audioBaseType(type))}
export interface RecordedAudio {blob:Blob;mimeType:string;durationMs:number}
