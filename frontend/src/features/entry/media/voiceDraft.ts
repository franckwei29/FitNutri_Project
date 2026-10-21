import type {ParsedEntry} from '../../../api/contracts'
import {createFoodRow} from '../draftReducer'
import type {DraftAction} from '../draftTypes'
/** Shared draft identities/provenance, never provider candidate IDs as record/client identities. */
export function voiceImportAction(parsed:ParsedEntry,ownerId:string,expectedRevision:number):DraftAction|null {
 if(parsed.kind==='FOOD'){
  if(parsed.exercise!==null||!parsed.items.length||parsed.items.length>50)return null
  const foods=parsed.items.map(item=>({...createFoodRow(),quantityGramsInput:item.suggestedGrams===null?'':String(item.suggestedGrams),mealType:item.mealType??'' as const,suggestion:{origin:'VOICE' as const,candidateKey:item.candidateKey,proposedName:item.proposedName,referenceMatches:structuredClone(item.referenceMatches),warnings:[...item.warnings]}}))
  return {type:'IMPORT_VOICE_CANDIDATES',ownerId,expectedRevision,pending:{kind:'food',recordDate:parsed.recordDate,foods}}
 }
 if(!parsed.exercise||parsed.items.length)return null
 return {type:'IMPORT_VOICE_CANDIDATES',ownerId,expectedRevision,pending:{kind:'exercise',recordDate:parsed.recordDate,durationMinutesInput:parsed.exercise.durationMinutes===null?'':String(parsed.exercise.durationMinutes),suggestion:{origin:'VOICE',proposedName:parsed.exercise.proposedName,typeMatches:structuredClone(parsed.exercise.typeMatches),warnings:[...parsed.warnings]}}}
}
