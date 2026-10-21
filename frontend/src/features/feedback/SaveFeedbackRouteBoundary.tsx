import {useEffect} from 'react'
import {useLocation} from 'react-router-dom'
import {useSaveFeedback} from './SaveFeedbackContext'
/** Leaving the destination ends an already shown toast; StrictMode remounts do not. */
export function SaveFeedbackRouteBoundary(){const {pathname}=useLocation(),{store,value}=useSaveFeedback();useEffect(()=>{if(pathname!=='/dashboard'&&value?.shown)store.elapse(value.event.key,2200)},[pathname,value,store]);return null}
