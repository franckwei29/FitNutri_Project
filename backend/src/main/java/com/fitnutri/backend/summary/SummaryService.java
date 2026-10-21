package com.fitnutri.backend.summary;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.summary.SummaryModels.*;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** One repeatable database snapshot and one recorded-data policy for today, daily, weekly and arbitrary ranges. */
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class SummaryService {
    private final SummaryRepository repository;private final UserProfileRepository profiles;private final WeightEntryRepository weights;private final RecordDatePolicy dates;private final RuleSummaryService rules;
    public SummaryService(SummaryRepository repository,UserProfileRepository profiles,WeightEntryRepository weights,RecordDatePolicy dates,RuleSummaryService rules){this.repository=repository;this.profiles=profiles;this.weights=weights;this.dates=dates;this.rules=rules;}
    public DaySummaryResponse today(Long owner,String zone){return daily(owner,dates.today(zone),zone);}
    public DaySummaryResponse daily(Long owner,LocalDate date,String zone){dates.requireDate(date,zone);var current=pair(owner,date,date,zone).current();return new DaySummaryResponse(current.days().getFirst(),current.goalComparison(),current.facts());}
    public WeeklySummaryResponse weekly(Long owner,LocalDate end,String zone){dates.requireDate(end,zone);return pair(owner,end.minusDays(6),end,zone);}
    public PeriodSummary period(Long owner,LocalDate from,LocalDate to,String zone){return pair(owner,from,to,zone).current();}
    public PeriodSummary trends(Long owner,LocalDate from,LocalDate to,Integer days,String zone){
        if(from!=null||to!=null){if(from==null||to==null||days!=null)throw BusinessException.invalid("from","Use either complete from/to or a days shortcut, not both.");return period(owner,from,to,zone);}
        int length=days==null?7:days;if(length<1||length>366)throw BusinessException.invalid("days","Choose 1 to366 days.");LocalDate end=dates.today(zone);return period(owner,end.minusDays(length-1L),end,zone);
    }
    private WeeklySummaryResponse pair(Long owner,LocalDate from,LocalDate to,String zone){
        int length=dates.requireRange(from,to,zone).dayCount();LocalDate previousFrom=from.minusDays(length),previousTo=from.minusDays(1);
        var food=repository.foodDays(owner,previousFrom,to).stream().collect(Collectors.toMap(SummaryRepository.FoodDay::date,Function.identity()));
        var exercise=repository.exerciseDays(owner,previousFrom,to).stream().collect(Collectors.toMap(SummaryRepository.ExerciseDay::date,Function.identity()));
        var measured=weights.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(owner,previousFrom,to);BigDecimal target=profiles.findDailyTargetKjByUserId(owner).orElse(null);
        if(target!=null&&target.signum()<=0)throw new IllegalStateException("Invalid stored explicit target.");
        var prior=raw(previousFrom,previousTo,food,exercise,target);var current=raw(from,to,food,exercise,target);
        return new WeeklySummaryResponse(current.withFacts(rules.buildFacts(current,prior,measured)),prior.withFacts(rules.buildFacts(prior,null,measured)));
    }
    private PeriodSummary raw(LocalDate from,LocalDate to,Map<LocalDate,SummaryRepository.FoodDay> foods,Map<LocalDate,SummaryRepository.ExerciseDay> exercises,BigDecimal target){
        List<DayAggregate> days=new ArrayList<>();for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1)){var f=foods.get(day);var e=exercises.get(day);days.add(new DayAggregate(day,f==null?MetricAggregate.empty():f.intake(),f==null?MetricAggregate.empty():f.protein(),f==null?MetricAggregate.empty():f.fat(),f==null?MetricAggregate.empty():f.carbs(),e==null?MetricAggregate.empty():e.expenditure(),e==null?null:e.activityMinutes(),f==null?0:f.count(),e==null?0:e.count(),f==null?0:f.entryCount(),e==null?0:e.entryCount()));}
        var totals=new Totals(sum(days,DayAggregate::intake),sum(days,DayAggregate::protein),sum(days,DayAggregate::fat),sum(days,DayAggregate::carbs),sum(days,DayAggregate::expenditure),sumValues(days.stream().map(DayAggregate::activityMinutes).toList()),days.stream().mapToLong(DayAggregate::foodCount).sum(),days.stream().mapToLong(DayAggregate::exerciseCount).sum(),days.stream().mapToLong(DayAggregate::foodEntryCount).sum(),days.stream().mapToLong(DayAggregate::exerciseEntryCount).sum());
        var averages=new Averages(average(days.stream().map(d->d.intake().knownSum()).toList()),average(days.stream().map(d->d.protein().knownSum()).toList()),average(days.stream().map(d->d.fat().knownSum()).toList()),average(days.stream().map(d->d.carbs().knownSum()).toList()),average(days.stream().map(d->d.expenditure().knownSum()).toList()),average(days.stream().map(DayAggregate::activityMinutes).toList()));
        GoalComparison goal=target==null?null:new GoalComparison(target,averages.intake().knownDayCount(),totals.intake().knownSum(),target.multiply(BigDecimal.valueOf(averages.intake().knownDayCount())));
        return new PeriodSummary(from,to,days.size(),days,totals,averages,goal,List.of());
    }
    private static MetricAggregate sum(List<DayAggregate> days,Function<DayAggregate,MetricAggregate> read){long count=0,known=0,legacy=0;List<BigDecimal> values=new ArrayList<>();for(var day:days){var metric=read.apply(day);count+=metric.recordCount();known+=metric.knownValueCount();legacy+=metric.legacyValueCount();values.add(metric.knownSum());}return MetricAggregate.of(count,known,sumValues(values),legacy);}
    private static BigDecimal sumValues(List<BigDecimal> values){BigDecimal result=null;for(var value:values)if(value!=null)result=result==null?value:result.add(value);return result;}
    private static Average average(List<BigDecimal> values){int count=(int)values.stream().filter(Objects::nonNull).count();return new Average(count==0?null:sumValues(values).divide(BigDecimal.valueOf(count),3,RoundingMode.HALF_UP),count);}
}
