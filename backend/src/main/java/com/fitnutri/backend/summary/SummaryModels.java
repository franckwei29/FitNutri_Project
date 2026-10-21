package com.fitnutri.backend.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Canonical recorded-data DTOs; nullable values are not zero or a clinical estimate. */
public final class SummaryModels {
    private SummaryModels(){}
    public record MetricAggregate(BigDecimal knownSum,long recordCount,long knownValueCount,long missingValueCount,long legacyValueCount,boolean hasData,boolean partial){
        public static MetricAggregate of(long count,long known,BigDecimal sum,long legacy){if(count<0||known<0||known>count||legacy<0||legacy>known||(known==0)!=(sum==null))throw new IllegalArgumentException("Inconsistent aggregate counts/value.");return new MetricAggregate(sum,count,known,count-known,legacy,count>0,known>0&&known<count);}
        public static MetricAggregate empty(){return of(0,0,null,0);}
    }
    public record DayAggregate(LocalDate recordDate,MetricAggregate intake,MetricAggregate protein,MetricAggregate fat,MetricAggregate carbs,MetricAggregate expenditure,BigDecimal activityMinutes,long foodCount,long exerciseCount,long foodEntryCount,long exerciseEntryCount){}
    public record Totals(MetricAggregate intake,MetricAggregate protein,MetricAggregate fat,MetricAggregate carbs,MetricAggregate expenditure,BigDecimal activityMinutes,long foodCount,long exerciseCount,long foodEntryCount,long exerciseEntryCount){}
    public record Average(BigDecimal value,int knownDayCount){}
    public record Averages(Average intake,Average protein,Average fat,Average carbs,Average expenditure,Average activityMinutes){}
    public record GoalComparison(BigDecimal dailyTargetKj,int comparedDays,BigDecimal knownIntakeKj,BigDecimal targetForComparedDaysKj){}
    public record Fact(String code,String text,Map<String,Object> values){public Fact{values=Collections.unmodifiableMap(new LinkedHashMap<>(values));}}
    public record PeriodSummary(LocalDate rangeStart,LocalDate rangeEnd,int calendarDayCount,List<DayAggregate> days,Totals totals,Averages averages,GoalComparison goalComparison,List<Fact> facts){
        public PeriodSummary{days=List.copyOf(days);facts=List.copyOf(facts);}
        public PeriodSummary withFacts(List<Fact> value){return new PeriodSummary(rangeStart,rangeEnd,calendarDayCount,days,totals,averages,goalComparison,value);}
    }
    public record DaySummaryResponse(DayAggregate day,GoalComparison goalComparison,List<Fact> facts){public DaySummaryResponse{facts=List.copyOf(facts);}}
    public record WeeklySummaryResponse(PeriodSummary current,PeriodSummary previous){}
}
