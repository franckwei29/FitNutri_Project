import { describe,expect,it,vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { SessionController,type SessionDependencies } from '../../src/app/sessionEpoch'
import { homeFor,safeReturnTo } from '../../src/app/sessionRouting'
import { ApiRequestError } from '../../src/api/errors'
import { user,profile } from '../fixtures/profile'
function arrange(overrides:Partial<SessionDependencies>={}) {
  const queries=new QueryClient({defaultOptions:{queries:{retry:false,gcTime:Infinity}}})
  const api:SessionDependencies={fetchMe:vi.fn().mockResolvedValue(user),fetchProfile:vi.fn().mockResolvedValue(profile()),login:vi.fn().mockResolvedValue(user),logout:vi.fn().mockResolvedValue(undefined),csrf:vi.fn().mockResolvedValue(undefined),...overrides}
  return {queries,api,session:new SessionController(queries,api)}
}
function deferred<T>() {let resolve!:(v:T)=>void;const promise=new Promise<T>(r=>{resolve=r});return {promise,resolve}}
describe('F03 server session ownership and routing',()=>{
  it('restores me and owner-scoped profile without local storage',async()=>{
    const {session,queries}=arrange();await session.restore();const state=session.getSnapshot();expect(state.status).toBe('authenticated');
    if(state.status==='authenticated') {expect(state.user.id).toBe(user.id);expect(state.profile?.profileVersion).toBeNull();expect(homeFor(state)).toBe('/onboarding')}
    expect(queries.getQueryData(['owner',user.id,'profile'])).toEqual(profile())
  })
  it('profile500 is an error and cannot be interpreted as missing onboarding',async()=>{
    const {session}=arrange({fetchProfile:vi.fn().mockRejectedValue(new Error('Unavailable'))});await session.restore();expect(session.getSnapshot().status).toBe('error')
  })
  it('401 becomes anonymous while unrelated403 remains an explicit error',async()=>{
    const denied=(status:number)=>new ApiRequestError({message:'Denied',code:status===401?'UNAUTHENTICATED':'FORBIDDEN',status,outcome:'read-failed',writeAttempted:false})
    const anonymous=arrange({fetchMe:vi.fn().mockRejectedValue(denied(401))});await anonymous.session.restore();expect(anonymous.session.getSnapshot().status).toBe('anonymous')
    const forbidden=arrange({fetchMe:vi.fn().mockRejectedValue(denied(403))});await forbidden.session.restore();expect(forbidden.session.getSnapshot().status).toBe('error')
  })
  it('ADMIN restoration does not fetch private profile and routes to admin',async()=>{
    const admin={...user,role:'ADMIN' as const};const {session,api}=arrange({fetchMe:vi.fn().mockResolvedValue(admin)});await session.restore();expect(api.fetchProfile).not.toHaveBeenCalled();const state=session.getSnapshot();if(state.status!=='authenticated') throw Error('Expected session');expect(state.profile).toBeNull();expect(homeFor(state)).toBe('/admin')
  })
  it('late profile from previous epoch cannot restore expired user or refill cache',async()=>{
    const late=deferred<ReturnType<typeof profile>>();const {session,queries,api}=arrange({fetchProfile:vi.fn().mockReturnValue(late.promise)});const restoring=session.restore();await vi.waitFor(()=>expect(api.fetchProfile).toHaveBeenCalled());session.expire();late.resolve(profile());await restoring;
    expect(session.getSnapshot().status).toBe('anonymous');expect(queries.getQueryCache().getAll()).toHaveLength(0)
  })
  it('late old account response cannot replace a newly signed-in account',async()=>{
    const late=deferred<ReturnType<typeof profile>>();const other={...user,id:'42',email:'other@example.test'};
    const {session,api}=arrange({fetchMe:vi.fn().mockResolvedValueOnce(user).mockResolvedValue(other),fetchProfile:vi.fn().mockReturnValueOnce(late.promise).mockResolvedValue(profile({user:other}))});
    const old=session.restore();await vi.waitFor(()=>expect(api.fetchProfile).toHaveBeenCalledTimes(1));await session.signIn({email:other.email,password:'Synthetic123'});late.resolve(profile());await old;
    const state=session.getSnapshot();if(state.status!=='authenticated') throw Error('Expected session');expect(state.user.id).toBe('42')
  })
  it('failed logout hides private state and blocks switching accounts until server confirmation',async()=>{
    const {session,queries,api}=arrange();await session.restore();vi.mocked(api.logout).mockRejectedValueOnce(new Error('Network'));
    await expect(session.signOut()).rejects.toThrow('Network');expect(session.getSnapshot().status).toBe('logout-unconfirmed');expect(queries.getQueryCache().getAll()).toHaveLength(0);
    await expect(session.signIn({email:'other@example.test',password:'Synthetic123'})).rejects.toMatchObject({code:'LOGOUT_UNCONFIRMED',writeAttempted:false});expect(api.login).not.toHaveBeenCalled();
    await session.signOut();expect(session.getSnapshot().status).toBe('anonymous');expect(queries.getQueryCache().getAll()).toHaveLength(0)
  })
  it('mismatched profile identity is rejected before publishing or caching',async()=>{
    const {session,queries}=arrange({fetchProfile:vi.fn().mockResolvedValue(profile({user:{...user,id:'42'}}))});await session.restore();expect(session.getSnapshot().status).toBe('error');expect(queries.getQueryData(['owner',user.id,'profile'])).toBeUndefined()
  })
  it('old adult confirmation goes to profile and unsafe returnTo never redirects externally',()=>{
    expect(homeFor({status:'authenticated',epoch:1,user,profile:profile({onboardingCompleted:true,adultConfirmed:false})})).toBe('/profile?reconfirm=adult');
    for(const unsafe of ['https://example.test','//example.test','/\\example.test','/login','/admin','javascript:alert(1)','/dashboard\n']) expect(safeReturnTo(unsafe)).toBeNull();
    expect(safeReturnTo('/history?from=2020-01-01')).toBe('/history?from=2020-01-01')
  })
  it('unknown profile write stays session-scoped until a server snapshot is explicitly accepted',async()=>{
    const {session}=arrange();await session.restore();const epoch=session.getSnapshot().epoch;session.markProfileWriteUnknown(epoch);const unknown=session.getSnapshot();expect(unknown.status==='authenticated'&&unknown.profileRecoveryPending).toBe(true);
    session.markProfileWriteUnknown(epoch-1);expect(session.acceptProfile(profile(),epoch)).toBe(true);const checked=session.getSnapshot();expect(checked.status==='authenticated'&&checked.profileRecoveryPending).toBe(false);session.expire();session.markProfileWriteUnknown(epoch);expect(session.getSnapshot().status).toBe('anonymous')
  })

  it('weight recovery is isolated by session and is not cleared by unrelated profile refresh',async()=>{
    const {session}=arrange();await session.restore();const epoch=session.getSnapshot().epoch,request={recordDate:'2020-01-01',weightKg:70,expectedVersion:null,zone:'Australia/Sydney'};session.markWeightWriteUnknown(request,epoch);session.acceptProfile(profile(),epoch);let state=session.getSnapshot();expect(state.status==='authenticated'&&state.weightRecovery).toEqual(request);session.clearWeightRecovery(epoch);state=session.getSnapshot();expect(state.status==='authenticated'&&state.weightRecovery).toBeUndefined();session.expire();session.markWeightWriteUnknown(request,epoch);expect(session.getSnapshot().status).toBe('anonymous')
  })

  it('unconfirmed logout checks only server identity and never reloads private profile',async()=>{
    const {session,api}=arrange();await session.restore();vi.mocked(api.logout).mockRejectedValue(new Error('Network'));await expect(session.signOut()).rejects.toThrow();vi.mocked(api.fetchProfile).mockClear();await session.checkLogoutSession();expect(session.getSnapshot().status).toBe('logout-unconfirmed');expect(api.fetchProfile).not.toHaveBeenCalled();vi.mocked(api.fetchMe).mockRejectedValueOnce(new ApiRequestError({message:'Signed out',status:401,code:'UNAUTHENTICATED',outcome:'read-failed',writeAttempted:false}));await session.checkLogoutSession();expect(session.getSnapshot().status).toBe('anonymous')
  })

})
