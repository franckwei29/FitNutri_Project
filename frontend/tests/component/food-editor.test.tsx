import {fetchRecentFoodV2} from '../../src/api/foodEntriesV2'
import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import {useReducer} from 'react'
import type {ReactNode} from 'react'
import {act,fireEvent,render,renderHook,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import type {FoodDraft} from '../../src/features/entry/draftTypes'
import type {FoodPreview,ReferenceInfo} from '../../src/api/contracts'
import {createDraft,createFoodRow,draftReducer,emptyDraftState} from '../../src/features/entry/draftReducer'
import {mapImageCandidates} from '../../src/features/entry/media/imageDraft'
import {imageRecognition} from '../fixtures/media'
import {FoodEditor} from '../../src/features/entry/FoodEditor'
import {previewFoodPortion} from '../../src/api/foodPreview'
import {useFoodPreview} from '../../src/hooks/useFoodPreview'
import {createQueryClient} from '../../src/app/queryClient'
vi.mock('../../src/api/foodPreview',()=>({previewFoodPortion:vi.fn()}))
vi.mock('../../src/api/foodReference',async importOriginal=>({...await importOriginal<typeof import('../../src/api/foodReference')>(),searchSmartFood:vi.fn()}))
vi.mock('../../src/api/foodEntriesV2',()=>({fetchRecentFoodV2:vi.fn().mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false})}))
const scope={ownerId:'1',epoch:1,catalogueRevision:'r1'},dataset={id:'1',name:'AUSNUT',release:'2023',revision:'r1',sourceUrl:null,nutrientPolicy:'per100g',verified:true}
const reference:ReferenceInfo={referenceId:'10',publicFoodKey:'F007661',displayName:'Rice',active:true,dataset}
function preview(grams:number):FoodPreview{return {reference,quantityGrams:grams,nutrients:{energyKj:grams===150?988.5:1318,proteinGrams:4.65,fatGrams:0.3,carbsGrams:52.05},calculationFingerprint:String(grams).padStart(64,'a')}}
function draft():FoodDraft{const d=createDraft('1','food','2026-10-16','MANUAL') as FoodDraft;d.entries=[1,2,3].map((_,i)=>({...createFoodRow(),referenceId:'10',selectedReference:reference,quantityGramsInput:'150',mealType:i===0?'LUNCH':'DINNER'}));return d}
function wrapper(){const client=createQueryClient();return ({children}:{children:ReactNode})=><QueryClientProvider client={client}>{children}</QueryClientProvider>}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(fetchRecentFoodV2).mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false})})
describe('F08 multi-food editor',()=>{
 it('shows authoritative portions and independent meals; removing the middle row preserves other identities and input',async()=>{
  vi.mocked(previewFoodPortion).mockResolvedValue(preview(150));const initial=draft(),review=vi.fn(),Wrapper=wrapper()
  function Harness(){const [state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='food')throw Error('Fixture');return <><output data-testid="ids">{foodRows(state.draft).map(r=>r.clientEntryId).join(',')}</output><FoodEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" onReview={review} /></>}
  render(<Wrapper><Harness /></Wrapper>);await waitFor(()=>expect((screen.getByRole('button',{name:'Review food entries'}) as HTMLButtonElement).disabled).toBe(false))
  expect(screen.getAllByText('989 kJ')).toHaveLength(3);expect(screen.getAllByRole('combobox',{name:/Meal occasion/})).toHaveLength(3)
  fireEvent.click(screen.getByRole('button',{name:'Remove food 2'}));expect(screen.getByTestId('ids').textContent).toBe([foodRows(initial)[0].clientEntryId,foodRows(initial)[2].clientEntryId].join(','))
  expect((screen.getByLabelText('Portion in grams 2') as HTMLInputElement).value).toBe('150')
  fireEvent.click(screen.getByRole('button',{name:'Review food entries'}));expect(review).toHaveBeenCalledOnce()
 })
 it('invalid or failed previews retain the row and forbid grams confirmation and review',async()=>{
  vi.mocked(previewFoodPortion).mockRejectedValue(new Error('Preview unavailable'));const initial=draft();initial.entries=foodRows(initial).slice(0,1);const Wrapper=wrapper()
  function Harness(){const [state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='food')throw Error('Fixture');return <FoodEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" onReview={vi.fn()} />}
  render(<Wrapper><Harness /></Wrapper>);await screen.findByText('Preview unavailable');expect((screen.getByRole('button',{name:'Review food entries'}) as HTMLButtonElement).disabled).toBe(true)
  fireEvent.change(screen.getByLabelText('Portion in grams 1'),{target:{value:'-2'}});expect(screen.queryByRole('checkbox')).toBeNull()
  fireEvent.change(screen.getByLabelText('Portion in grams 1'),{target:{value:'0'}});expect((screen.getByLabelText('Portion in grams 1') as HTMLInputElement).value).toBe('0')
  expect(previewFoodPortion).toHaveBeenCalledTimes(1)
 })
 it('hides late preview values after grams change and preserves exact decimal input',async()=>{
  let resolve!:(value:FoodPreview)=>void;const old=new Promise<FoodPreview>(r=>{resolve=r});vi.mocked(previewFoodPortion).mockImplementation((_,grams)=>grams===150?old:Promise.resolve({...preview(200),quantityGrams:200.125}))
  const Wrapper=wrapper();const hook=renderHook(({grams})=>useFoodPreview(reference.referenceId,grams,scope),{initialProps:{grams:'150'},wrapper:Wrapper})
  await waitFor(()=>expect(previewFoodPortion).toHaveBeenCalledTimes(1));hook.rerender({grams:'200.125'});expect(hook.result.current.data).toBeUndefined()
  await waitFor(()=>expect(hook.result.current.data?.quantityGrams).toBe(200.125));await act(async()=>{resolve(preview(150));await old});expect(hook.result.current.data?.quantityGrams).toBe(200.125)
 })
 it('clearing a chosen reference revokes both confirmations without changing row or entry IDs',()=>{
  const initial=draft(),row=foodRows(initial)[0],state={...emptyDraftState('1'),draft:initial}
  const cleared=draftReducer(state,{type:'CLEAR_REFERENCE',ownerId:'1',rowKey:row.rowKey});if(cleared.draft?.kind!=='food')throw Error('Fixture')
  expect(foodRows(cleared.draft)[0]).toMatchObject({rowKey:row.rowKey,clientEntryId:row.clientEntryId,referenceId:null,selectedReference:null})
 })
 it('lets image suggestions choose a real reference while keeping meal and both confirmations pending',async()=>{
  vi.mocked(previewFoodPortion).mockResolvedValue(preview(150));const initial=draft();initial.source='IMAGE';initial.entries=mapImageCandidates(imageRecognition().candidates).slice(0,1);const Wrapper=wrapper()
  function Harness(){const [state,dispatch]=useReducer(draftReducer,{...emptyDraftState('1'),draft:initial});if(state.draft?.kind!=='food')throw Error('Fixture');return <><output data-testid="image-row">{JSON.stringify(foodRows(state.draft)[0])}</output><FoodEditor draft={state.draft} scope={scope} dispatch={dispatch} unit="KJ" onReview={vi.fn()} /></>}
  render(<Wrapper><Harness /></Wrapper>);expect(screen.getByText('Image suggestion: Synthetic rice suggestion')).toBeTruthy();fireEvent.click(screen.getByRole('button',{name:'Choose suggested reference 1 for food 1'}));await waitFor(()=>expect(previewFoodPortion).toHaveBeenCalledOnce());expect(JSON.parse(screen.getByTestId('image-row').textContent!)).toMatchObject({referenceId:'10',mealType:''});expect((screen.getByRole('button',{name:'Review food entries'}) as HTMLButtonElement).disabled).toBe(true)
 })

})
