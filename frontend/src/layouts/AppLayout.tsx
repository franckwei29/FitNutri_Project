import {SaveFeedbackRouteBoundary} from '../features/feedback/SaveFeedbackRouteBoundary'
import {hasBusinessDraft} from '../features/entry/draftBusinessInput'
import {discardDraftForNavigation} from '../features/entry/draftNavigation'
import {useDraftNavigationLifecycle} from '../hooks/useDraftNavigationLifecycle'
import { useCallback,useMemo,useState } from 'react'
import { Outlet } from 'react-router-dom'
import {SkipToMain} from '../components/SkipToMain'
import { Header } from './Header'
import { UnsavedChangesGuard } from '../components/UnsavedChangesGuard'
import { useDraft } from '../hooks/useDraft'
import { NavigationSafetyContext } from '../app/NavigationSafetyContext'
import type { FormNavigationState } from '../app/NavigationSafetyContext'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
import {RecordMutationBoundary} from '../components/RecordMutationBoundary'
/** One router blocker combines business drafts and settings forms. */
export function AppLayout() {
  const draft=useDraft(),{state}=draft,status=pendingDraftOperation(state)
  useDraftNavigationLifecycle()
  const [forms,setForms]=useState<Record<string,FormNavigationState>>({})
  const updateForm=useCallback((key:string,value:FormNavigationState|null)=>setForms(current=>{if(!value&&!current[key]) return current;const next={...current};if(value) next[key]=value;else delete next[key];return next}),[])
  const form=useMemo(()=>{const all=Object.values(forms);return all.length?{dirty:all.some(f=>f.dirty),pending:all.some(f=>f.pending==='saving')?'saving' as const:all.some(f=>f.pending==='unknown')?'unknown' as const:null,onDiscard:()=>all.forEach(f=>f.onDiscard())}:null},[forms])
  const discardDraft=useCallback(()=>discardDraftForNavigation(draft),[draft])
  const value=useMemo(()=>({form,updateForm,discardDraft}),[form,updateForm,discardDraft])
  const pending=status==='saving'||form?.pending==='saving'?'saving':status==='unknown'||form?.pending==='unknown'?'unknown':null
  return <NavigationSafetyContext.Provider value={value}><div className="app-shell"><SkipToMain /><Header /><SaveFeedbackRouteBoundary /><RecordMutationBoundary /><Outlet /><UnsavedChangesGuard dirty={hasBusinessDraft(state.draft)||!!form?.dirty} pending={pending} settings={!!form} recordChange={state.recordMutation.status==='saving'||state.recordMutation.status==='unknown'} entryFlow={!form?.dirty} editFlow={!!state.draft?.edit||!!state.draft?.reuseFrom} onDiscard={()=>{if(!discardDraft())return false;form?.onDiscard();return true}} /></div></NavigationSafetyContext.Provider>
}
