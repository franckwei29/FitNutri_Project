import type { QueryClient } from '@tanstack/react-query'
import type { CurrentUser,LoginRequest,ProfileResponse,WeightRequest } from '../api/contracts'
import { ApiRequestError } from '../api/errors'
import { queryKeys } from './queryKeys'

export type SessionState=
  | {status:'booting';epoch:number}
  | {status:'anonymous';epoch:number}
  | {status:'error';epoch:number;message:string}
  | {status:'logout-unconfirmed';epoch:number;message:string}
  | {status:'authenticated';epoch:number;user:CurrentUser;profile:ProfileResponse|null;profileRecoveryPending?:boolean;weightRecovery?:WeightRequest}
export interface SessionDependencies {
  fetchMe:(signal?:AbortSignal)=>Promise<CurrentUser>
  fetchProfile:(signal?:AbortSignal)=>Promise<ProfileResponse>
  login:(payload:LoginRequest,signal?:AbortSignal)=>Promise<CurrentUser>
  logout:()=>Promise<void>
  csrf:()=>Promise<unknown>
}
/** Epoch and abort checks protect React state AND cache writes across identity changes. */
export class SessionController {
  private epoch=0
  private controller=new AbortController()
  private state:SessionState={status:'booting',epoch:0}
  private readonly listeners=new Set<()=>void>()
  private readonly queries:QueryClient
  private readonly api:SessionDependencies
  constructor(queries:QueryClient,api:SessionDependencies) {this.queries=queries;this.api=api}
  getSnapshot=():SessionState=>this.state
  subscribe=(listener:()=>void)=>{this.listeners.add(listener);return ()=>{this.listeners.delete(listener)}}
  private publish(state:SessionState) {this.state=state;this.listeners.forEach(listener=>listener())}
  private clearPrivateResources() {
    this.controller.abort();this.controller=new AbortController();this.epoch++
    void this.queries.cancelQueries();this.queries.clear();return this.epoch
  }
  private begin(status:'booting'|'anonymous') {const epoch=this.clearPrivateResources();this.publish({status,epoch});return epoch}
  expire=()=>{this.begin('anonymous')}
  dispose=()=>{this.controller.abort();this.epoch++;void this.queries.cancelQueries();this.queries.clear()}
  private current(epoch:number) {return epoch===this.epoch&&!this.controller.signal.aborted}
  private async hydrate(epoch:number) {
    const signal=this.controller.signal
    const user=await this.api.fetchMe(signal)
    if(!this.current(epoch)) return
    if(user.role==='ADMIN') {this.publish({status:'authenticated',epoch,user,profile:null});return}
    const profile=await this.api.fetchProfile(signal)
    if(!this.current(epoch)) return
    if(profile.user.id!==user.id || profile.user.role!=='USER') throw new Error('The session identity changed. Please retry.')
    this.queries.setQueryData(queryKeys.profile(user.id),profile)
    this.publish({status:'authenticated',epoch,user:profile.user,profile})
  }
  restore=async()=>{
    const epoch=this.begin('booting')
    try {await this.hydrate(epoch)}
    catch(error) {if(this.current(epoch)) this.readFailed(error,epoch)}
  }
  signIn=async(payload:LoginRequest)=>{
    if(this.state.status==='logout-unconfirmed') throw new ApiRequestError({message:'Confirm the previous log out before signing in.',code:'LOGOUT_UNCONFIRMED',outcome:'rejected',writeAttempted:false})
    const epoch=this.begin('anonymous')
    try {
      await this.api.login(payload,this.controller.signal);if(!this.current(epoch)) return
      await this.api.csrf();if(!this.current(epoch)) return
      this.publish({status:'booting',epoch});await this.hydrate(epoch)
    } catch(error) {
      if(this.current(epoch)) {
        if(this.state.status==='booting') this.readFailed(error,epoch)
        else this.publish({status:'anonymous',epoch})
      }
      throw error
    }
  }
  signOut=async()=>{
    const epoch=this.epoch
    try {await this.api.logout();if(this.current(epoch)) this.expire()}
    catch(error) {
      if(this.current(epoch)&&this.state.status!=='anonymous') {
        const next=this.clearPrivateResources()
        this.publish({status:'logout-unconfirmed',epoch:next,message:'Log out was not confirmed. Private data has been hidden. Retry log out or check the server session.'})
      }
      throw error
    }
  }
  checkLogoutSession=async()=>{
    if(this.state.status!=='logout-unconfirmed') return
    const epoch=this.epoch
    try {
      await this.api.fetchMe(this.controller.signal)
      if(this.current(epoch)) this.publish({status:'logout-unconfirmed',epoch,message:'Your server session is still active. Retry log out before signing in to another account.'})
    } catch(error) {
      if(!this.current(epoch)) return
      if(error instanceof ApiRequestError&&error.status===401) this.expire()
      else this.publish({status:'logout-unconfirmed',epoch,message:'The server session could not be checked. Retry log out when the server is available.'})
    }
  }
  private readFailed(error:unknown,epoch:number) {
    if(error instanceof ApiRequestError && error.status===401) this.publish({status:'anonymous',epoch})
    else this.publish({status:'error',epoch,message:'Your session or profile could not be loaded. Retry to check the server.'})
  }
  markWeightWriteUnknown=(request:WeightRequest,epoch:number)=>{if(this.current(epoch)&&this.state.status==='authenticated') this.publish({...this.state,weightRecovery:Object.freeze({...request})})}
  clearWeightRecovery=(epoch:number)=>{if(this.current(epoch)&&this.state.status==='authenticated') this.publish({...this.state,weightRecovery:undefined})}
  markProfileWriteUnknown=(epoch:number)=>{if(this.current(epoch)&&this.state.status==='authenticated') this.publish({...this.state,profileRecoveryPending:true})}
  acceptProfile=(profile:ProfileResponse,epoch:number)=>{
    if(!this.current(epoch)||this.state.status!=='authenticated'||profile.user.id!==this.state.user.id||profile.user.role!=='USER') return false
    const current=this.state.profile
    if(current?.profileVersion!=null && (profile.profileVersion==null||profile.profileVersion<current.profileVersion)) return false
    if(profile.user.version<this.state.user.version) return false
    this.queries.setQueryData(queryKeys.profile(profile.user.id),profile)
    this.publish({...this.state,user:profile.user,profile,profileRecoveryPending:false});return true
  }
  refreshProfile=async()=>{
    const state=this.state;if(state.status!=='authenticated'||state.user.role!=='USER') return
    const profile=await this.api.fetchProfile(this.controller.signal)
    return this.acceptProfile(profile,state.epoch)?profile:undefined
  }
}
