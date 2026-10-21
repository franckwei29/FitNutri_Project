import {useEffect,useState} from 'react'
import {Alert} from '@mui/material'
export interface SuccessNotice {id:number;message:string;startedAt?:number}
/** A verified save event keeps its original deadline even if its view remounts. */
function TimedNotice({message,startedAt}:{message:string;startedAt?:number}){
 const [mountedAt]=useState(()=>Date.now()),start=startedAt??mountedAt
 const [phase,setPhase]=useState<'visible'|'exiting'|'hidden'>(()=>{const elapsed=Math.max(0,Date.now()-start);return elapsed>=2800?'hidden':elapsed>=2500?'exiting':'visible'})
 useEffect(()=>{const elapsed=Math.max(0,Date.now()-start),fade=elapsed<2500?setTimeout(()=>setPhase('exiting'),2500-elapsed):undefined,remove=elapsed<2800?setTimeout(()=>setPhase('hidden'),2800-elapsed):undefined;return ()=>{clearTimeout(fade);clearTimeout(remove)}},[start])
 if(phase==='hidden')return null
 return <Alert severity="success" role="status" aria-live="polite" data-phase={phase} className={`transient-success ${phase}`}>{message}</Alert>
}
export function TransientSuccessAlert({notice}:{notice:SuccessNotice|null}){return notice?<TimedNotice key={notice.id} message={notice.message} startedAt={notice.startedAt} />:null}
