import type {FoodRootV2,ExerciseRootV2} from '../../api/contracts'
import {isValidLocalDate,isValidRecordRange,shiftCalendarDate} from '../../domain/dates'
export type RecordKind='food'|'exercise'
export interface Highlight {kind:RecordKind;id:string}
export type RootKind='single'|'dish'|'quick'|'workout'
export interface RootHighlight {kind:RootKind;id:string}
export function typedHighlightKey(target:RootHighlight){return `${target.kind}:${target.id}`}
function parseRoot(value:string):RootHighlight|null{const match=/^(single|dish|quick|workout):([1-9][0-9]{0,18})$/.exec(value);return match?{kind:match[1] as RootKind,id:match[2]}:null}
export interface HistorySearch {rootHighlights:RootHighlight[];openRoot:RootHighlight|null;valid:boolean;errors:string[];from:string;to:string;type:RecordKind|'all';highlight:Highlight|null;summary:'period'|'daily'|'weekly';summaryDate:string}
export function parseHistorySearch(params:URLSearchParams,today:string):HistorySearch{
  const errors:string[]=[],to=params.has('to')?params.get('to')!:today,from=params.has('from')?params.get('from')!:shiftCalendarDate(isValidLocalDate(to)?to:today,-6),rawType=params.get('type')??'all',rawSummary=params.get('summary')??'period',summaryDate=params.has('summaryDate')?params.get('summaryDate')!:to
  if(!isValidRecordRange(from,to)||to>today)errors.push('Choose a valid, non-future date range of 1 to 366 days.')
  if(!['all','food','exercise'].includes(rawType))errors.push('Choose All records, Food or Exercise.')
  if(!['period','daily','weekly'].includes(rawSummary)||!isValidLocalDate(summaryDate)||summaryDate>today)errors.push('Choose a valid summary mode and date.')
  let highlight:Highlight|null=null
  if(params.has('highlight')){const value=params.get('highlight')!,match=/^(food|exercise):([1-9][0-9]{0,18})$/.exec(value);if(match)highlight={kind:match[1] as RecordKind,id:match[2]};else errors.push('The highlighted record reference is invalid.')}
  const rootHighlights:RootHighlight[]=[];for(const value of params.getAll('highlightRoot')){const target=parseRoot(value);if(!target)errors.push('The highlighted root reference is invalid.');else if(!rootHighlights.some(t=>typedHighlightKey(t)===value))rootHighlights.push(target)}
  if(rootHighlights.length>100)errors.push('Choose at most100 saved roots.');const openRoot=params.has('openRoot')?parseRoot(params.get('openRoot')!):null;if(params.has('openRoot')&&!openRoot)errors.push('The opened root reference is invalid.')
  for(const key of ['from','to','type','highlight','summary','summaryDate','openRoot'])if(params.getAll(key).length>1)errors.push('Use one value for each history filter.')
  return {rootHighlights,openRoot,valid:errors.length===0,errors,from,to,type:['food','exercise'].includes(rawType)?rawType as RecordKind:'all',highlight,summary:['daily','weekly'].includes(rawSummary)?rawSummary as 'daily'|'weekly':'period',summaryDate}
}
export function savedRecordLocation(kind:RecordKind,id:string,date:string){return `/history?from=${encodeURIComponent(date)}&to=${encodeURIComponent(date)}&type=all&highlight=${kind}:${encodeURIComponent(id)}`}

export function rootTarget(root:FoodRootV2|ExerciseRootV2):RootHighlight{return {kind:root.input.kind.toLowerCase() as RootHighlight['kind'],id:root.id}}
export function rootLocation(root:FoodRootV2|ExerciseRootV2){const target=typedHighlightKey(rootTarget(root)),params=new URLSearchParams({from:root.recordDate,to:root.recordDate,type:'all',highlightRoot:target,openRoot:target});return `/history?${params}`}

export function entryRootName(root:FoodRootV2|ExerciseRootV2){return 'periodCount' in root&&root.input.kind==='QUICK'&&root.periodDetails[0]?.movements.length===1?root.periodDetails[0].movements[0].name:root.displayName}
export function correctedRootLocation(returnTo:string,root:FoodRootV2|ExerciseRootV2){const url=new URL(returnTo,'http://local.invalid'),params=url.pathname==='/history'?url.searchParams:new URLSearchParams();if(!isValidRecordRange(params.get('from')??'',params.get('to')??'')||root.recordDate<params.get('from')!||root.recordDate>params.get('to')!){params.set('from',root.recordDate);params.set('to',root.recordDate)}const domain='componentCount' in root?'food':'exercise',filter=params.get('type');if(!filter)params.set('type','all');else if(filter!=='all'&&filter!==domain)params.set('type',domain);params.delete('openRoot');params.delete('highlight');params.delete('highlightRoot');params.append('highlightRoot',typedHighlightKey(rootTarget(root)));return `/history?${params}`}
