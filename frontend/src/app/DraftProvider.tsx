import {SaveFeedbackContext} from '../features/feedback/SaveFeedbackContext'
import {createSaveFeedbackStore} from '../features/feedback/saveFeedbackState'
import { useCallback, useMemo, useReducer, useRef,useEffect,useState } from 'react'
import type { ReactNode } from 'react'
import { DraftContext } from './DraftContext'
import { draftReducer, emptyDraftState } from '../features/entry/draftReducer'
import type { CreatePayload, DraftAction } from '../features/entry/draftTypes'
import type {ExerciseEstimate} from '../api/contracts'

function OwnedDraftProvider({ ownerId, children }: { ownerId: string | null; children: ReactNode }) {
  const [feedback]=useState(()=>createSaveFeedbackStore(ownerId))
  const [state, reactDispatch] = useReducer(draftReducer, emptyDraftState(ownerId))
  const current = useRef(emptyDraftState(ownerId))
  const undoSequence=useRef(0)
  const dispatch = useCallback((action: DraftAction) => {
    const stamped=['REMOVE_ROOT','REMOVE_FOOD','REMOVE_INGREDIENT','REMOVE_PERIOD','REMOVE_MOVEMENT'].includes(action.type)?{...action,undoToken:++undoSequence.current}:action
    const previous = current.current
    const next = draftReducer(previous, stamped)
    if (next === previous) return false
    current.current = next
    reactDispatch(stamped)
    if(next.lastSaved&&next.lastSaved!==previous.lastSaved){feedback.accept(next.lastSaved);feedback.refresh(`${ownerId}:${next.lastSaved.ownerEpoch}:${next.lastSaved.receipt.submissionId}`,next.lastSaved.refreshFailed)}
    return true
  }, [feedback,ownerId])
  const undoToken=state.undoRemoval?.token
  useEffect(()=>{if(!ownerId||undoToken===undefined)return;const timer=setTimeout(()=>dispatch({type:'EXPIRE_UNDO',ownerId,token:undoToken}),8000);return ()=>clearTimeout(timer)},[ownerId,undoToken,dispatch])
  const beginSubmission = useCallback((payload: CreatePayload, ownerEpoch: number, expectedRevision: number,reviewedEstimate?:ExerciseEstimate) => {
    if (!ownerId) return false
    return dispatch({ type: 'BEGIN_SUBMIT', ownerId, payload, ownerEpoch, expectedRevision,reviewedEstimate })
  }, [dispatch, ownerId])
  const getSnapshot=useCallback(()=>current.current,[])
  const value = useMemo(() => ({ state, dispatch, beginSubmission,getSnapshot }), [state, dispatch, beginSubmission,getSnapshot])
  return <DraftContext.Provider value={value}><SaveFeedbackContext.Provider value={feedback}>{children}</SaveFeedbackContext.Provider></DraftContext.Provider>
}
/** Ownership changes remount all draft consumers; no previous account data is rendered. */
export function DraftProvider({ ownerId, children }: { ownerId: string | null; children: ReactNode }) {
  return <OwnedDraftProvider key={ownerId ?? 'anonymous'} ownerId={ownerId}>{children}</OwnedDraftProvider>
}
