import type { EnergyUnit,OnboardingRequest,ProfileResponse,ProfileSettingsRequest } from '../../api/contracts'
import { DEFAULT_ZONE,isValidLocalDate } from '../../domain/dates'
export interface ProfileFormState {
  displayName:string;birthYear:string;adultConfirmed:boolean;sex:ProfileSettingsRequest['sex']|'';heightCm:string
  activityLevel:ProfileSettingsRequest['activityLevel']|'';goalType:ProfileSettingsRequest['goalType'];energyUnit:EnergyUnit
  targetInput:string;dailyTargetKj:number|null|undefined;healthSafetyAnswer:ProfileSettingsRequest['healthSafetyAnswer']|'';qaConsentGranted:boolean
  weightInput:string;weightDate:string
}
function displayTarget(kj:number|null,unit:EnergyUnit) {return kj==null?'':String(Number((kj/(unit==='KCAL'?4.184:1)).toFixed(3)))}
export function initialiseProfileForm(p:ProfileResponse,today:string):ProfileFormState {
  return {displayName:p.user.displayName,birthYear:p.birthYear==null?'':String(p.birthYear),adultConfirmed:p.adultConfirmed,sex:p.sex??'',heightCm:p.heightCm==null?'':String(p.heightCm),activityLevel:p.activityLevel??'',goalType:p.goalType,energyUnit:p.energyUnit,targetInput:displayTarget(p.dailyTargetKj,p.energyUnit),dailyTargetKj:p.dailyTargetKj,healthSafetyAnswer:p.healthSafetyAnswer??'',qaConsentGranted:p.qaConsentGranted,weightInput:'',weightDate:today}
}
/** Decimal input conversion only. The server remains authoritative; targets use one HALF_UP rounding to kJ3. */
export function editEnergyTarget(form:ProfileFormState,input:string):ProfileFormState {
  if(input==='') return {...form,targetInput:input,dailyTargetKj:null}
  if(!/^\d+(?:\.\d{1,3})?$/.test(input)||input.length>16) return {...form,targetInput:input,dailyTargetKj:undefined}
  const [whole,fraction='']=input.split('.'),milli=BigInt(whole)*1000n+BigInt(fraction.padEnd(3,'0'))
  const kjMilli=form.energyUnit==='KCAL'?(milli*4184n+500n)/1000n:milli
  const value=kjMilli>0n&&kjMilli<=999999999999n?Number(kjMilli)/1000:undefined
  return {...form,targetInput:input,dailyTargetKj:value}
}
export function changeEnergyUnit(form:ProfileFormState,unit:EnergyUnit):ProfileFormState {
  if(form.dailyTargetKj===undefined) return form
  return {...form,energyUnit:unit,targetInput:displayTarget(form.dailyTargetKj,unit)}
}
export function stepForField(field:string):number {
  if(['activityLevel','goalType','dailyTargetKj','energyUnit'].includes(field)) return 1
  if(['healthSafetyAnswer','qaConsentGranted'].includes(field)) return 2
  return 0
}
export function formField(field:string):string {return field==='initialWeight.weightKg'?'weightInput':field==='initialWeight.recordDate'?'weightDate':field}
export function validateProfileForm(f:ProfileFormState,today:string,onboarding:boolean):Record<string,string> {
  const e:Record<string,string>={}
  if(!f.displayName.trim()||f.displayName.trim().length>100) e.displayName='Enter a display name of 1 to 100 characters.'
  if(!f.adultConfirmed) e.adultConfirmed='Confirm that you are 18 or older.'
  if(!/^\d{4}$/.test(f.birthYear)||Number(f.birthYear)<1900||Number(f.birthYear)>Number(today.slice(0,4))-18) e.birthYear='Enter an adult birth year from 1900.'
  if(!f.sex) e.sex='Choose an option.'
  if(!/^\d+(?:\.\d+)?$/.test(f.heightCm)||Number(f.heightCm)<50||Number(f.heightCm)>260) e.heightCm='Enter a height from 50 to 260 cm.'
  if(!f.activityLevel) e.activityLevel='Choose an activity level.'
  if(f.dailyTargetKj===undefined) e.dailyTargetKj='Enter a positive energy reference with up to 3 decimal places, or leave it empty.'
  if(!f.healthSafetyAnswer) e.healthSafetyAnswer='Choose a health-safety response.'
  if(onboarding) {
    if(!/^\d+(?:\.\d{1,2})?$/.test(f.weightInput)||Number(f.weightInput)<20||Number(f.weightInput)>400) e.weightInput='Enter 20 to 400 kg with up to 2 decimal places.'
    if(!isValidLocalDate(f.weightDate)||f.weightDate>today) e.weightDate='Choose a valid measurement date that is not in the future.'
  }
  return e
}
export function settingsPayload(f:ProfileFormState,baseline:ProfileResponse):ProfileSettingsRequest {
  if(!f.sex||!f.activityLevel||!f.healthSafetyAnswer||f.dailyTargetKj===undefined) throw Error('Complete the required settings.')
  return {expectedUserVersion:baseline.user.version,expectedProfileVersion:baseline.profileVersion,displayName:f.displayName.trim(),birthYear:Number(f.birthYear),adultConfirmed:true,sex:f.sex,heightCm:Number(f.heightCm),activityLevel:f.activityLevel,goalType:f.goalType,dailyTargetKj:f.dailyTargetKj,energyUnit:f.energyUnit,healthSafetyAnswer:f.healthSafetyAnswer,qaConsentGranted:f.qaConsentGranted,zone:DEFAULT_ZONE}
}
export function onboardingPayload(f:ProfileFormState,baseline:ProfileResponse):OnboardingRequest {return {...settingsPayload(f,baseline),initialWeight:{recordDate:f.weightDate,weightKg:Number(f.weightInput)}}}
