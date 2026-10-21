import { useNavigate } from 'react-router-dom'
import { Page } from '../components/presentation'
import { ProfileSettingsForm } from '../features/profile/ProfileSettingsForm'
import { useSession } from '../hooks/useSession'
export function OnboardingPage() {
  const {state}=useSession(),navigate=useNavigate()
  if(state.status!=='authenticated'||!state.profile) return null
  return <Page title="Let’s set up your profile." eyebrow="A FEW SIMPLE STEPS" subtitle="Tell us a little about yourself. Your records stay in your control." narrow><ProfileSettingsForm key={state.user.id} initial={state.profile} onboarding onSaved={()=>navigate('/dashboard',{replace:true})} /></Page>
}
