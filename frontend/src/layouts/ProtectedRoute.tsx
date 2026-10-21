import type { ReactNode } from 'react'
import { Navigate,useLocation } from 'react-router-dom'
import { useSession } from '../hooks/useSession'
import { homeFor,safeReturnTo } from '../app/sessionRouting'
import { SessionBoundary } from './SessionBoundary'
export function ProtectedRoute({children,role='USER',mode='app'}:{children:ReactNode;role?:'USER'|'ADMIN';mode?:'app'|'setup'|'profile'}) {
  const {state}=useSession(),location=useLocation()
  if(state.status==='booting'||state.status==='error'||state.status==='logout-unconfirmed') return <SessionBoundary />
  if(state.status==='anonymous') {const to=safeReturnTo(location.pathname+location.search);return <Navigate to={'/login'+(to?'?returnTo='+encodeURIComponent(to):'')} replace />}
  if(state.user.role!==role) return <Navigate to={homeFor(state)} replace />
  if(role==='USER') {
    if(mode==='setup'&&state.profile?.onboardingCompleted) return <Navigate to={homeFor(state)} replace />
    if(mode!=='setup'&&!state.profile?.onboardingCompleted) return <Navigate to="/onboarding" replace />
    if(mode==='app'&&!state.profile?.adultConfirmed) return <Navigate to="/profile?reconfirm=adult" replace />
  }
  return children
}
