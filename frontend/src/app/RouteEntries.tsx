import { Navigate,useSearchParams } from 'react-router-dom'
import {lazy,Suspense} from 'react'
import {RouteLoading} from './RouteStatus'
const AuthPage=lazy(()=>import('../pages/AuthPage').then(m=>({default:m.AuthPage})))
import { SessionBoundary } from '../layouts/SessionBoundary'
import { useSession } from '../hooks/useSession'
import { homeFor,safeReturnTo } from './sessionRouting'
export function LoginRoute() {
  const {state}=useSession(),[params]=useSearchParams()
  if(state.status==='booting'||state.status==='error'||state.status==='logout-unconfirmed') return <SessionBoundary />
  if(state.status==='authenticated') {
    const target=state.user.role==='USER'&&state.profile?.onboardingCompleted&&state.profile.adultConfirmed?safeReturnTo(params.get('returnTo')):null
    return <Navigate to={target??homeFor(state)} replace />
  }
  return <Suspense fallback={<RouteLoading />}><AuthPage /></Suspense>
}
export function Home() {const {state}=useSession();if(state.status==='booting'||state.status==='error'||state.status==='logout-unconfirmed') return <SessionBoundary />;return <Navigate to={state.status==='authenticated'?homeFor(state):'/login'} replace />}
