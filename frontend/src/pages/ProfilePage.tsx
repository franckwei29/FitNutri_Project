import { Alert } from '@mui/material'
import { Page } from '../components/presentation'
import { ProfileSettingsForm } from '../features/profile/ProfileSettingsForm'
import { WeightEntryForm } from '../features/profile/WeightEntryForm'
import { useSession } from '../hooks/useSession'
export function ProfilePage() {
  const {state}=useSession()
  if(state.status!=='authenticated'||!state.profile) return null
  return <Page title="Your profile, your preferences." eyebrow="PROFILE & GOALS" subtitle="Keep your details and everyday goals up to date.">
    {!state.profile.adultConfirmed&&<Alert severity="info">Confirm your adult details and save settings to finish updating your existing profile. Your earlier measurements are kept.</Alert>}
    <ProfileSettingsForm key={state.user.id} initial={state.profile} onboarding={false} />
    <WeightEntryForm key={'weight-'+state.user.id} />
  </Page>
}
