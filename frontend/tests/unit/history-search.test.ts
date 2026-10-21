import {describe,expect,it} from 'vitest'
import {parseHistorySearch,savedRecordLocation,correctedRootLocation} from '../../src/features/history/historySearch'
describe('F12 history URL contract',()=>{
 it('defaults only missing fields to seven calendar days and keeps explicit historical locations',()=>{
  expect(parseHistorySearch(new URLSearchParams(),'2026-10-17')).toMatchObject({valid:true,from:'2026-10-11',to:'2026-10-17',type:'all',summary:'period',summaryDate:'2026-10-17',highlight:null})
  const value=parseHistorySearch(new URLSearchParams('from=2026-09-17&to=2026-09-17&type=food&highlight=food:9007199254740993'),'2026-10-17')
  expect(value).toMatchObject({valid:true,from:'2026-09-17',to:'2026-09-17',type:'food',highlight:{kind:'food',id:'9007199254740993'}})
 })
 it.each(['from=2026-02-30','from=&to=2026-10-17','from=2026-10-17&to=2026-10-16','from=2024-01-01&to=2026-10-17','to=2026-10-18','type=other','highlight=food:0','highlight=exercise:abc','summary=other','summaryDate=wrong','type=food&type=exercise'])('does not silently accept invalid filters: %s',query=>{expect(parseHistorySearch(new URLSearchParams(query),'2026-10-17').valid).toBe(false)})
 it('accepts inclusive 366 days and rejects 367 days',()=>{
  expect(parseHistorySearch(new URLSearchParams('from=2025-10-17&to=2026-10-17'),'2026-10-17').valid).toBe(true)
  expect(parseHistorySearch(new URLSearchParams('from=2025-10-16&to=2026-10-17'),'2026-10-17').valid).toBe(false)
 })
 it('locates a confirmed historical record with an inclusive matching range and compatible filter',()=>{
  expect(savedRecordLocation('exercise','9007199254740993','2026-09-17')).toBe('/history?from=2026-09-17&to=2026-09-17&type=all&highlight=exercise:9007199254740993')
 })
})

it.each(['food','exercise'] as const)('keeps a compatible %s filter and the original multi-day range after correction',async type=>{const {savedSingleV2,savedWorkoutV2}=await import('../fixtures/entryRootsV2');const root=type==='food'?savedSingleV2():savedWorkoutV2(),target=new URL(correctedRootLocation('/history?from=2020-01-01&to=2020-01-03&type='+type+'&summary=daily&summaryDate=2020-01-02',root),'http://local.invalid');expect(target.searchParams.get('type')).toBe(type);expect(target.searchParams.get('from')).toBe('2020-01-01');expect(target.searchParams.get('to')).toBe('2020-01-03');expect(target.searchParams.get('summary')).toBe('daily');expect(target.searchParams.get('highlightRoot')).toBe(type==='food'?'single:8':'workout:7')})
it('moves an outside-range correction to its actual date and makes an incompatible filter show that root',async()=>{const {savedSingleV2}=await import('../fixtures/entryRootsV2');const root=savedSingleV2(),target=new URL(correctedRootLocation('/history?from=2020-01-01&to=2020-01-01&type=exercise',root),'http://local.invalid');expect(target.searchParams.get('type')).toBe('food');expect(target.searchParams.get('from')).toBe(root.recordDate);expect(target.searchParams.get('to')).toBe(root.recordDate)})
