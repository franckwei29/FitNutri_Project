import { describe,expect,it } from 'vitest'
import { initialiseProfileForm,changeEnergyUnit,editEnergyTarget,validateProfileForm,settingsPayload,onboardingPayload,stepForField } from '../../src/features/profile/profileForm'
import { profile,user } from '../fixtures/profile'
describe('F06/F14 full profile form boundary',()=>{
  it('retains physical kJ target when switching display units without editing',()=>{
    const form=initialiseProfileForm(profile({dailyTargetKj:8400.123}), '2026-10-16');const kcal=changeEnergyUnit(form,'KCAL');expect(kcal.dailyTargetKj).toBe(8400.123);expect(changeEnergyUnit(kcal,'KJ').dailyTargetKj).toBe(8400.123)
  })
  it('converts explicit kcal input with decimal rounding exactly once and preserves empty null',()=>{
    let form=changeEnergyUnit(initialiseProfileForm(profile(),'2026-10-16'),'KCAL');form=editEnergyTarget(form,'2000.001');expect(form.dailyTargetKj).toBe(8368.004);expect(editEnergyTarget(form,'').dailyTargetKj).toBeNull();expect(editEnergyTarget(form,'0').dailyTargetKj).toBeUndefined();expect(editEnergyTarget(form,'1e3').dailyTargetKj).toBeUndefined()
  })
  it('builds one complete onboarding snapshot including explicit false consent and initial weight',()=>{
    const baseline=profile();const form={...initialiseProfileForm(baseline,'2026-10-16'),displayName:' Synthetic ',birthYear:'1990',adultConfirmed:true,sex:'OTHER' as const,heightCm:'170',activityLevel:'LIGHT' as const,healthSafetyAnswer:'NO' as const,weightInput:'70.25'};
    expect(validateProfileForm(form,'2026-10-16',true)).toEqual({});const request=onboardingPayload(form,baseline);expect(request.expectedUserVersion).toBe(user.version);expect(request.expectedProfileVersion).toBeNull();expect(request.qaConsentGranted).toBe(false);expect(request.initialWeight).toEqual({recordDate:'2026-10-16',weightKg:70.25});expect(request.goalType).toBeNull();expect(request.dailyTargetKj).toBeNull();expect(request.displayName).toBe('Synthetic');expect(settingsPayload(form,baseline)).not.toHaveProperty('initialWeight')
  })
  it('rejects adult/date/weight/precision errors and maps server fields to steps',()=>{
    const form={...initialiseProfileForm(profile(),'2026-10-16'),birthYear:'2009',weightInput:'70.001',weightDate:'2090-01-01'};const errors=validateProfileForm(form,'2026-10-16',true);expect(errors.birthYear).toBeTruthy();expect(errors.adultConfirmed).toBeTruthy();expect(errors.weightInput).toBeTruthy();expect(errors.weightDate).toBeTruthy();expect(stepForField('initialWeight.weightKg')).toBe(0);expect(stepForField('dailyTargetKj')).toBe(1);expect(stepForField('healthSafetyAnswer')).toBe(2)
  })
})
