import { createContext } from 'react'
import type { PendingSubmission } from '../hooks/useUnsavedChanges'
export interface FormNavigationState {dirty:boolean;pending:PendingSubmission;onDiscard:()=>void}
export const NavigationSafetyContext=createContext<{form:FormNavigationState|null;discardDraft?:()=>boolean;updateForm:(key:string,value:FormNavigationState|null)=>void}|null>(null)
