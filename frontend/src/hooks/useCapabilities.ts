import {useQuery} from '@tanstack/react-query'
import {fetchCapabilities} from '../api/capabilities'
import {useSession} from './useSession'
export function useCapabilities(){const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'';return useQuery({queryKey:['owner',owner,state.epoch,'capabilities',state.status==='authenticated'?state.user.version:null,state.status==='authenticated'?state.profile?.profileVersion:null],queryFn:({signal})=>fetchCapabilities(signal),enabled:!!owner,retry:false})}
