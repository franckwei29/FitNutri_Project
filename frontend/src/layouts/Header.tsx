import {hasBusinessDraft} from '../features/entry/draftBusinessInput'
import {discardDraftForNavigation} from '../features/entry/draftNavigation'
import { useState } from 'react'
import { Add,Logout,PersonOutlined } from '@mui/icons-material'
import { Alert,Button,IconButton,Menu,MenuItem } from '@mui/material'
import { Link,NavLink,useNavigate } from 'react-router-dom'
import { Brand,ConfirmDialog } from '../components/presentation'
import { messageOf } from '../components/formHelpers'
import { useSession } from '../hooks/useSession'
import { useNavigationSafety } from '../hooks/useNavigationSafety'
import { useDraft } from '../hooks/useDraft'
import {pendingDraftOperation} from '../features/entry/recordMutationState'
export function Header() {
  const {state,actions}=useSession(),draft=useDraft(),navigate=useNavigate(),navigation=useNavigationSafety()
  const [anchor,setAnchor]=useState<HTMLElement|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[confirm,setConfirm]=useState(false)
  if(state.status!=='authenticated') return null
  const admin=state.user.role==='ADMIN',pending=pendingDraftOperation(draft.state)
  async function logout() {
    if(!(navigation?.discardDraft?.()??discardDraftForNavigation(draft)))return
    setConfirm(false);setBusy(true);setError('')
    try {await actions.signOut();navigate('/login',{replace:true})}
    catch(e) {setError('Log out was not confirmed. '+messageOf(e))}
    finally {setBusy(false)}
  }
  return <><header className={`app-header ${admin?'admin-header':''}`}><Brand />
    {admin?<span className="admin-title">Administrator Console</span>:<nav aria-label="Main navigation"><NavLink to="/dashboard">Dashboard</NavLink><NavLink to="/history">History</NavLink><NavLink to="/profile">Profile</NavLink></nav>}
    {!admin&&state.profile?.onboardingCompleted&&state.profile.adultConfirmed&&<Button component={Link} to="/add" variant="contained" startIcon={<Add />}>Add a record</Button>}
    {admin&&<span className="admin-email">{state.user.email}</span>}
    <IconButton aria-label="Account menu" onClick={e=>setAnchor(e.currentTarget)}><span className="avatar">{state.user.displayName.split(' ').map(n=>n[0]).join('').slice(0,2)}</span></IconButton>
    <Menu anchorEl={anchor} open={!!anchor} onClose={()=>setAnchor(null)}>
      {!admin&&<MenuItem onClick={()=>{setAnchor(null);navigate('/profile')}}><PersonOutlined fontSize="small" sx={{mr:1}} />My profile</MenuItem>}
      <MenuItem disabled={busy||pending==='saving'||navigation?.form?.pending==='saving'} onClick={()=>{setAnchor(null);if(hasBusinessDraft(draft.state.draft)||pending||navigation?.form?.dirty||navigation?.form?.pending) setConfirm(true);else void logout()}}><Logout fontSize="small" sx={{mr:1}} />Log out</MenuItem>
    </Menu></header>
    {error&&<Alert severity="error">{error}</Alert>}
    <ConfirmDialog open={confirm} title="Leave this draft and log out?" onCancel={()=>setConfirm(false)} onConfirm={()=>void logout()} label="Log out" busy={busy||pending==='saving'||navigation?.form?.pending==='saving'}>
      {navigation?.form?.pending==='unknown'?'Your change may already be saved. Check its current saved state after signing in again.':draft.state.recordMutation.status==='unknown'||draft.state.submission.status==='unknown'?"The save may already have completed. Leaving won't cancel it. Check History before recording it again. Signing out clears this local state.":'Your unsubmitted draft will be cleared when log out is confirmed.'}
    </ConfirmDialog>
  </>
}
