import {createContext,useContext,useSyncExternalStore} from 'react'
import type {SaveFeedbackStore} from './saveFeedbackState'
export const SaveFeedbackContext=createContext<SaveFeedbackStore|null>(null)
export function useSaveFeedback(){const store=useContext(SaveFeedbackContext);if(!store)throw Error('Save feedback requires its owned provider.');const value=useSyncExternalStore(store.subscribe,store.getSnapshot);return {store,value}}
