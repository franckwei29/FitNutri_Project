import {render,screen,within} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe,expect,it} from 'vitest'
import {RecordList} from '../../src/components/RecordList'
import {IntakeChart} from '../../src/components/IntakeChart'
import {daySummary,metric} from '../fixtures/summary'
import type {ExerciseRecord,FoodRecord} from '../../src/api/contracts'
describe('F11 snapshot lists and missing-value chart',()=>{
 it('keeps legacy names, original business date, null energy and explicit zero without recalculation',()=>{
  const common={id:'9007199254740993',clientEntryId:'0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd',recordDate:'2026-10-16',source:'MANUAL' as const,version:2,dataQuality:'LEGACY_UNVERIFIED' as const,createdAt:'2026-10-17T01:00:00Z',updatedAt:'2026-10-17T01:00:00Z'}
  const food:FoodRecord={...common,reference:null,foodName:'Original historical food',quantityGrams:150,mealType:'LUNCH',nutrients:{energyKj:0,proteinGrams:null,fatGrams:null,carbsGrams:null}}
  const activity:ExerciseRecord={...common,exerciseType:null,exerciseName:'Original historical activity',durationMinutes:12.5,estimatedKj:null,estimateStatus:'LEGACY_UNVERIFIED',weightKgUsed:null,weightDateUsed:null,metUsed:null,formulaVersion:null}
  render(<MemoryRouter><RecordList foods={[food]} exercises={[activity]} unit="KJ" /></MemoryRouter>)
  expect(screen.getByText('Original historical food')).toBeTruthy();expect(screen.getByText('Original historical activity')).toBeTruthy();expect(screen.getByText('0 kJ')).toBeTruthy();expect(screen.getByText('Unavailable')).toBeTruthy()
  expect(screen.getByRole('link',{name:/Original historical food/}).getAttribute('href')).toBe('/history?from=2026-10-16&to=2026-10-16&type=food&highlight=food:9007199254740993')
 })
 it('leaves unknown dates as line breaks and keeps a real zero point',()=>{
  const days=['2026-10-14','2026-10-15','2026-10-16'].map(date=>daySummary(date).day);days[0].intake=metric({hasData:true,knownSum:100,recordCount:1,knownValueCount:1});days[2].intake=metric({hasData:true,knownSum:0,recordCount:1,knownValueCount:1})
  render(<IntakeChart days={days} unit="KJ" />)
  const table=screen.getByRole('table',{name:'Recorded intake values'})
  expect(within(screen.getByRole('row',{name:'2026-10-14 100 kJ'})).getByText('100 kJ')).toBeTruthy()
  expect(within(screen.getByRole('row',{name:'2026-10-15 Not recorded'})).queryByText('0 kJ')).toBeNull()
  expect(within(screen.getByRole('row',{name:'2026-10-16 0 kJ'})).getByText('0 kJ')).toBeTruthy()
  expect(within(table).getAllByRole('row')).toHaveLength(4)
 })
})
