import type { z } from 'zod'

export class UnexpectedApiResponse extends Error {
  readonly code = 'UNEXPECTED_RESPONSE'
  constructor() { super('Unexpected server response. Refresh and try again.') }
}
/** Decode only at the API boundary. Never expose Zod input snapshots in UI/logs. */
export function decodeResponse<T>(schema: z.ZodType<T>, value: unknown): T {
  if (value !== null && typeof value === 'object' && ('passwordHash' in value || 'password_hash' in value)) throw new UnexpectedApiResponse()
  const result = schema.safeParse(value)
  if (!result.success) throw new UnexpectedApiResponse()
  return result.data
}
