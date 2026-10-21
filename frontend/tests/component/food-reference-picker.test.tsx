import {fetchRecentFoodV2} from '../../src/api/foodEntriesV2'
import {useState} from 'react'
import {act,fireEvent,render,renderHook,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import type {ReactNode} from 'react'
import type {FoodReference,SmartFoodSearchV2,ReferenceInfo} from '../../src/api/contracts'
import {createQueryClient} from '../../src/app/queryClient'
import {searchSmartFood} from '../../src/api/foodReference'
import {useFoodSearch} from '../../src/hooks/useFoodSearch'
import {FoodReferencePicker} from '../../src/features/entry/FoodReferencePicker'
vi.mock('../../src/api/foodReference',async importOriginal=>({...await importOriginal<typeof import('../../src/api/foodReference')>(),searchSmartFood:vi.fn()}))
vi.mock('../../src/api/foodEntriesV2',()=>({fetchRecentFoodV2:vi.fn().mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false})}))
const scope={ownerId:'9007199254740993',epoch:1,catalogueRevision:'revision-a'}
const dataset={id:'1',name:'AUSNUT',release:'2023',revision:'r1',sourceUrl:'https://www.foodstandards.gov.au/',nutrientPolicy:'PER_100_G',verified:true}
const rice:FoodReference={referenceId:'9007199254740994',publicFoodKey:'F007661',displayName:'Rice, white, cooked',dataset,active:true,per100g:{energyKj:659,proteinGrams:3.1,fatGrams:0.2,carbsGrams:34.7}}
const chicken:FoodReference={...rice,referenceId:'9007199254740995',publicFoodKey:'F002594',displayName:'Chicken breast'}
function page(items:FoodReference[],p=0,more=false):SmartFoodSearchV2{return {results:items.map(reference=>({reference,shortName:reference.displayName,matchKind:'EXACT',distinguishers:[]})),page:p,totalMatches:more?21:items.length,size:5,hasMore:more,nextQuestion:null,metadataRevision:'ux-test'}}
function wrapper(){const client=createQueryClient();return {client,Wrapper:({children}:{children:ReactNode})=><QueryClientProvider client={client}>{children}</QueryClientProvider>}}
function deferred<T>(){let resolve!:(v:T)=>void;const promise=new Promise<T>(r=>{resolve=r});return {promise,resolve}}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(fetchRecentFoodV2).mockResolvedValue({results:[],totalMatches:0,page:0,size:10,hasMore:false})})
describe('F07 remote search and stable selection',()=>{
 it('ignores reverse-order chicken/rice results even when transport ignores abort',async()=>{
  const old=deferred<SmartFoodSearchV2>(),current=deferred<SmartFoodSearchV2>();vi.mocked(searchSmartFood).mockImplementation(q=>q==='chicken'?old.promise:current.promise)
  const {Wrapper}=wrapper();const hook=renderHook(({query})=>useFoodSearch(query,0,scope,5),{initialProps:{query:'chicken'},wrapper:Wrapper})
  await waitFor(()=>expect(searchSmartFood).toHaveBeenCalledWith('chicken',expect.objectContaining({page:0}),expect.any(AbortSignal)))
  hook.rerender({query:'rice'});expect(hook.result.current.data).toBeUndefined()
  await waitFor(()=>expect(searchSmartFood).toHaveBeenCalledWith('rice',expect.objectContaining({page:0}),expect.any(AbortSignal)))
  await act(async()=>{current.resolve(page([rice]));await current.promise});await waitFor(()=>expect(hook.result.current.data?.results[0].reference.referenceId).toBe(rice.referenceId))
  await act(async()=>{old.resolve(page([chicken]));await old.promise});expect(hook.result.current.data?.results[0].reference.referenceId).toBe(rice.referenceId)
 })
 it('isolates cached results by owner, epoch and catalogue revision',async()=>{
  vi.mocked(searchSmartFood).mockResolvedValue(page([rice]));const {Wrapper,client}=wrapper()
  const hook=renderHook(({current})=>useFoodSearch('rice',0,current,5),{initialProps:{current:scope},wrapper:Wrapper})
  await waitFor(()=>expect(hook.result.current.data?.results).toHaveLength(1))
  hook.rerender({current:{...scope,ownerId:'2',epoch:2,catalogueRevision:'revision-b'}})
  expect(hook.result.current.data).toBeUndefined();await waitFor(()=>expect(searchSmartFood).toHaveBeenCalledTimes(2))
  expect(client.getQueryCache().getAll().every(q=>q.queryKey[0]==='owner')).toBe(true)
 })
 it('retains selection and source across page changes without local label filtering',async()=>{
  // Server may match an official synonym which is absent from displayName.
  vi.mocked(searchSmartFood).mockImplementation(async(_,options)=>({...options.page===0?page([rice],0,true):page([chicken],1),size:options.size}))
  const {Wrapper}=wrapper();function Harness(){const [selected,setSelected]=useState<ReferenceInfo|null>(rice);return <FoodReferencePicker scope={scope} value={selected} onSelect={setSelected} label="Food 1" />}
  render(<Wrapper><Harness /></Wrapper>);const input=screen.getByRole('combobox',{name:'Food 1'})
  fireEvent.change(input,{target:{value:'official synonym'}});fireEvent.keyDown(input,{key:'ArrowDown'})
  await screen.findByRole('option',{name:/Rice, white, cooked/});fireEvent.click(screen.getByText('Food details'));expect(screen.getByText(/AUSNUT · 2023 · r1/)).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:'More results'}));await waitFor(()=>expect(searchSmartFood).toHaveBeenLastCalledWith('official synonym',expect.objectContaining({mode:'ALL',page:0}),expect.any(AbortSignal)));fireEvent.click(await screen.findByRole('button',{name:'Next results'}));await waitFor(()=>expect(searchSmartFood).toHaveBeenCalledWith('official synonym',expect.objectContaining({mode:'ALL',page:1}),expect.any(AbortSignal)))
  expect(screen.getByText('Selected: Rice, white, cooked')).toBeTruthy()
  expect(screen.getByRole('link',{name:'Official dataset source'}).getAttribute('href')).toBe(dataset.sourceUrl)
 })
 it('shows failure and explicit retry without fallback rows, and shows genuine no matches',async()=>{
  vi.mocked(searchSmartFood).mockRejectedValue(new Error('Catalogue unavailable'));const {Wrapper}=wrapper()
  render(<Wrapper><FoodReferencePicker scope={scope} value={null} onSelect={vi.fn()} label="Food 2" /></Wrapper>)
  fireEvent.change(screen.getByRole('combobox',{name:'Food 2'}),{target:{value:'rice'}})
  expect(await screen.findByRole('alert')).toHaveProperty('textContent','Catalogue unavailable')
  expect(screen.queryAllByRole('option')).toHaveLength(0)
  vi.mocked(searchSmartFood).mockResolvedValue(page([]));fireEvent.click(screen.getByRole('button',{name:'Retry food search'}));await screen.findByText('No matching foods. Try another search.')
 })
})
