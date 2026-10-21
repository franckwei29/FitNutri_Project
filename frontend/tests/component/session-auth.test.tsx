import { beforeEach,describe,expect,it,vi } from 'vitest'
import { fireEvent,render,screen,waitFor } from '@testing-library/react'
import { QueryClient,QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter,RouterProvider } from 'react-router-dom'
import { SessionProvider } from '../../src/app/SessionProvider'
import { routes } from '../../src/app/router'
import { ApiRequestError } from '../../src/api/errors'
import * as adminApi from '../../src/api/admin'
import * as auth from '../../src/api/auth'
import * as api from '../../src/api/profile'
import * as summaryApi from '../../src/api/summary'
import * as recordApi from '../../src/api/records'
import {daySummary,periodSummary} from '../fixtures/summary'
import { user,profile } from '../fixtures/profile'
vi.mock('../../src/api/summary',()=>({fetchDailySummary:vi.fn(),fetchTrends:vi.fn()}))
vi.mock('../../src/api/records',()=>({fetchFoodRecords:vi.fn(),fetchExerciseRecords:vi.fn()}))
vi.mock('../../src/api/admin',()=>({fetchAccounts:vi.fn(),changeAccountStatus:vi.fn(),readAccountState:vi.fn()}))
vi.mock('../../src/api/auth',()=>({fetchCurrentUser:vi.fn(),login:vi.fn(),register:vi.fn(),logout:vi.fn()}))
vi.mock('../../src/api/profile',()=>({fetchProfile:vi.fn(),completeOnboarding:vi.fn(),updateSettings:vi.fn()}))
vi.mock('../../src/api/csrf',()=>({ensureCsrfToken:vi.fn().mockResolvedValue({headerName:'X-CSRF-TOKEN',token:'synthetic'}),clearCsrfToken:vi.fn()}))
const unauthenticated=()=>new ApiRequestError({message:'Sign in to continue.',status:401,code:'UNAUTHENTICATED',outcome:'read-failed',writeAttempted:false})
function mount(path='/login') {
  const queries=new QueryClient({defaultOptions:{queries:{retry:false,gcTime:Infinity}}}),router=createMemoryRouter(routes,{initialEntries:[path]})
  render(<QueryClientProvider client={queries}><SessionProvider><RouterProvider router={router} /></SessionProvider></QueryClientProvider>);return {queries,router}
}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(adminApi.fetchAccounts).mockResolvedValue({results:[],totalMatches:0,page:0,size:20,hasMore:false});vi.mocked(summaryApi.fetchDailySummary).mockImplementation(async date=>daySummary(date));vi.mocked(summaryApi.fetchTrends).mockImplementation(async(from,to)=>periodSummary(from,to));vi.mocked(recordApi.fetchFoodRecords).mockResolvedValue([]);vi.mocked(recordApi.fetchExerciseRecords).mockResolvedValue([]);vi.mocked(auth.fetchCurrentUser).mockRejectedValue(unauthenticated());vi.mocked(api.fetchProfile).mockResolvedValue(profile());vi.mocked(auth.login).mockResolvedValue(user);vi.mocked(auth.register).mockResolvedValue(user);vi.mocked(auth.logout).mockResolvedValue(undefined)})
async function enterLogin() {fireEvent.change(await screen.findByLabelText('Email address'),{target:{value:user.email}});fireEvent.change(screen.getByLabelText('Password'),{target:{value:'Synthetic123'}})}
describe('F03/F04 real-session routes and Demo auth presentation',()=>{
  it('anonymous protected route leads to login with an internal return path and no demo prefill',async()=>{
    const {router}=mount('/history');await screen.findByRole('heading',{name:'Welcome back.'});expect(router.state.location.pathname).toBe('/login');expect(router.state.location.search).toContain('returnTo');expect((screen.getByLabelText('Email address') as HTMLInputElement).value).toBe('');expect(screen.queryByText('Fill demo account')).toBeNull()
  })
  it('profile failure renders retry error rather than onboarding',async()=>{
    vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);vi.mocked(api.fetchProfile).mockRejectedValue(new Error('Synthetic outage'));const {router}=mount('/dashboard');await screen.findByRole('button',{name:'Retry'});expect(screen.getByRole('alert').textContent).toContain('could not be loaded');expect(screen.getByRole('main').id).toBe('main-content');expect(router.state.location.pathname).toBe('/dashboard');expect(screen.queryByRole('heading',{name:'Let’s set up your profile.'})).toBeNull()
  })
  it('restores ADMIN without profile requests and prevents user-only routes',async()=>{
    vi.mocked(auth.fetchCurrentUser).mockResolvedValue({...user,role:'ADMIN'});const {router}=mount('/profile');await screen.findByRole('heading',{name:'User management.'});expect(router.state.location.pathname).toBe('/admin');expect(api.fetchProfile).not.toHaveBeenCalled()
  })
  it('restores completed USER and redirects admin URL to dashboard',async()=>{
    vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);vi.mocked(api.fetchProfile).mockResolvedValue(profile({onboardingCompleted:true,adultConfirmed:true}));const {router}=mount('/admin');await screen.findByRole('heading',{name:'A little progress, every day.'});expect(router.state.location.pathname).toBe('/dashboard')
  })
  it('wrong password keeps the form, shows error and never loads profile',async()=>{
    vi.mocked(auth.login).mockRejectedValue(new ApiRequestError({message:'The email or password is incorrect.',status:401,code:'UNAUTHENTICATED',outcome:'rejected',writeAttempted:true}));mount();await enterLogin();fireEvent.click(screen.getByRole('button',{name:'Sign in'}));await waitFor(()=>expect(screen.getByRole('alert').textContent).toContain('incorrect'));expect(api.fetchProfile).not.toHaveBeenCalled();expect(screen.getByRole('heading',{name:'Welcome back.'})).toBeTruthy()
  })
  it('registration confirms account creation but requires an explicit login',async()=>{
    mount('/login?demo=jessica@example.com');await screen.findByRole('heading',{name:'Welcome back.'});fireEvent.click(screen.getByRole('button',{name:'Create an account'}));fireEvent.change(screen.getByLabelText('Display name'),{target:{value:' Synthetic '}});await enterLogin();fireEvent.change(screen.getByLabelText('Confirm password'),{target:{value:'Synthetic123'}});fireEvent.click(screen.getByRole('button',{name:'Create account'}));await screen.findByText('Account created. Sign in to set up your profile.');expect(auth.register).toHaveBeenCalledWith({displayName:' Synthetic ',email:user.email,password:'Synthetic123'});expect(auth.login).not.toHaveBeenCalled();expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('')
  })
  it('successful login restores profile before navigating and rejects external returnTo',async()=>{
    const {router}=mount('/login?returnTo=https%3A%2F%2Fexample.test');await enterLogin();vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);fireEvent.click(screen.getByRole('button',{name:'Sign in'}));await screen.findByRole('heading',{name:'Let’s set up your profile.'});expect(api.fetchProfile).toHaveBeenCalledOnce();expect(router.state.location.pathname).toBe('/onboarding')
  })
  it('logout waits for confirmation and failure does not claim success',async()=>{
    vi.mocked(auth.fetchCurrentUser).mockResolvedValue(user);vi.mocked(api.fetchProfile).mockResolvedValue(profile({onboardingCompleted:true,adultConfirmed:true}));vi.mocked(auth.logout).mockRejectedValueOnce(new Error('Synthetic network failure'));const {router}=mount('/dashboard');await screen.findByRole('heading',{name:'A little progress, every day.'});fireEvent.click(screen.getByRole('button',{name:'Account menu'}));fireEvent.click(screen.getByRole('menuitem',{name:'Log out'}));await screen.findByRole('button',{name:'Retry log out'});expect(screen.getByRole('alert').textContent).toContain('not confirmed');expect(screen.queryByRole('heading',{name:'A little progress, every day.'})).toBeNull();expect(screen.queryByRole('button',{name:'Account menu'})).toBeNull();expect(router.state.location.pathname).toBe('/dashboard');fireEvent.click(screen.getByRole('button',{name:'Retry log out'}));await screen.findByRole('heading',{name:'Welcome back.'});expect(router.state.location.pathname).toBe('/login')
  })
  it('duplicate registration shows the server error and does not sign in',async()=>{
    vi.mocked(auth.register).mockRejectedValue(new ApiRequestError({message:'This email is already registered.',status:409,code:'EMAIL_ALREADY_REGISTERED',outcome:'rejected',writeAttempted:true}));mount();await screen.findByRole('heading',{name:'Welcome back.'});fireEvent.click(screen.getByRole('button',{name:'Create an account'}));fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Synthetic'}});await enterLogin();fireEvent.change(screen.getByLabelText('Confirm password'),{target:{value:'Synthetic123'}});fireEvent.click(screen.getByRole('button',{name:'Create account'}));await waitFor(()=>expect(screen.getByRole('alert').textContent).toContain('already registered'));expect(auth.login).not.toHaveBeenCalled()
  })
  it('mismatched password confirmation blocks registration before dispatch',async()=>{
    mount();await screen.findByRole('heading',{name:'Welcome back.'});fireEvent.click(screen.getByRole('button',{name:'Create an account'}));fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Synthetic'}});await enterLogin();fireEvent.change(screen.getByLabelText('Confirm password'),{target:{value:'Different123'}});fireEvent.click(screen.getByRole('button',{name:'Create account'}));expect(screen.getByText('Passwords do not match.')).toBeTruthy();expect(auth.register).not.toHaveBeenCalled()
  })

})
