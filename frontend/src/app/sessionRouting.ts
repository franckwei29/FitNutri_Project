import type { SessionState } from './sessionEpoch'
export function safeReturnTo(value:unknown):string|null {
  if(typeof value!=='string'||value.length>2048||!value.startsWith('/')||value.startsWith('//')||value.includes('\\')||Array.from(value).some(c=>c.charCodeAt(0)<=32)) return null
  const url=new URL(value,'http://fitnutri.invalid')
  if(url.origin!=='http://fitnutri.invalid'||!['/dashboard','/history','/profile','/add','/entry','/review','/chat'].includes(url.pathname)) return null
  return url.pathname+url.search
}
export function homeFor(state:Extract<SessionState,{status:'authenticated'}>):string {
  if(state.user.role==='ADMIN') return '/admin'
  if(!state.profile?.onboardingCompleted) return '/onboarding'
  return state.profile.adultConfirmed?'/dashboard':'/profile?reconfirm=adult'
}
