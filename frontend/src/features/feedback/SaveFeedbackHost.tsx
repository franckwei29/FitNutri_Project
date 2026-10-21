import {createPortal} from 'react-dom'
import {useEffect,useState} from 'react'
import {Alert,useMediaQuery} from '@mui/material'
import {useLocation} from 'react-router-dom'
import {useSaveFeedback} from './SaveFeedbackContext'
import {saveFeedbackMessage} from './saveFeedbackState'
/** Mounted by the destination, so lazy route time never consumes the notice. */
export function SaveFeedbackHost({active}:{active?:boolean}){
 const {store,value}=useSaveFeedback(),location=useLocation(),enabled=active??location.pathname==='/dashboard',key=value?.event.key
 const reduced=useMediaQuery('(prefers-reduced-motion: reduce)'),[visible,setVisible]=useState(()=>document.visibilityState!=='hidden')
  useEffect(()=>{const update=()=>setVisible(document.visibilityState!=='hidden');document.addEventListener('visibilitychange',update);return ()=>document.removeEventListener('visibilitychange',update)},[])
 useEffect(()=>{if(!enabled||!visible||!key)return;store.present(key);let anchor=Date.now();const tick=()=>{const now=Date.now();store.elapse(key,Math.max(0,now-anchor));anchor=now};const remaining=store.getSnapshot()?.remainingMs??0,first=setTimeout(tick,Math.max(0,remaining-200)),end=setTimeout(()=>{tick()},Math.max(0,remaining));return ()=>{clearTimeout(first);clearTimeout(end);tick()}},[enabled,visible,key,store,reduced])
 if(!enabled||!visible||!value||value.remainingMs<=0)return null
 return createPortal(<div className="save-feedback-toast"><Alert severity="success" role="status" aria-live="polite" aria-atomic="true" data-phase={value.remainingMs<=200?'exiting':'visible'}>{saveFeedbackMessage(value.event)}</Alert></div>,document.body)
}
