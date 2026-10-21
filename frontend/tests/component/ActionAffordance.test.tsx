import {DraftProvider} from '../../src/app/DraftProvider'
import {fireEvent,render,screen} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe,expect,it,vi} from 'vitest'
import {ProfileSections} from '../../src/features/profile/ProfileSections'
import {initialiseProfileForm} from '../../src/features/profile/profileForm'
import {DashboardPage} from '../../src/pages/DashboardPage'
import {profile,user} from '../fixtures/profile'
import {daySummary,periodSummary} from '../fixtures/summary'
vi.mock('../../src/hooks/useSession',()=>({useSession:()=>({state:{status:'authenticated',user,profile:profile()}})}))
vi.mock('../../src/hooks/useBusinessToday',()=>({useBusinessToday:()=> '2026-10-17'}))
vi.mock('../../src/hooks/useSummary',()=>({useDaySummary:()=>({data:daySummary('2026-10-17')}),usePeriodSummary:()=>({isPending:true,data:periodSummary('2026-10-11','2026-10-17')})}))
vi.mock('../../src/hooks/useRecordQueries',()=>({useEntryRootQueries:()=>({foods:{isSuccess:true,data:{results:[],totalMatches:0}},exercises:{isSuccess:true,data:{results:[],totalMatches:0}}})}))
describe('UX-F01 action affordance and consent',()=>{
 it('aligns the consent checkbox to the first text line and keeps the label clickable',()=>{
  const update=vi.fn()
  render(<ProfileSections form={initialiseProfileForm(profile(),'2026-10-17')} setForm={update} errors={{}} step={2} onboarding today="2026-10-17" />)
  const checkbox=screen.getByRole('checkbox',{name:/Allow FitNutri/})
  const label=checkbox.closest('label')!
  expect(getComputedStyle(label).alignItems).toBe('flex-start')
  expect(getComputedStyle(label).marginLeft).toBe('0px')
  fireEvent.click(screen.getByText(/Allow FitNutri/))
  expect(update).toHaveBeenCalledWith(expect.objectContaining({qaConsentGranted:true}))
 })
 it('distinguishes primary Add actions from secondary navigation and read-only metrics',()=>{
  render(<DraftProvider ownerId={user.id}><MemoryRouter><DashboardPage /></MemoryRouter></DraftProvider>)
  for(const name of ['Add food','Add exercise'])expect(screen.getByRole('link',{name}).className).toContain('MuiButton-contained')
  expect(screen.getByRole('button',{name:'Refresh'}).className).toContain('MuiButton-outlined')
  expect(screen.getByRole('link',{name:'History'}).className).toContain('MuiButton-outlined')
  const metric=screen.getByRole('region',{name:'Energy intake'})
  expect(metric.tagName).toBe('SECTION');expect(metric.hasAttribute('tabindex')).toBe(false)
  expect(metric.querySelector('button,a')).toBeNull()
 })
})
