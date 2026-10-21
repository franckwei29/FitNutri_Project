import type {ReactNode} from 'react'
import {render,screen,within} from '@testing-library/react'
import {describe,expect,it,vi} from 'vitest'
import {IntakeChart} from '../../src/components/IntakeChart'
import {daySummary,metric} from '../fixtures/summary'
vi.mock('recharts',()=>({
 ResponsiveContainer:({children}:{children:ReactNode})=><div>{children}</div>,
 LineChart:({data,children}:{data:unknown;children:ReactNode})=><div data-testid="points" data-points={JSON.stringify(data)}>{children}</div>,
 Line:({connectNulls}:{connectNulls:boolean})=><span data-testid="line" data-connect={String(connectNulls)} />,
 XAxis:({interval}:{interval:string})=><span data-testid="x-axis">{interval}</span>,
 YAxis:({label}:{label:{value:string}})=><span data-testid="y-axis">{label.value}</span>,
 CartesianGrid:()=>null,Tooltip:()=>null,
}))
describe('UX-F02 canonical chart axes and accessible values',()=>{
 it('keeps null gaps and recorded zero with numeric labelled energy axes and date bounds',()=>{
  const days=['2026-10-15','2026-10-16','2026-10-17'].map(d=>daySummary(d).day)
  days[0].intake=metric({hasData:true,knownSum:4184,recordCount:1,knownValueCount:1})
  days[2].intake=metric({hasData:true,knownSum:0,recordCount:1,knownValueCount:1})
  render(<IntakeChart days={days} unit="KCAL" />)
  expect(screen.getByTestId('y-axis').textContent).toBe('Energy (kcal)')
  expect(screen.getByTestId('x-axis').textContent).toBe('preserveStartEnd')
  expect(JSON.parse(screen.getByTestId('points').getAttribute('data-points')!).map((p:{intake:number|null})=>p.intake)).toEqual([1000,null,0])
  expect(screen.getByTestId('line').getAttribute('data-connect')).toBe('false')
  const table=screen.getByRole('table',{name:'Recorded intake values'})
  expect(within(table).getByText('1,000 kcal')).toBeTruthy()
  expect(within(table).getByText('Not recorded')).toBeTruthy()
  expect(within(table).getByText('0 kcal')).toBeTruthy()
 })
 it('shows an honest empty state instead of a fabricated zero line',()=>{
  render(<IntakeChart days={[daySummary('2026-10-17').day]} unit="KJ" />)
  expect(screen.queryByTestId('line')).toBeNull()
  expect(screen.getByText('No known intake values in this period.')).toBeTruthy()
 })
})
