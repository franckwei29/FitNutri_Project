import type {QaRequest,QaResponse} from './contracts'
import {QaRequestSchema,QaResponseSchema} from './schemas'
import {apiPost} from './client'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {isValidLocalDate,shiftCalendarDate,todayInZone} from '../domain/dates'
function controls(text:string){return Array.from(text).some(character=>{const code=character.codePointAt(0)!;return (code<32&&![9,10,13].includes(code))||(code>=127&&code<=159)})}
export function qaRequestProblem(request:QaRequest):string|null {
 if(!QaRequestSchema.safeParse(request).success)return 'Check the question and selected date fields.'
 if(!request.message.trim()||request.message.length>2000||controls(request.message))return 'Enter a plain-text question of 1 to 2,000 characters.'
 let today:string;try{today=todayInZone(request.zone)}catch{return 'Choose a valid application time zone.'}
 const range=request.recordRange
 if(range&&(!isValidLocalDate(range.from)||!isValidLocalDate(range.to)||range.from>range.to||range.to>today||range.to>shiftCalendarDate(range.from,30)))return 'Choose an inclusive record range of 1 to 31 days that is not in the future.'
 return null
}
/** One stateless question. No browser message history, profile or client-produced facts are sent. */
export async function askQuestion(request:QaRequest,signal?:AbortSignal):Promise<QaResponse>{
 const problem=qaRequestProblem(request);if(problem)throw new Error(problem)
 const input=decodeResponse(QaRequestSchema,request),response=decodeResponse(QaResponseSchema,await apiPost('/qa/messages',input,{signal,timeoutMs:70000}))
 if(!response.answer.trim()||!response.disclaimer.trim()||response.disclaimer.length>2000||controls(response.answer)||controls(response.disclaimer))throw new UnexpectedApiResponse()
 const requested=input.recordRange,used=response.recordRangeUsed,same=!!requested&&!!used&&requested.from===used.from&&requested.to===used.to
 if((used!==null&&!same)||(response.scope==='RECORD_SUMMARY'&&(!requested||!same))||(response.scope==='GENERAL'&&(requested!==null||used!==null)))throw new UnexpectedApiResponse()
 return response
}
