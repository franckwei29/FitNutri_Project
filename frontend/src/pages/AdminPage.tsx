import {useCallback,useEffect,useRef,useState} from 'react'
import {Alert,Button,Chip,Table,TableBody,TableCell,TableContainer,TableHead,TableRow,TextField} from '@mui/material'
import {useQuery} from '@tanstack/react-query'
import type {CurrentUser} from '../api/contracts'
import {fetchAccounts,changeAccountStatus,readAccountState} from '../api/admin'
import {isKnownRejectedWrite} from '../api/errors'
import {ConfirmDialog,ErrorBox,Page,SelectField} from '../components/presentation'
import {messageOf} from '../components/formHelpers'
import {useSession} from '../hooks/useSession'
import {useNavigationSafety} from '../hooks/useNavigationSafety'

export function AdminPage(){
  const {state}=useSession()
  return state.status==='authenticated'&&state.user.role==='ADMIN'?<AccountConsole key={`${state.user.id}/${state.epoch}`} owner={state.user.id} epoch={state.epoch} />:null
}
type Selection={account:CurrentUser;enabled:boolean}
/** Demo extras.tsx table, toolbar and confirmation; all account data now comes from the server. */
function AccountConsole({owner,epoch}:{owner:string;epoch:number}){
  const {actions}=useSession(),navigation=useNavigationSafety()
  const [input,setInput]=useState(''),[query,setQuery]=useState(''),[page,setPage]=useState(0),[filter,setFilter]=useState('all')
  const [selected,setSelected]=useState<Selection|null>(null),[phase,setPhase]=useState<'confirm'|'saving'|'unknown'|null>(null),[checking,setChecking]=useState(false)
  const [error,setError]=useState(''),[notice,setNotice]=useState('')
  const lock=useRef(false),mounted=useRef(true),read=useRef<AbortController|null>(null)
  const accounts=useQuery({queryKey:['owner',owner,epoch,'admin-accounts',query,page],queryFn:({signal})=>fetchAccounts(query,page,20,signal)})
  const busy=phase==='saving'||checking,pending=phase==='saving'?'saving':phase==='unknown'?'unknown':null
  const discard=useCallback(()=>{setSelected(null);setPhase(null)},[]),updateNavigation=navigation?.updateForm
  useEffect(()=>{mounted.current=true;return()=>{mounted.current=false;read.current?.abort()}},[])
  useEffect(()=>{updateNavigation?.('admin-account',{dirty:!!selected,pending,onDiscard:discard});return()=>updateNavigation?.('admin-account',null)},[selected,pending,discard,updateNavigation])
  function current(){const value=actions.getSnapshot();return mounted.current&&value.status==='authenticated'&&value.user.id===owner&&value.user.role==='ADMIN'&&value.epoch===epoch}
  async function refresh(){try{await accounts.refetch({throwOnError:true})}catch{if(current())setError('The account directory could not refresh. Any confirmed change is retained.')}}
  async function change(){
    if(!selected||lock.current||!current())return
    const frozen=structuredClone(selected);lock.current=true;setPhase('saving');setError('');setNotice('')
    try{
      await changeAccountStatus(frozen.account,frozen.enabled)
      if(!current())return
      setSelected(null);setPhase(null)
      setNotice(`${frozen.account.displayName} was ${frozen.enabled?'reactivated':'deactivated'}. Saved personal records are retained.`)
      await refresh()
    }catch(e){
      if(!current())return
      if(isKnownRejectedWrite(e)){setSelected(null);setPhase(null);setError(messageOf(e));await refresh()}
      else{setSelected(frozen);setPhase('unknown');setError('The server may have applied this change. Read its current state before deciding what to do next.')}
    }finally{if(current())lock.current=false}
  }
  async function check(){
    if(!selected||phase!=='unknown'||lock.current||!current())return
    lock.current=true;setChecking(true);setError('');const frozen=selected,controller=new AbortController();read.current=controller
    try{
      const observed=await readAccountState(frozen.account,controller.signal)
      if(!current())return
      if(!observed){setError('This identity was not found in the bounded account search. The original outcome remains unconfirmed.');return}
      if(observed.enabled===frozen.enabled){setSelected(null);setPhase(null);setNotice(`The current server state is ${observed.enabled?'active':'deactivated'}. This does not prove which request changed it.`);await refresh()}
      else if(observed.version>frozen.account.version){setSelected(null);setPhase(null);setError('The account changed to a different state. Review the refreshed version before making another change.');await refresh()}
      else setError('The desired state is not visible. The original outcome remains unconfirmed; an explicit retry keeps the original version.')
    }catch{if(current())setError('The current account state could not be read. The original outcome remains unconfirmed.')}
    finally{if(current()){lock.current=false;setChecking(false)}}
  }
  const rows=accounts.data?.results??[],visible=rows.filter(r=>filter==='all'||r.enabled===(filter==='active'))
  return <Page title="User management." eyebrow="ADMINISTRATOR CONSOLE" subtitle="Manage account access. Personal food, exercise and health details stay private.">
    {notice&&<Alert severity="success" role="status">{notice}</Alert>}<ErrorBox message={error} />
    {phase==='unknown'&&selected&&<section className="card" aria-label="Unconfirmed account change"><h2>Account change outcome is unconfirmed.</h2><p>{selected.account.displayName} · {selected.account.email} · original version {selected.account.version}. Requested state: {selected.enabled?'active':'deactivated'}.</p><Button disabled={busy} onClick={()=>void check()}>Read current account state</Button><Button disabled={busy} onClick={()=>void change()}>Retry original version</Button><p>This recovery state stays in memory while this page is open. Leaving or logging out does not cancel the server operation; reread the account before another change.</p></section>}
    <div className="admin-stats"><div><strong>{accounts.data?.totalMatches??'—'}</strong><span>Accounts matching server search</span></div><div><strong>{accounts.data?rows.filter(r=>r.enabled).length:'—'}</strong><span>Active on this page</span></div><div><strong>{accounts.data?rows.filter(r=>!r.enabled).length:'—'}</strong><span>Deactivated on this page</span></div></div>
    <section className="card"><form className="card-heading admin-toolbar" onSubmit={e=>{e.preventDefault();if(!selected&&!busy){setQuery(input.trim());setPage(0)}}}><TextField label="Search name or email" value={input} onChange={e=>setInput(e.target.value)} disabled={!!selected||busy} slotProps={{htmlInput:{maxLength:200}}} /><Button type="submit" disabled={!!selected||busy}>Search accounts</Button><SelectField label="Status on this page" value={filter} onChange={setFilter} options={{all:'All on this page',active:'Active on this page',inactive:'Deactivated on this page'}} disabled={!!selected||busy} /><Button disabled={!!selected||busy||accounts.isFetching} onClick={()=>void refresh()}>Refresh accounts</Button></form>
      {accounts.isPending?<p role="status">Loading accounts…</p>:accounts.isError?<ErrorBox message="Accounts could not be loaded." />:accounts.data?.totalMatches===0?<p>No accounts match this server search.</p>:<><p>Page {page+1}. Status filtering applies only to this server page. Your account cannot deactivate itself; the server also protects the last active administrator.</p><TableContainer><Table aria-label="User accounts"><TableHead><TableRow>{['User','Email','Role','Status','Version','Actions'].map(label=><TableCell key={label}>{label}</TableCell>)}</TableRow></TableHead><TableBody>{visible.map(account=><TableRow key={account.id}><TableCell><div className="inline-row"><span className="avatar">{account.displayName.split(' ').map(n=>n[0]).join('').slice(0,2)}</span><div><strong>{account.displayName}</strong><span className="autocomplete-meta">U-{account.id}</span></div></div></TableCell><TableCell>{account.email}</TableCell><TableCell>{account.role==='ADMIN'?'Administrator':'User'}</TableCell><TableCell><Chip size="small" label={account.enabled?'Active':'Deactivated'} color={account.enabled?'success':'default'} variant="outlined" /></TableCell><TableCell>{account.version}</TableCell><TableCell><Button size="small" color={account.enabled?'error':'primary'} variant="outlined" aria-label={account.id===owner&&account.enabled?'Deactivate your own account':`${account.enabled?'Deactivate':'Reactivate'} ${account.displayName}`} disabled={!!selected||busy||(account.id===owner&&account.enabled)} onClick={()=>{setSelected({account:structuredClone(account),enabled:!account.enabled});setPhase('confirm');setError('');setNotice('')}}>{account.enabled?'Deactivate':'Reactivate'}</Button></TableCell></TableRow>)}</TableBody></Table></TableContainer>{!visible.length&&<p>No accounts on this page match the status filter.</p>}</>}
      <div className="inline-row"><Button disabled={!!selected||busy||page===0||accounts.isFetching} onClick={()=>setPage(p=>p-1)}>Previous accounts page</Button><Button disabled={!!selected||busy||!accounts.data?.hasMore||accounts.isFetching||accounts.isError} onClick={()=>setPage(p=>p+1)}>Next accounts page</Button></div>
    </section>
    <ConfirmDialog open={!!selected&&phase!=='unknown'} title={selected?.enabled?'Reactivate this account?':'Deactivate this account?'} label={selected?.enabled?'Confirm reactivation':'Confirm deactivation'} danger={!selected?.enabled} busy={busy} onCancel={discard} onConfirm={()=>void change()}>{selected&&<p>{selected.account.displayName} · {selected.account.email} · {selected.account.role} · currently {selected.account.enabled?'active':'deactivated'} · version {selected.account.version}. Personal records are retained. {selected.enabled?'Access will be restored.':'New requests from this account will be denied.'}</p>}</ConfirmDialog>
  </Page>
}
