import { describe, expect, it } from 'vitest'
import { isValidLocalDate, todayInZone } from '../../src/domain/dates'
import { displayEnergy, energyValue, toKilojoules } from '../../src/domain/units'
import { parseQuantity } from '../../src/domain/inputRules'

describe('F01 calendar dates, display units and form boundaries', () => {
  it('rejects impossible dates and preserves leap days', () => {
    expect(isValidLocalDate('2026-02-30')).toBe(false)
    expect(isValidLocalDate('2024-02-29')).toBe(true)
    expect(isValidLocalDate('2026-02-29')).toBe(false)
    expect(isValidLocalDate('2026-1-01')).toBe(false)
  })
  it('uses Sydney local midnight and DST without shifting saved strings', () => {
    const instant = new Date('2026-10-03T14:30:00Z')
    expect(todayInZone('Australia/Sydney', instant)).toBe('2026-10-04')
    expect(todayInZone('UTC', instant)).toBe('2026-10-03')
    expect(() => todayInZone('invalid/zone', instant)).toThrow()
  })
  it('distinguishes missing energy and real zero and converts display only', () => {
    expect(energyValue(null,'KCAL')).toBeNull()
    expect(displayEnergy(null,'KJ')).toBe('Unavailable')
    expect(displayEnergy(0,'KJ')).toBe('0 kJ')
    expect(energyValue(418.4,'KCAL')).toBeCloseTo(100,10)
    expect(toKilojoules(100,'KCAL')).toBeCloseTo(418.4,10)
    expect(() => displayEnergy(Number.NaN,'KJ')).toThrow()
  })
  it('rejects blank/zero/negative/overflow/nonfinite inputs without Number-empty coercion', () => {
    for (const value of ['', ' ', '0','-1','10000.001','NaN','Infinity','1e3','1.0001'])
      expect(parseQuantity(value,'grams').ok).toBe(false)
    expect(parseQuantity('150.125','grams')).toEqual({ok:true,value:150.125})
    expect(parseQuantity('1440','minutes')).toEqual({ok:true,value:1440})
    expect(parseQuantity('10.001','minutes').ok).toBe(false)
    expect(parseQuantity('1440.01','minutes').ok).toBe(false)
  })
})
