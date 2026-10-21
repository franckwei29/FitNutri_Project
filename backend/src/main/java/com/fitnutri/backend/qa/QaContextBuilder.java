package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.summary.SummaryService;
import com.fitnutri.backend.summary.SummaryModels.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Explicit external-data whitelist. Never serializes PeriodSummary, profile/identity, names, raw weights or arbitrary facts maps. */
@Service
public final class QaContextBuilder {
 private final SummaryService summaries;private final RecordDatePolicy dates;
 public QaContextBuilder(SummaryService summaries,RecordDatePolicy dates){this.summaries=summaries;this.dates=dates;}
 public void validateRange(QaRequest.RecordRange range,String zone){
  if(zone==null||zone.isBlank())throw BusinessException.invalid("zone","Choose a supported IANA time zone.");dates.today(zone);
  if(range!=null){if(range.from()==null||range.to()==null)throw BusinessException.invalid("recordRange","Choose both record range dates.");if(dates.requireRange(range.from(),range.to(),zone).dayCount()>31)throw BusinessException.invalid("recordRange","Choose an inclusive record range of 1 to 31 days.");}
 }
 public AllowedFacts buildAllowedFacts(Long owner,QaRequest.RecordRange range,String zone){
  validateRange(range,zone);if(range==null)return null;if(owner==null||owner<1)throw BusinessException.invalid("recordRange","Use the current signed-in account.");
  var period=summaries.period(owner,range.from(),range.to(),zone);var totals=period.totals();var goal=period.goalComparison();
  var days=period.days().stream().map(day->new DayFacts(day.recordDate().toString(),measures(day.intake(),day.protein(),day.fat(),day.carbs(),day.expenditure(),day.activityMinutes(),day.foodCount(),day.exerciseCount(),day.foodEntryCount(),day.exerciseEntryCount()))).toList();
  return new AllowedFacts("qa-facts-1",range.from().toString(),range.to().toString(),period.calendarDayCount(),days,measures(totals.intake(),totals.protein(),totals.fat(),totals.carbs(),totals.expenditure(),totals.activityMinutes(),totals.foodCount(),totals.exerciseCount(),totals.foodEntryCount(),totals.exerciseEntryCount()),goal==null?null:new CurrentTargetFacts(goal.dailyTargetKj(),goal.comparedDays(),goal.knownIntakeKj(),goal.targetForComparedDaysKj(),true),List.of("Only recorded data for the selected range is included. Unrecorded meals and activities are unknown.","Null totals mean no known value, never zero; missing and legacy counts are retained.","Activity energy is a general recorded estimate, not an intake allowance or target offset.","Any target comparison uses the current explicit target, not a historical target or a clinical recommendation."));
 }
 private Measures measures(MetricAggregate intake,MetricAggregate protein,MetricAggregate fat,MetricAggregate carbs,MetricAggregate expenditure,BigDecimal minutes,long food,long exercise,long foodRoots,long exerciseRoots){return new Measures(metric(intake),metric(protein),metric(fat),metric(carbs),metric(expenditure),minutes,food,exercise,foodRoots,exerciseRoots);}
 private MetricFacts metric(MetricAggregate value){return new MetricFacts(value.knownSum(),value.recordCount(),value.knownValueCount(),value.missingValueCount(),value.legacyValueCount());}
 public record MetricFacts(BigDecimal knownTotal,long recordCount,long knownValueCount,long missingValueCount,long legacyValueCount){}
 public record Measures(MetricFacts intakeKj,MetricFacts proteinGrams,MetricFacts fatGrams,MetricFacts carbsGrams,MetricFacts expenditureKj,BigDecimal activityMinutes,long foodCount,long exerciseCount,long foodEntryCount,long exerciseEntryCount){}
 public record DayFacts(String recordDate,Measures recorded){}
 public record CurrentTargetFacts(BigDecimal dailyTargetKj,int comparedKnownDays,BigDecimal knownIntakeKj,BigDecimal targetForComparedDaysKj,boolean currentReferenceOnly){}
 public record AllowedFacts(String schemaVersion,String rangeStart,String rangeEnd,int calendarDays,List<DayFacts> days,Measures totals,CurrentTargetFacts currentTarget,List<String> notices){public AllowedFacts{days=List.copyOf(days);notices=List.copyOf(notices);}}
}
