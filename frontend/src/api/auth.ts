import type { CurrentUser, LoginRequest, RegisterRequest } from './contracts'
import { apiGet, apiPost } from './client'
import { clearCsrfToken } from './csrf'
import { CurrentUserSchema, LoginRequestSchema, RegisterRequestSchema } from './schemas'
import { decodeResponse } from './validators'
export type UserResponse = CurrentUser
export type LoginPayload = LoginRequest
export type RegisterPayload = RegisterRequest
export async function login(payload:LoginPayload,signal?:AbortSignal):Promise<CurrentUser> {
  const result=await apiPost('/auth/login',decodeResponse(LoginRequestSchema,{...payload,email:payload.email.trim().toLowerCase()}),{signal})
  clearCsrfToken();return decodeResponse(CurrentUserSchema,result)
}
export async function register(payload:RegisterPayload):Promise<CurrentUser> {return decodeResponse(CurrentUserSchema,await apiPost('/auth/register',decodeResponse(RegisterRequestSchema,{...payload,email:payload.email.trim().toLowerCase(),displayName:payload.displayName.trim()})))}
export async function fetchCurrentUser(signal?:AbortSignal):Promise<CurrentUser> {return decodeResponse(CurrentUserSchema,await apiGet('/auth/me',{signal}))}
export async function logout():Promise<void> {await apiPost('/auth/logout');clearCsrfToken()}
