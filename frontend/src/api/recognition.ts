import type {ImageRecognition} from './contracts'
import {ImageRecognitionSchema} from './schemas'
import {apiPost} from './client'
import {decodeResponse} from './validators'
import {ApiRequestError} from './errors'
import {parseQuantity} from '../domain/inputRules'
const extensions:Record<string,string>={'image/jpeg':'jpg','image/png':'png','image/webp':'webp'}
export function imageFileProblem(file:File):string|null {
  if(!extensions[file.type])return 'Choose a JPEG, PNG or WebP image.'
  if(file.size===0||file.size>8*1024*1024)return 'Choose a nonempty image no larger than 8 MiB.'
  return null
}
export async function recognizeImage(file:File,signal?:AbortSignal):Promise<ImageRecognition> {
  const problem=imageFileProblem(file)
  if(problem)throw new ApiRequestError({message:problem,code:'VALIDATION_ERROR',status:400,outcome:'rejected',writeAttempted:false})
  const form=new FormData();form.append('image',file,`image.${extensions[file.type]}`)
  const result=decodeResponse(ImageRecognitionSchema,await apiPost('/food/recognize',form,{signal,timeoutMs:70000}))
  const keys=new Set<string>()
  for(const row of result.candidates){
    if(!row.candidateKey||keys.has(row.candidateKey)||!row.proposedName.trim()||row.proposedName.length>100||row.suggestedGrams!==null&&!parseQuantity(String(row.suggestedGrams),'grams').ok||new Set(row.referenceMatches.map(x=>x.referenceId)).size!==row.referenceMatches.length)throw new Error('Unexpected server response. Refresh and try again.')
    if(row.referenceMatches.some(reference=>!reference.active||!reference.dataset.verified))throw new Error('Unexpected server response. Refresh and try again.')
    keys.add(row.candidateKey)
  }
  return result
}
