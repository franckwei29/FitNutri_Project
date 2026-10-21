import {fetchExerciseOptionsV2} from './exerciseEntriesV2'
import type {ExerciseOptionV2} from './contracts'
import {IdSchema} from './schemas'
import {decodeResponse,UnexpectedApiResponse} from './validators'
/** Follow the real bounded directory; an absent immutable option is never replaced with the first result. */
export async function loadExerciseOptionV2(id:string,signal?:AbortSignal,category?:ExerciseOptionV2['category']):Promise<ExerciseOptionV2>{decodeResponse(IdSchema,id);const seen=new Set<string>();for(let page=0;page<500;page++){if(signal?.aborted)throw new Error('Activity selection was cancelled.');const result=await fetchExerciseOptionsV2('',page,signal,category);if(result.page!==page||result.results.some(row=>seen.has(row.id)))throw new UnexpectedApiResponse();for(const row of result.results){seen.add(row.id);if(row.id===id)return row}if(!result.hasMore)throw new Error('This saved activity is no longer available. Choose another activity.')}throw new UnexpectedApiResponse()}
