import {useEffect,useRef} from 'react'
import {useDraft} from '../../hooks/useDraft'
import {useSession} from '../../hooks/useSession'
/** Wait for query reconciliation to remove the root AND for modal cleanup to finish. */
export function useDeletedRootFocus(visibleKeys:string[],enabled=true){
 const {state}=useDraft(),session=useSession(),handled=useRef<string|null>(null),mutation=state.recordMutation
 const target=mutation.apiVersion===2&&mutation.status==='saved'&&mutation.intent.operation==='DELETE'?mutation:null
 const missing=!!target&&!visibleKeys.includes(`${target.intent.rootKind}:${target.intent.recordId}`)
 useEffect(()=>{if(!enabled||!missing||!target||handled.current===target.mutationId||session.state.status!=='authenticated'||target.ownerEpoch!==session.state.epoch)return;handled.current=target.mutationId;const owner=session.state.user.id,epoch=session.state.epoch;let active=true;queueMicrotask(()=>{const current=session.actions.getSnapshot();if(active&&current.status==='authenticated'&&current.user.id===owner&&current.epoch===epoch)(document.querySelector<HTMLElement>('.history-day h2')??document.querySelector<HTMLElement>('.record-group > h3')??document.getElementById('main-content'))?.focus()});return ()=>{active=false}},[target,missing,enabled,session.state,session.actions])
}
