import {useReducer} from 'react'
import type {ReactNode} from 'react'
import {fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {createQueryClient} from '../../src/app/queryClient'
import {FoodEditor} from '../../src/features/entry/FoodEditor'
import {ExerciseEditor} from '../../src/features/entry/ExerciseEditor'
import {draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import {loadEditDraft} from '../../src/features/entry/correctionIntent'
import {previewFoodPortion} from '../../src/api/foodPreview'
import {loadExerciseTypes,previewDatedExercise} from '../../src/api/exerciseTypes'
import {savedFood,savedExercise} from '../fixtures/records'
import {exerciseEstimate} from '../fixtures/exercise'
vi.mock('../../src/api/foodPreview',()=>({previewFoodPortion:vi.fn()}))
vi.mock('../../src/api/foodReference',async importOriginal=>({...await importOriginal<typeof import('../../src/api/foodReference')>(),searchSmartFood:vi.fn()}))
vi.mock('../../src/api/exerciseTypes',()=>({loadExerciseTypes:vi.fn(),previewDatedExercise:vi.fn()}))
const scope={ownerId:'1',epoch:1,catalogueRevision:'r1'}
function Wrapper({children}:{children:ReactNode}){return <QueryClientProvider client={createQueryClient()}>{children}</QueryClientProvider>}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(loadExerciseTypes).mockResolvedValue([])})
function Food(){const initial=loadEditDraft('1','food',savedFood({dataQuality:'LEGACY_UNVERIFIED',nutrients:{energyKj:null,proteinGrams:null,fatGrams:0,carbsGrams:null}})),[state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='food')throw Error('Fixture');return <FoodEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" onReview={vi.fn()} />}
function Exercise(){const initial=loadEditDraft('1','exercise',savedExercise()),[state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='exercise')throw Error('Fixture');return <ExerciseEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" profileVersion={99} onReview={vi.fn()} />}
describe('F13 correction editors preserve saved snapshots',()=>{
 it('does not recalculate an unchanged legacy food and permits date-only review with original null nutrients',async()=>{
  render(<Wrapper><Food /></Wrapper>);await screen.findByText('Unchanged reference and grams retain the saved nutrition snapshot.');expect(previewFoodPortion).not.toHaveBeenCalled();expect(screen.getAllByText('Unavailable')).toHaveLength(3);expect((screen.getByRole('button',{name:'Review food changes'}) as HTMLButtonElement).disabled).toBe(true)
  fireEvent.change(screen.getByLabelText('Record date'),{target:{value:'2026-10-15'}});await waitFor(()=>expect((screen.getByRole('button',{name:'Review food changes'}) as HTMLButtonElement).disabled).toBe(false));expect(previewFoodPortion).not.toHaveBeenCalled()
 })
 it('requires a new owned preview after food grams change',async()=>{
  const original=savedFood();vi.mocked(previewFoodPortion).mockResolvedValue({reference:original.reference!,quantityGrams:200,nutrients:{energyKj:1318,proteinGrams:6.2,fatGrams:0.4,carbsGrams:69.4},calculationFingerprint:'a'.repeat(64)})
  render(<Wrapper><Food /></Wrapper>);fireEvent.change(screen.getByLabelText('Portion in grams 1'),{target:{value:'200'}});await waitFor(()=>expect(previewFoodPortion).toHaveBeenCalledWith('10',200,expect.any(AbortSignal),original.id));expect(screen.queryByRole('checkbox')).toBeNull();await waitFor(()=>expect((screen.getByRole('button',{name:'Review food changes'}) as HTMLButtonElement).disabled).toBe(false))
 })
 it('keeps original exercise energy despite a newer profile version until its physical inputs change',async()=>{
  vi.mocked(previewDatedExercise).mockResolvedValue(exerciseEstimate({exerciseType:savedExercise().exerciseType!,durationMinutes:12.5}));render(<Wrapper><Exercise /></Wrapper>);await screen.findByText('Unchanged activity inputs retain the saved estimate.');expect(previewDatedExercise).not.toHaveBeenCalled();expect(screen.getByText('183 kJ')).toBeTruthy();expect((screen.getByRole('button',{name:'Review exercise changes'}) as HTMLButtonElement).disabled).toBe(true)
  fireEvent.change(screen.getByLabelText('Duration (minutes)'),{target:{value:'12.5'}});await waitFor(()=>expect(previewDatedExercise).toHaveBeenCalledWith(savedExercise().exerciseType!.id,12.5,'2026-10-16',expect.any(AbortSignal),savedExercise().id));await waitFor(()=>expect((screen.getByRole('button',{name:'Review exercise changes'}) as HTMLButtonElement).disabled).toBe(false))
 })
})
