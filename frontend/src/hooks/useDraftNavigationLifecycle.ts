import {useLayoutEffect,useRef} from 'react'
import {useLocation} from 'react-router-dom'
import {useDraft} from './useDraft'
import {hasBusinessDraft} from '../features/entry/draftBusinessInput'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
/** Clears page-only saved context and empty inputs after actually leaving the entry flow. */
export function useDraftNavigationLifecycle(){const {pathname}=useLocation(),{getSnapshot,dispatch}=useDraft(),previous=useRef(pathname);useLayoutEffect(()=>{const from=previous.current;previous.current=pathname;const state=getSnapshot(),ownerId=state.ownerId;if(!ownerId||from===pathname)return;if(pathname==='/review')dispatch({type:'END_UNDO_WINDOW',ownerId});if(['/entry','/review'].includes(from)&&!['/entry','/review'].includes(pathname)){if(state.draft&&!hasBusinessDraft(state.draft)&&!pendingDraftOperation(state))dispatch({type:'DISCARD',ownerId});else if(state.lastSaved)dispatch({type:'CLEAR_LAST_SAVED',ownerId,submissionId:state.lastSaved.receipt.submissionId})}},[pathname,dispatch,getSnapshot])}
