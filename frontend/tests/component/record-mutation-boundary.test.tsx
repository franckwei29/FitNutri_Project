import {fireEvent,render,screen,waitFor} from '@testing-library/react'
import {QueryClientProvider} from '@tanstack/react-query'
import {createMemoryRouter,RouterProvider} from 'react-router-dom'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import {SessionController} from '../../src/app/sessionEpoch'
import {SessionContext} from '../../src/app/SessionContext'
import {DraftProvider} from '../../src/app/DraftProvider'
import {createQueryClient} from '../../src/app/queryClient'
import {AppLayout} from '../../src/layouts/AppLayout'
import {useDraft} from '../../src/hooks/useDraft'
import {fetchFoodRecord} from '../../src/api/records'
import {ApiRequestError} from '../../src/api/errors'
import {savedFood} from '../fixtures/records'
import {profile,user} from '../fixtures/profile'
vi.mock('../../src/api/records',()=>({fetchFoodRecord:vi.fn(),fetchExerciseRecord:vi.fn()}))
beforeEach(()=>vi.resetAllMocks())
describe('F13 shared pending change navigation boundary',()=>{
 it('blocks leaving an uncertain DELETE even without a draft and recovers through GET only',async()=>{
  const client=createQueryClient(),actions=new SessionController(client,{fetchMe:async()=>user,fetchProfile:async()=>profile({user,adultConfirmed:true,onboardingCompleted:true}),login:async()=>user,logout:async()=>{},csrf:async()=>{}});await actions.restore()
  function Probe(){const {dispatch}=useDraft();return <button onClick={()=>{const mutationId='27d4db90-dc34-4010-a2dc-eaf902c74181';dispatch({type:'BEGIN_RECORD_MUTATION',ownerId:user.id,ownerEpoch:actions.getSnapshot().epoch,mutationId,intent:{operation:'DELETE',kind:'food',recordId:savedFood().id,expectedVersion:2,original:savedFood()}});dispatch({type:'RECORD_MUTATION_UNKNOWN',ownerId:user.id,ownerEpoch:actions.getSnapshot().epoch,mutationId})}}>Seed uncertain deletion</button>}
  const router=createMemoryRouter([{element:<AppLayout />,children:[{path:'/dashboard',element:<Probe />},{path:'/history',element:<p>History destination</p>}]}],{initialEntries:['/dashboard']});render(<QueryClientProvider client={client}><SessionContext.Provider value={{state:actions.getSnapshot(),actions}}><DraftProvider ownerId={user.id}><RouterProvider router={router} /></DraftProvider></SessionContext.Provider></QueryClientProvider>)
  fireEvent.click(screen.getByRole('button',{name:'Seed uncertain deletion'}));await screen.findByText('A record change is awaiting confirmation. Its original input and version remain frozen.')
  fireEvent.click(screen.getByRole('link',{name:'History'}));await screen.findByRole('dialog',{name:'Confirmation is still pending'});expect(screen.getByText("The save may already have completed. Leaving won't cancel it. Check History before recording it again.")).toBeTruthy();expect(router.state.location.pathname).toBe('/dashboard');fireEvent.click(screen.getByRole('button',{name:'Stay on this page'}))
  vi.mocked(fetchFoodRecord).mockRejectedValue(new ApiRequestError({message:'Not found',status:404,code:'NOT_FOUND',outcome:'read-failed',writeAttempted:false}));fireEvent.click(await screen.findByRole('button',{name:'Check current record'}));await screen.findByText('The previously read record is no longer visible. This does not prove which request removed it.');fireEvent.click(screen.getByRole('link',{name:'History'}));await waitFor(()=>expect(router.state.location.pathname).toBe('/history'))
 })
})
