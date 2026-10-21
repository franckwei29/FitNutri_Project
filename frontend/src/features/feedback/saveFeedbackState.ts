import type {LastSavedV2} from '../entry/draftTypes'
/** Display metadata only. The owned draft reducer remains the authority for verified saves. */
export interface SaveEvent {key:string;domain:'FOOD'|'EXERCISE';recordDate:string;roots:{kind:'SINGLE'|'DISH'|'QUICK'|'WORKOUT';id:string}[]}
export interface SaveFeedback {event:SaveEvent;shown:boolean;navigated:boolean;remainingMs:number;dismissed:boolean;refreshFailed:boolean}
export function createSaveFeedbackStore(owner:string|null){
 let value:SaveFeedback|null=null
 const seen=new Set<string>(),listeners=new Set<()=>void>()
 function update(next:SaveFeedback){value=next;listeners.forEach(fn=>fn())}
 function change(key:string,patch:Partial<Omit<SaveFeedback,'event'>>){if(!value||value.event.key!==key||Object.entries(patch).every(([k,v])=>value?.[k as keyof SaveFeedback]===v))return;update({...value,...patch})}
 return {
  getSnapshot:()=>value,
  subscribe:(fn:()=>void)=>{listeners.add(fn);return ()=>{listeners.delete(fn)}},
  accept(saved:LastSavedV2){if(!owner)return;const key=`${owner}:${saved.ownerEpoch}:${saved.receipt.submissionId}`;if(seen.has(key))return;seen.add(key);update({event:{key,domain:saved.domain,recordDate:saved.recordDate,roots:saved.receipt.roots.map(({kind,id})=>({kind,id}))},shown:false,navigated:false,remainingMs:2000,dismissed:false,refreshFailed:saved.refreshFailed})},
  present:(key:string)=>change(key,{shown:true}),
  navigate:(key:string)=>change(key,{navigated:true}),
  dismiss:(key:string)=>change(key,{dismissed:true}),
  refresh:(key:string,failed:boolean)=>change(key,{refreshFailed:failed}),
  elapse(key:string,ms:number){if(value?.event.key===key&&Number.isFinite(ms)&&ms>0)change(key,{remainingMs:Math.max(0,value.remainingMs-ms)})},
 }
}
export type SaveFeedbackStore=ReturnType<typeof createSaveFeedbackStore>
export function saveFeedbackLocation(event:SaveEvent){const p=new URLSearchParams({from:event.recordDate,to:event.recordDate,type:event.domain==='FOOD'?'food':'exercise'});for(const root of event.roots)p.append('highlightRoot',`${root.kind.toLowerCase()}:${root.id}`);return `/history?${p}`}
export function saveFeedbackMessage(event:SaveEvent){return event.domain==='FOOD'?'Food saved':'Exercise saved'}
