import type {Transcription,ParsedEntry,ParseSpeechRequest} from './contracts'
import {TranscriptionSchema,ParsedEntrySchema} from './schemas'
import {apiPost} from './client'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {acceptedAudioType,audioBaseType,MAX_AUDIO_BYTES,MAX_RECORDING_MS} from '../domain/media'
import {parseQuantity} from '../domain/inputRules'
import {todayInZone,DEFAULT_ZONE} from '../domain/dates'
import type {RecordedAudio} from '../domain/media'
/** Actual browser Blob, never fabricated WAV MIME or a transcript placeholder. Server still decodes. */
export async function transcribeAudio(recording:RecordedAudio,signal?:AbortSignal):Promise<Transcription>{
 const {blob,mimeType,durationMs}=recording
 if(!acceptedAudioType(mimeType)||audioBaseType(blob.type)!==audioBaseType(mimeType)||!blob.size||blob.size>MAX_AUDIO_BYTES||!Number.isFinite(durationMs)||durationMs<=0||durationMs>MAX_RECORDING_MS)throw new Error('Record supported WebM or MP4 audio up to 60 seconds and 8 MiB.')
 const form=new FormData();form.append('audio',blob,audioBaseType(mimeType)==='audio/webm'?'audio.webm':'audio.mp4')
 const response=decodeResponse(TranscriptionSchema,await apiPost('/speech/transcribe',form,{signal,timeoutMs:80000}))
 if(!response.transcript.trim()||Array.from(response.transcript).some(character=>{const code=character.codePointAt(0)!;return (code<32&&![9,10,13].includes(code))||(code>=127&&code<=159)}))throw new UnexpectedApiResponse()
 return response
}

export async function parseTranscript(kind:ParseSpeechRequest['kind'],transcript:string,signal?:AbortSignal):Promise<ParsedEntry>{
 if(!transcript.trim()||transcript.length>10000||Array.from(transcript).some(character=>{const code=character.codePointAt(0)!;return (code<32&&![9,10,13].includes(code))||(code>=127&&code<=159)}))throw new Error('Enter 1 to 10,000 characters of editable transcript text.')
 const baseDate=todayInZone(),result=decodeResponse(ParsedEntrySchema,await apiPost('/speech/parse',{kind,transcript,baseDate,zone:DEFAULT_ZONE},{signal,timeoutMs:70000}))
 if(result.kind!==kind||result.recordDate>baseDate||result.dateWasDefaulted&&result.recordDate!==baseDate)throw new UnexpectedApiResponse()
 if(kind==='FOOD'){
  const keys=new Set<string>()
  if(result.exercise!==null)throw new UnexpectedApiResponse()
  for(const item of result.items){if(!item.candidateKey||keys.has(item.candidateKey)||!item.proposedName.trim()||item.proposedName.length>100||item.suggestedGrams!==null&&!parseQuantity(String(item.suggestedGrams),'grams').ok||item.referenceMatches.some(reference=>!reference.active||!reference.dataset.verified)||new Set(item.referenceMatches.map(x=>x.referenceId)).size!==item.referenceMatches.length)throw new UnexpectedApiResponse();keys.add(item.candidateKey)}
 }else{
  const exercise=result.exercise
  if(result.items.length||!exercise||exercise.proposedName!==null&&(!exercise.proposedName.trim()||exercise.proposedName.length>100)||exercise.durationMinutes!==null&&!parseQuantity(String(exercise.durationMinutes),'minutes').ok||exercise.typeMatches.some(type=>!type.active||!type.dataset.verified)||new Set(exercise.typeMatches.map(x=>x.id)).size!==exercise.typeMatches.length)throw new UnexpectedApiResponse()
 }
 return result
}
