import {useQuery} from '@tanstack/react-query'
import {fetchDailySummary,fetchTrends} from '../api/summary'
import {queryKeys} from '../app/queryKeys'
import {isValidLocalDate,isValidRecordRange} from '../domain/dates'
import {useSession} from './useSession'
export function useDaySummary(date:string,enabled=true){const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',version=state.status==='authenticated'?state.profile?.profileVersion:null;return useQuery({queryKey:queryKeys.daySummary(owner,state.epoch,date,version??null),queryFn:({signal})=>fetchDailySummary(date,signal),enabled:!!owner&&enabled&&isValidLocalDate(date),retry:false})}
export function usePeriodSummary(from:string,to:string,enabled=true){const {state}=useSession(),owner=state.status==='authenticated'?state.user.id:'',version=state.status==='authenticated'?state.profile?.profileVersion:null;return useQuery({queryKey:queryKeys.periodSummary(owner,state.epoch,from,to,version??null),queryFn:({signal})=>fetchTrends(from,to,signal),enabled:!!owner&&enabled&&isValidRecordRange(from,to),retry:false})}
