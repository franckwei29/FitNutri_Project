import type { EnergyUnit } from '../api/contracts'
const KJ_PER_KCAL = 4.184
function finite(value: number): number {
  if (!Number.isFinite(value)) throw new Error('Energy must be a finite number.')
  return value
}
/** Display conversion only; authoritative nutrient calculation remains on the server. */
export function energyValue(kj: number | null, unit: EnergyUnit): number | null {
  return kj === null ? null : finite(kj) / (unit === 'KCAL' ? KJ_PER_KCAL : 1)
}
export function toKilojoules(value: number, unit: EnergyUnit): number {
  return finite(value) * (unit === 'KCAL' ? KJ_PER_KCAL : 1)
}
export function displayEnergy(kj: number | null, unit: EnergyUnit, digits = 0): string {
  const value = energyValue(kj, unit)
  return value === null ? 'Unavailable' : `${new Intl.NumberFormat('en-AU', { maximumFractionDigits: digits }).format(value)} ${unit === 'KCAL' ? 'kcal' : 'kJ'}`
}
