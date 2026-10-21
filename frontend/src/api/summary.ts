import type {DaySummary,PeriodSummary,WeeklySummary} from './contracts'
import {DaySummarySchema,PeriodSummarySchema,WeeklySummarySchema} from './schemas'
import {apiGet} from './client'
import {decodeResponse,UnexpectedApiResponse} from './validators'
import {DEFAULT_ZONE,shiftCalendarDate,isValidRecordRange} from '../domain/dates'
export async function fetchTodaySummary(signal?:AbortSignal):Promise<DaySummary>{return decodeResponse(DaySummarySchema,await apiGet('/summary/today',{params:{zone:DEFAULT_ZONE},signal}))}
export async function fetchDailySummary(date:string,signal?:AbortSignal):Promise<DaySummary>{const result=decodeResponse(DaySummarySchema,await apiGet('/summary/daily',{params:{date,zone:DEFAULT_ZONE},signal}));if(result.day.recordDate!==date)throw new UnexpectedApiResponse();return result}
export async function fetchWeeklySummary(end:string,signal?:AbortSignal):Promise<WeeklySummary>{const result=decodeResponse(WeeklySummarySchema,await apiGet('/summary/weekly',{params:{end,zone:DEFAULT_ZONE},signal}));if(result.current.rangeEnd!==end||result.current.rangeStart!==shiftCalendarDate(end,-6)||result.current.calendarDayCount!==7||result.previous.rangeEnd!==shiftCalendarDate(end,-7)||result.previous.rangeStart!==shiftCalendarDate(end,-13)||result.previous.calendarDayCount!==7)throw new UnexpectedApiResponse();checkPeriodDates(result.current);checkPeriodDates(result.previous);return result}
export async function fetchTrends(from:string,to:string,signal?:AbortSignal):Promise<PeriodSummary>{const result=decodeResponse(PeriodSummarySchema,await apiGet('/summary/trends',{params:{from,to,zone:DEFAULT_ZONE},signal}));if(result.rangeStart!==from||result.rangeEnd!==to)throw new UnexpectedApiResponse();checkPeriodDates(result);return result}

function checkPeriodDates(value:PeriodSummary){if(!isValidRecordRange(value.rangeStart,value.rangeEnd)||value.days.length!==value.calendarDayCount||value.days.some((d,i)=>d.recordDate!==shiftCalendarDate(value.rangeStart,i))||value.days.at(-1)?.recordDate!==value.rangeEnd)throw new UnexpectedApiResponse()}
