export type ParsedQuantity = { ok: true; value: number } | { ok: false; message: string }
export function parseQuantity(input: string, kind: 'grams' | 'minutes'): ParsedQuantity {
  const text = input.trim()
  const precision = kind === 'grams' ? 3 : 2
  const maximum = kind === 'grams' ? 10000 : 1440
  if (!new RegExp(`^\\d+(?:\\.\\d{1,${precision}})?$`).test(text)) return { ok: false, message: `Enter ${kind} with at most ${precision} decimal places.` }
  const value = Number(text)
  if (!Number.isFinite(value) || value <= 0 || value > maximum) return { ok: false, message: `Enter ${kind} greater than zero and no more than ${maximum}.` }
  return { ok: true, value }
}
