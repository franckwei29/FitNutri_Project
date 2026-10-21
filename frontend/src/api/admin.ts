import type {AdminUsers,CurrentUser} from './contracts'
import {AdminUsersSchema,CurrentUserSchema,ChangeStatusRequestSchema} from './schemas'
import {apiGet,apiPatch} from './client'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {ApiRequestError} from './errors'
export async function fetchAccounts(query:string,page=0,size=20,signal?:AbortSignal):Promise<AdminUsers>{
  if(query.length>200||!Number.isSafeInteger(page)||page<0||!Number.isSafeInteger(size)||size<1||size>100||page*size>2147483647)throw new Error('Choose a supported account search and page.')
  const value=decodeResponse(AdminUsersSchema.extend({results:CurrentUserSchema.strict().array()}).strict(),await apiGet('/admin/users',{params:{query,page,size},signal}))
  if(value.page!==page||value.size!==size||value.results.length>size||new Set(value.results.map(r=>r.id)).size!==value.results.length||value.hasMore!==(page*size+value.results.length<value.totalMatches)||value.totalMatches<value.results.length)throw new UnexpectedApiResponse()
  return value
}
export async function changeAccountStatus(account:CurrentUser,enabled:boolean):Promise<CurrentUser>{
  try{decodeResponse(CurrentUserSchema.strict(),account);decodeResponse(ChangeStatusRequestSchema,{expectedVersion:account.version})}catch{throw new ApiRequestError({message:'Reload the account before changing its status.',code:'VALIDATION_ERROR',outcome:'rejected',writeAttempted:false})}
  const raw=await apiPatch(`/admin/users/${encodeURIComponent(account.id)}/${enabled?'reactivate':'deactivate'}`,{expectedVersion:account.version})
  try{const value=decodeResponse(CurrentUserSchema.strict(),raw);if(value.id!==account.id||value.email!==account.email||value.displayName!==account.displayName||value.role!==account.role||value.enabled!==enabled||value.version!==account.version+(account.enabled===enabled?0:1))throw new UnexpectedApiResponse();return value}
  catch{throw new ApiRequestError({message:'The account change response could not be verified. Read the current account state.',code:'UNEXPECTED_RESPONSE',outcome:'unknown',writeAttempted:true})}
}
/** Exact identity lookup over bounded server pages; absence is not proof of rollback. */
export async function readAccountState(account:CurrentUser,signal?:AbortSignal,size=100):Promise<CurrentUser|null>{
  for(let page=0;page<10;page++){const response=await fetchAccounts(account.email,page,size,signal);const exact=response.results.find(r=>r.id===account.id&&r.email===account.email);if(exact)return exact;if(!response.hasMore)return null}
  return null
}
