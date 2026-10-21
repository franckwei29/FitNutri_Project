import { Alert,Button,CircularProgress } from '@mui/material'
import { Brand } from '../components/presentation'
import { useSession } from '../hooks/useSession'
export function SessionBoundary() {
  const {state,actions}=useSession()
  return <main id="main-content" tabIndex={-1} className="boot-screen"><Brand />{state.status==='logout-unconfirmed'?<><Alert severity="warning">{state.message}</Alert><Button onClick={()=>void actions.signOut().catch(()=>{})}>Retry log out</Button><Button onClick={()=>void actions.checkLogoutSession()}>Check server session</Button></>:state.status==='error'?<><Alert severity="error">{state.message}</Alert><Button onClick={()=>void actions.restore()}>Retry</Button></>:<><CircularProgress size={26} /><p role="status" aria-live="polite">Loading your FitNutri session…</p></>}</main>
}
