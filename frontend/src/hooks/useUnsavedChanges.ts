import {allowInternalFlowNavigation} from '../features/entry/draftNavigation'
import { useEffect } from 'react'
import { useBlocker } from 'react-router-dom'

export type PendingSubmission = 'saving' | 'unknown' | null
export function useUnsavedChanges(dirty: boolean, pending: PendingSubmission, entryFlow = false,editFlow=false) {
  const blocker = useBlocker(({ currentLocation, nextLocation }) => {
    if (pending) return true
    const insideFlow=allowInternalFlowNavigation(currentLocation.pathname,nextLocation.pathname,entryFlow,editFlow)
    return dirty && !insideFlow
  })
  useEffect(() => {
    if (!dirty && !pending) return
    const beforeUnload = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', beforeUnload)
    return () => window.removeEventListener('beforeunload', beforeUnload)
  }, [dirty, pending])
  useEffect(() => {
    if (!dirty && !pending && blocker.state === 'blocked') blocker.reset()
  }, [dirty, pending, blocker])
  return blocker
}
