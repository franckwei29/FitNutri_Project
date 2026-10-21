import type {ExerciseEntryPreviewV2Request,ExerciseEntryV2Request,ExercisePreviewV2} from './contracts'
import {UnexpectedApiResponse} from './validators'
type Entry=ExerciseEntryPreviewV2Request|ExerciseEntryV2Request
function scaled(value:number,scale:number):number {
 const result=Math.round(value*scale)
 if(!Number.isFinite(value)||value<0||!Number.isSafeInteger(result)||Math.abs(value-result/scale)>Math.max(1e-9,Math.abs(value)*Number.EPSILON*4))throw new UnexpectedApiResponse()
 return result
}
export function validateExercisePreviewInput(entry:Entry) {
 const periods=entry.kind==='QUICK'?[entry.period]:entry.periods
 const ids=new Set<string>([entry.clientEntryId])
 if(entry.kind==='QUICK'&&entry.period.clientEntryId!==entry.clientEntryId)throw new UnexpectedApiResponse()
 if(entry.kind==='WORKOUT')for(const period of periods){if(ids.has(period.clientEntryId))throw new UnexpectedApiResponse();ids.add(period.clientEntryId)}
 if(periods.reduce((sum,p)=>sum+scaled(p.durationMinutes,100),0)>144000)throw new UnexpectedApiResponse()
 return periods
}
/** Validate identity/status/aggregation, never recalculate MET or infer an unavailable estimate. */
export function validateExercisePreview(entry:Entry,preview:ExercisePreviewV2):void {
 const inputs=validateExercisePreviewInput(entry)
 if(inputs.length!==preview.periods.length)throw new UnexpectedApiResponse()
 let minutes=0,energy=0,available=0
 for(const [index,period] of preview.periods.entries()){
  if(period.clientEntryId!==inputs[index].clientEntryId||scaled(period.durationMinutes,100)!==scaled(inputs[index].durationMinutes,100))throw new UnexpectedApiResponse()
  minutes+=scaled(period.durationMinutes,100)
  if(period.status==='AVAILABLE'){
   if(period.estimatedKj===null||period.unavailableReason!==null)throw new UnexpectedApiResponse()
   available++;energy+=scaled(period.estimatedKj,1000)
  }else if(period.estimatedKj!==null||!period.unavailableReason?.trim())throw new UnexpectedApiResponse()
 }
 if(!Number.isSafeInteger(energy)||scaled(preview.totalMinutes,100)!==minutes||preview.partial!==(available>0&&available<preview.periods.length))throw new UnexpectedApiResponse()
 if(available===0?preview.knownEnergyKj!==null:preview.knownEnergyKj===null||scaled(preview.knownEnergyKj,1000)!==energy)throw new UnexpectedApiResponse()
}
