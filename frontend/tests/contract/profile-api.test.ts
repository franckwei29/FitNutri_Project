import { beforeEach,describe,expect,it,vi } from 'vitest'
import { apiGet,apiPost,apiPut } from '../../src/api/client'
import { completeOnboarding,fetchProfile,updateSettings } from '../../src/api/profile'
import { initialiseProfileForm,onboardingPayload,settingsPayload } from '../../src/features/profile/profileForm'
import { profile } from '../fixtures/profile'
vi.mock('../../src/api/client',()=>({apiGet:vi.fn(),apiPost:vi.fn(),apiPut:vi.fn()}))
const baseline=profile(),form={...initialiseProfileForm(baseline,'2026-10-16'),birthYear:'1990',adultConfirmed:true,sex:'OTHER' as const,heightCm:'170',activityLevel:'LIGHT' as const,healthSafetyAnswer:'NO' as const,weightInput:'70'}
beforeEach(()=>vi.resetAllMocks())
describe('F06 profile API write/read contracts',()=>{
  it('sends one full onboarding request and independent settings PUT without weight',async()=>{
    vi.mocked(apiPost).mockResolvedValue(baseline);vi.mocked(apiPut).mockResolvedValue(baseline)
    const onboarding=onboardingPayload(form,baseline);await completeOnboarding(onboarding);expect(apiPost).toHaveBeenCalledExactlyOnceWith('/profile/onboarding',onboarding)
    const settings=settingsPayload(form,baseline);await updateSettings(settings);expect(apiPut).toHaveBeenCalledExactlyOnceWith('/profile',settings);expect(settings).not.toHaveProperty('weightKg')
  })
  it('malformed successful write response has unknown outcome, never safe-to-repeat status',async()=>{
    vi.mocked(apiPost).mockResolvedValue({onboardingCompleted:true});await expect(completeOnboarding(onboardingPayload(form,baseline))).rejects.toMatchObject({code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true});expect(apiPost).toHaveBeenCalledOnce()
  })
  it('omitted required nullable goal is rejected before dispatch and read corruption stays a read error',async()=>{
    const bad={...settingsPayload(form,baseline)} as Partial<ReturnType<typeof settingsPayload>>;delete bad.goalType
    await expect(updateSettings(bad as ReturnType<typeof settingsPayload>)).rejects.toMatchObject({outcome:'rejected',writeAttempted:false});expect(apiPut).not.toHaveBeenCalled();vi.mocked(apiGet).mockResolvedValue({});await expect(fetchProfile()).rejects.toMatchObject({code:'UNEXPECTED_RESPONSE'})
  })
})
