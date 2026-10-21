import axios,{AxiosHeaders} from 'axios'
import type { ApiError,WeightEntry,ExerciseEstimate,FoodPreviewV2,ExercisePreviewV2 } from './contracts'
import { ApiErrorSchema,WeightConflictResponseSchema,CalculationChangedResponseSchema,FoodCalculationChangedV2Schema,ExerciseCalculationChangedV2Schema } from './schemas'

export type WriteOutcome = 'rejected' | 'unknown' | 'read-failed'
export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: ApiError['fieldErrors']
  readonly requestId: string | null
  readonly retryAfterSeconds?:number
  readonly retryable: boolean
  readonly canceled: boolean
  readonly outcome: WriteOutcome
  readonly currentItem?: WeightEntry | null
  readonly currentExercisePreviewV2?: ExercisePreviewV2
  readonly currentFoodPreview?: FoodPreviewV2
  readonly currentPreview?: ExerciseEstimate
  readonly writeAttempted: boolean
  constructor(options: { currentExercisePreviewV2?:ExercisePreviewV2; currentFoodPreview?:FoodPreviewV2; retryAfterSeconds?:number; currentPreview?: ExerciseEstimate; currentItem?: WeightEntry | null; message: string; code: string; status?: number; fieldErrors?: ApiError['fieldErrors']; requestId?: string | null; retryable?: boolean; canceled?: boolean; outcome: WriteOutcome; writeAttempted: boolean }) {
    super(options.message);this.name='ApiRequestError';this.status=options.status??0;this.code=options.code
    this.retryAfterSeconds=options.retryAfterSeconds;this.fieldErrors=options.fieldErrors??[];this.requestId=options.requestId??null;this.retryable=options.retryable??false
    this.canceled=options.canceled??false;this.outcome=options.outcome;this.writeAttempted=options.writeAttempted;this.currentItem=options.currentItem;this.currentPreview=options.currentPreview;this.currentFoodPreview=options.currentFoodPreview;this.currentExercisePreviewV2=options.currentExercisePreviewV2
  }
}
const rejectedCodes=new Set(['VALIDATION_ERROR','NOT_FOUND','CONTRACT_UPGRADE_REQUIRED','UNAUTHENTICATED','FORBIDDEN','CSRF_INVALID','VERSION_CONFLICT','IDEMPOTENCY_CONFLICT','CLIENT_ENTRY_CONFLICT','GROUP_EDIT_REQUIRED','REFERENCE_INACTIVE','CALCULATION_CHANGED','CONSTRAINT_CONFLICT','EMAIL_ALREADY_REGISTERED','ONBOARDING_ALREADY_COMPLETED','ONBOARDING_REQUIRED','CATALOGUE_UNAVAILABLE','UNSUPPORTED_REFERENCE','RATE_LIMITED','QA_RESTRICTED','ACCOUNT_PROTECTED','DATE_CONTEXT_CHANGED','DATE_REVIEW_REQUIRED','SPLIT_DATED_ENTRY','SPLIT_EXERCISE_ENTRY','PROVIDER_REFUSED','PAYLOAD_TOO_LARGE'])
export function normaliseApiError(error: unknown, method='GET', writeAttempted=false): ApiRequestError {
  const write=!['GET','HEAD','OPTIONS'].includes(method.toUpperCase())
  const outcome=(known=false):WriteOutcome=>!write?'read-failed':!writeAttempted||known?'rejected':'unknown'
  if(error instanceof ApiRequestError) return new ApiRequestError({currentExercisePreviewV2:error.currentExercisePreviewV2,currentFoodPreview:error.currentFoodPreview,retryAfterSeconds:error.retryAfterSeconds,currentPreview:error.currentPreview,currentItem:error.currentItem,message:error.message,code:error.code,status:error.status,fieldErrors:error.fieldErrors,requestId:error.requestId,retryable:error.retryable,canceled:error.canceled,outcome:outcome(error.outcome==='rejected'),writeAttempted:write&&writeAttempted})
  if(axios.isCancel(error)) return new ApiRequestError({message:'Request canceled.',code:'REQUEST_CANCELED',canceled:true,outcome:outcome(),writeAttempted:write&&writeAttempted})
  if(axios.isAxiosError(error)) {
    const parsed=ApiErrorSchema.safeParse(error.response?.data)
    if(parsed.success && parsed.data.status===error.response?.status) {
      const server=parsed.data
      const responseHeaders=error.response?.headers
      const retryHeader=String((responseHeaders instanceof AxiosHeaders?responseHeaders.get('Retry-After'):responseHeaders?.['retry-after']??responseHeaders?.['Retry-After'])??'')
      const retryAfterSeconds=server.status===429&&server.code==='RATE_LIMITED'&&/^[0-9]{1,3}$/.test(retryHeader)?Math.max(1,Math.min(Number(retryHeader),300)):undefined
      const conflict=server.status===409&&server.code==='VERSION_CONFLICT'&&server.path==='/api/profile/weight'?WeightConflictResponseSchema.safeParse(error.response?.data):null
      const changed=server.status===409&&server.code==='CALCULATION_CHANGED'&&/^\/api\/records\/exercise(?:\/[1-9][0-9]*)?$/.test(server.path)?CalculationChangedResponseSchema.safeParse(error.response?.data):null
      const foodChanged=server.status===409&&server.code==='CALCULATION_CHANGED'&&/^\/api\/v2\/food\/(?:batches|entries\/(?:single|dish)\/[1-9][0-9]*)$/.test(server.path)?FoodCalculationChangedV2Schema.safeParse(error.response?.data):null
      const exerciseChanged=server.status===409&&server.code==='CALCULATION_CHANGED'&&/^\/api\/v2\/exercise\/entries(?:\/(?:quick|workout)\/[1-9][0-9]*)?$/.test(server.path)?ExerciseCalculationChangedV2Schema.safeParse(error.response?.data):null
      return new ApiRequestError({currentExercisePreviewV2:exerciseChanged?.success?exerciseChanged.data.currentPreview:undefined,currentFoodPreview:foodChanged?.success?foodChanged.data.currentPreview:undefined,retryAfterSeconds,currentPreview:changed?.success?changed.data.currentPreview:undefined,currentItem:conflict?.success?conflict.data.currentItem:undefined,message:server.message,code:server.code,status:server.status,fieldErrors:server.fieldErrors,requestId:server.requestId,retryable:server.retryable,outcome:outcome(server.status<500&&rejectedCodes.has(server.code)),writeAttempted:write&&writeAttempted})
    }
    const timeout=error.code==='ECONNABORTED'||error.code==='ETIMEDOUT'
    return new ApiRequestError({message:timeout?'The request timed out. Check its result before retrying a save.':error.response?'Unexpected server response. Refresh and try again.':'The server could not be reached.',code:timeout?'REQUEST_TIMEOUT':error.response?'UNEXPECTED_RESPONSE':'NETWORK_ERROR',status:error.response?.status,retryable:true,outcome:outcome(),writeAttempted:write&&writeAttempted})
  }
  return new ApiRequestError({message:'Unexpected server response. Refresh and try again.',code:'UNEXPECTED_RESPONSE',outcome:outcome(),writeAttempted:write&&writeAttempted})
}
export function isKnownRejectedWrite(error: unknown): boolean {return error instanceof ApiRequestError && error.outcome==='rejected'}
export function isCancellation(error: unknown): boolean {return error instanceof ApiRequestError && error.canceled}
const expiredListeners=new Set<()=>void>()
export function subscribeAuthExpired(listener:()=>void):()=>void {expiredListeners.add(listener);return ()=>{expiredListeners.delete(listener)}}
export function signalAuthExpired():void {expiredListeners.forEach(listener=>listener())}
