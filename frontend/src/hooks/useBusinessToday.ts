import {useEffect,useState} from 'react'
import {todayInZone} from '../domain/dates'
export function useBusinessToday(){
  const [today,setToday]=useState(()=>todayInZone())
  useEffect(()=>{const update=()=>setToday(todayInZone()),timer=window.setInterval(update,15000);window.addEventListener('focus',update);document.addEventListener('visibilitychange',update);return ()=>{window.clearInterval(timer);window.removeEventListener('focus',update);document.removeEventListener('visibilitychange',update)}},[])
  return today
}
