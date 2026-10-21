export const DEFAULT_ZONE = 'Australia/Sydney'
/** Pure calendar validation; dates remain YYYY-MM-DD and are never converted to instants. */
export function isValidLocalDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  if (year === undefined || month === undefined || day === undefined || year < 1900 || month < 1 || month > 12 || day < 1) return false
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  const maximum = days[month - 1]
  return maximum !== undefined && day <= maximum
}
export function todayInZone(zone = DEFAULT_ZONE, now = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-AU', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(now)
  const year = parts.find(x => x.type === 'year')?.value
  const month = parts.find(x => x.type === 'month')?.value
  const day = parts.find(x => x.type === 'day')?.value
  if (!year || !month || !day) throw new Error('Could not resolve the application date.')
  return `${year}-${month}-${day}`
}

/** Calendar arithmetic on a validated date, independent of the machine timezone and DST. */
export function shiftCalendarDate(date:string,days:number):string {
  if(!isValidLocalDate(date)||!Number.isInteger(days)) throw Error('Enter a valid calendar date and day offset.')
  const [year,month,day]=date.split('-').map(Number),shifted=new Date(Date.UTC(year,month-1,day+days))
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth()+1).padStart(2,'0')}-${String(shifted.getUTCDate()).padStart(2,'0')}`
}
export function isValidRecordRange(from:string,to:string){return isValidLocalDate(from)&&isValidLocalDate(to)&&from<=to&&to<=shiftCalendarDate(from,365)}
