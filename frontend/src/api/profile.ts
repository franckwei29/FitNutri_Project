import type { OnboardingRequest,ProfileResponse,ProfileSettingsRequest } from './contracts'
import type { z } from 'zod'
import { apiGet,apiPost,apiPut } from './client'
import { OnboardingRequestSchema,ProfileResponseSchema,ProfileSettingsRequestSchema } from './schemas'
import { decodeResponse } from './validators'
import { ApiRequestError } from './errors'
export async function fetchProfile(signal?:AbortSignal):Promise<ProfileResponse> {return decodeResponse(ProfileResponseSchema,await apiGet('/profile',{signal}))}
async function writeProfile<T>(schema:z.ZodType<T>,payload:T,path:string,method:'POST'|'PUT'):Promise<ProfileResponse> {
  let request:T
  try {request=decodeResponse(schema,payload)} catch {throw new ApiRequestError({message:'Check the settings before saving.',code:'VALIDATION_ERROR',outcome:'rejected',writeAttempted:false})}
  const raw=method==='POST'?await apiPost(path,request):await apiPut(path,request)
  try {return decodeResponse(ProfileResponseSchema,raw)}
  catch {throw new ApiRequestError({message:'The save response could not be verified. Check the saved settings.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
}
export function completeOnboarding(payload:OnboardingRequest):Promise<ProfileResponse> {return writeProfile(OnboardingRequestSchema,payload,'/profile/onboarding','POST')}
export function updateSettings(payload:ProfileSettingsRequest):Promise<ProfileResponse> {return writeProfile(ProfileSettingsRequestSchema,payload,'/profile','PUT')}
