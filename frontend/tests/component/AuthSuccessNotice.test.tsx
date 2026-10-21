import {act,fireEvent,render,screen} from '@testing-library/react'
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest'
import {TransientSuccessAlert} from '../../src/components/TransientSuccessAlert'
import {MemoryRouter} from 'react-router-dom'
import {AuthPage} from '../../src/pages/AuthPage'
vi.mock('../../src/hooks/useSession',()=>({useSession:()=>({actions:{signIn:vi.fn()}})}))
vi.mock('../../src/api/auth',()=>({register:vi.fn().mockResolvedValue({})}))
beforeEach(()=>vi.useFakeTimers())
afterEach(()=>vi.useRealTimers())
describe('UX-F00 / TUX-004–006 transient success',()=>{
 it('fades after2500ms and removes its entire layout by2800ms',()=>{
  render(<TransientSuccessAlert notice={{id:1,message:'Saved'}} />)
  expect(screen.getByRole('status').textContent).toContain('Saved')
  act(()=>vi.advanceTimersByTime(2500))
  expect(screen.getByRole('status').getAttribute('data-phase')).toBe('exiting')
  act(()=>vi.advanceTimersByTime(300))
  expect(screen.queryByRole('status')).toBeNull()
 })
 it('restarts the clock for a new notice and clears timers on unmount',()=>{
  const view=render(<TransientSuccessAlert notice={{id:1,message:'First'}} />)
  act(()=>vi.advanceTimersByTime(2600))
  view.rerender(<TransientSuccessAlert notice={{id:2,message:'Second'}} />)
  act(()=>vi.advanceTimersByTime(200))
  expect(screen.getByRole('status').textContent).toContain('Second')
  expect(screen.getByRole('status').getAttribute('data-phase')).toBe('visible')
  view.unmount();expect(vi.getTimerCount()).toBe(0)
 })
 it('registration uses the transient notice and leaves login errors visible',async()=>{
  render(<MemoryRouter><AuthPage /></MemoryRouter>)
  fireEvent.click(screen.getByRole('button',{name:'Create an account'}))
  for(const [label,value] of [['Display name','Synthetic'],['Email address','ux@example.test'],['Password','Synthetic123'],['Confirm password','Synthetic123']])fireEvent.change(screen.getByLabelText(label),{target:{value}})
  await act(async()=>fireEvent.click(screen.getByRole('button',{name:'Create account'})))
  expect(screen.getByRole('status').textContent).toContain('Account created')
  expect(screen.getByRole('heading',{name:'Welcome back.'})).toBeTruthy()
  act(()=>vi.advanceTimersByTime(2800))
  expect(screen.queryByRole('status')).toBeNull()
  fireEvent.click(screen.getByRole('button',{name:'Sign in'}))
  act(()=>vi.advanceTimersByTime(5000))
  expect(screen.getByText('Enter your password.')).toBeTruthy()
 })
})

 it('does not restart an already elapsed saved notice when the same event remounts',()=>{const startedAt=Date.now(),notice={id:9,message:'Earlier save',startedAt};const first=render(<TransientSuccessAlert notice={notice} />);act(()=>vi.advanceTimersByTime(2000));first.unmount();act(()=>vi.advanceTimersByTime(1000));render(<TransientSuccessAlert notice={notice} />);expect(screen.queryByText('Earlier save')).toBeNull()})
