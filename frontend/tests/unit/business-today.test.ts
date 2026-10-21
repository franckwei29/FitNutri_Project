import {act,renderHook} from '@testing-library/react'
import {afterEach,describe,expect,it,vi} from 'vitest'
import {useBusinessToday} from '../../src/hooks/useBusinessToday'
afterEach(()=>vi.useRealTimers())
describe('F11 Sydney today rollover',()=>{
 it('changes the default record date across Sydney midnight without UTC slicing',()=>{
  vi.useFakeTimers();vi.setSystemTime(new Date('2026-10-17T12:59:55Z'));const hook=renderHook(()=>useBusinessToday());expect(hook.result.current).toBe('2026-10-17')
  act(()=>vi.advanceTimersByTime(15000));expect(hook.result.current).toBe('2026-10-18')
 })
 it('updates on focus after a suspended window resumes',()=>{
  vi.useFakeTimers();vi.setSystemTime(new Date('2026-10-17T12:00:00Z'));const hook=renderHook(()=>useBusinessToday());vi.setSystemTime(new Date('2026-10-18T02:00:00Z'));act(()=>window.dispatchEvent(new Event('focus')));expect(hook.result.current).toBe('2026-10-18')
 })
})
