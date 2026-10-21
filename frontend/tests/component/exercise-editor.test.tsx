import {legacyExerciseDraft} from '../fixtures/exercise'
import {useReducer} from 'react'
import type {ReactNode} from 'react'
import {act,fireEvent,render,renderHook,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import type {ExerciseEstimate} from '../../src/api/contracts'
import {draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import {ExerciseEditor} from '../../src/features/entry/ExerciseEditor'
import {useExerciseEstimate} from '../../src/hooks/useExerciseEstimate'
import {loadExerciseTypes,previewDatedExercise} from '../../src/api/exerciseTypes'
import {createQueryClient} from '../../src/app/queryClient'
import {exerciseEstimate,exerciseType} from '../fixtures/exercise'
vi.mock('../../src/api/exerciseTypes',()=>({loadExerciseTypes:vi.fn(),previewDatedExercise:vi.fn()}))
const scope={ownerId:'1',epoch:1,catalogueRevision:'r1'}
function wrapper(){const client=createQueryClient();return ({children}:{children:ReactNode})=><QueryClientProvider client={client}>{children}</QueryClientProvider>}
function Harness({onReview}:{onReview:(estimate:ExerciseEstimate)=>void}){const initial=legacyExerciseDraft('1','10.5');const [state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='exercise')throw Error('Fixture');return <ExerciseEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" profileVersion={2} onReview={onReview} />}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(loadExerciseTypes).mockResolvedValue([exerciseType])})
describe('F09 exercise editor',()=>{
 it('displays the actual historical weight/date and decimal duration without an intensity field',async()=>{
  vi.mocked(previewDatedExercise).mockResolvedValue(exerciseEstimate());const Wrapper=wrapper(),review=vi.fn();render(<Wrapper><Harness onReview={review} /></Wrapper>)
  await screen.findByText(/71.5 kg recorded on 2026-10-15/);expect((screen.getByLabelText('Duration (minutes)') as HTMLInputElement).value).toBe('10.5')
  expect(screen.queryByLabelText(/intensity/i)).toBeNull();expect(screen.getByText(/17160/)).toBeTruthy();fireEvent.click(screen.getByRole('button',{name:'Review exercise entry'}));expect(review).toHaveBeenCalledWith(exerciseEstimate())
 })
 it('permits review when no historical weight is available and keeps energy null',async()=>{
  const absent=exerciseEstimate({estimatedKj:null,estimateStatus:'MISSING_WEIGHT',unavailableReason:'No weight was recorded on or before this activity date.',weightKgUsed:null,weightDateUsed:null,metUsed:null});vi.mocked(previewDatedExercise).mockResolvedValue(absent)
  const Wrapper=wrapper(),review=vi.fn();render(<Wrapper><Harness onReview={review} /></Wrapper>);await screen.findByText(absent.unavailableReason!)
  expect(screen.getByText('Unavailable')).toBeTruthy();expect(screen.queryByText(/70 kg/)).toBeNull();fireEvent.click(screen.getByRole('button',{name:'Review exercise entry'}));expect(review).toHaveBeenCalledWith(absent)
 })
 it('hides stale dated estimates after input changes and rejects a late response',async()=>{
  let resolve!:(estimate:ExerciseEstimate)=>void;const old=new Promise<ExerciseEstimate>(r=>{resolve=r});vi.mocked(previewDatedExercise).mockImplementation((_,minutes,date)=>date==='2026-10-16'?old:Promise.resolve(exerciseEstimate({recordDate:date,durationMinutes:minutes,weightDateUsed:'2026-10-10'})))
  const Wrapper=wrapper();const hook=renderHook(({date})=>useExerciseEstimate(exerciseType.id,'10.5',date,scope,2),{initialProps:{date:'2026-10-16'},wrapper:Wrapper})
  await waitFor(()=>expect(previewDatedExercise).toHaveBeenCalledTimes(1));hook.rerender({date:'2026-10-14'});expect(hook.result.current.data).toBeUndefined()
  await waitFor(()=>expect(hook.result.current.data?.recordDate).toBe('2026-10-14'));await act(async()=>{resolve(exerciseEstimate());await old});expect(hook.result.current.data?.weightDateUsed).toBe('2026-10-10')
 })
 it('keeps failed input visible and disables Review until an authoritative preview succeeds',async()=>{
  vi.mocked(previewDatedExercise).mockRejectedValue(new Error('Estimate unavailable'));const Wrapper=wrapper();render(<Wrapper><Harness onReview={vi.fn()} /></Wrapper>)
  await screen.findByText('Estimate unavailable');expect((screen.getByRole('button',{name:'Review exercise entry'}) as HTMLButtonElement).disabled).toBe(true)
  fireEvent.change(screen.getByLabelText('Duration (minutes)'),{target:{value:'0'}});expect((screen.getByLabelText('Duration (minutes)') as HTMLInputElement).value).toBe('0');expect(previewDatedExercise).toHaveBeenCalledTimes(1)
 })
})
