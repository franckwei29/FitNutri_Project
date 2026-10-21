import {describe,it,expect} from 'vitest'
import {DaySummarySchema,PeriodSummarySchema} from '../../src/api/schemas'
import {daySummary,periodSummary} from '../fixtures/summary'
describe('UX-B15 leaf and root counts are separate server facts',()=>{
 it('retains five components/two food roots and two periods/one workout without rewriting leaf counts',()=>{const source=daySummary('2020-01-02');source.day={...source.day,foodCount:5,exerciseCount:2,...{foodEntryCount:2,exerciseEntryCount:1}};const result=DaySummarySchema.parse(source);expect(result.day).toMatchObject({foodCount:5,foodEntryCount:2,exerciseCount:2,exerciseEntryCount:1});const range=periodSummary('2020-01-02','2020-01-02');range.days=[source.day];range.totals={...range.totals,foodCount:5,exerciseCount:2,...{foodEntryCount:2,exerciseEntryCount:1}};expect(PeriodSummarySchema.parse(range).totals).toMatchObject({foodCount:5,foodEntryCount:2,exerciseCount:2,exerciseEntryCount:1})})
 it('does not accept negative root counts',()=>{const source=daySummary('2020-01-02');expect(DaySummarySchema.safeParse({...source,day:{...source.day,foodEntryCount:-1}}).success).toBe(false)})
})
