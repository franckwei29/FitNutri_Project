import { useEffect,useState,useSyncExternalStore } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import * as auth from '../api/auth'
import { fetchProfile } from '../api/profile'
import { clearCsrfToken,ensureCsrfToken } from '../api/csrf'
import { subscribeAuthExpired } from '../api/errors'
import { SessionController } from './sessionEpoch'
import { SessionContext } from './SessionContext'
import { DraftProvider } from './DraftProvider'
export function SessionProvider({children}:{children:ReactNode}) {
  const queries=useQueryClient()
  const [actions]=useState(()=>new SessionController(queries,{fetchMe:auth.fetchCurrentUser,fetchProfile,login:auth.login,logout:auth.logout,csrf:ensureCsrfToken}))
  const state=useSyncExternalStore(actions.subscribe,actions.getSnapshot)
  useEffect(()=>{
    const unsubscribe=subscribeAuthExpired(()=>{clearCsrfToken();actions.expire()})
    void actions.restore()
    return ()=>{unsubscribe();actions.dispose()}
  },[actions])
  const owner=state.status==='authenticated'?state.user.id:null
  const key=state.status==='authenticated'?`${owner}:${state.epoch}`:'anonymous'
  return <SessionContext.Provider value={{state,actions}}><DraftProvider key={key} ownerId={owner}>{children}</DraftProvider></SessionContext.Provider>
}
