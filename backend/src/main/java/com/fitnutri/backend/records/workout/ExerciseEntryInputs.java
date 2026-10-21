package com.fitnutri.backend.records.workout;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.RecordSource;
import com.fitnutri.backend.records.food.FoodEntryInputs.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Strict confirmed exercise DTOs; catalogue/dated-estimate authority is resolved by B11/B12. */
public final class ExerciseEntryInputs {
 private ExerciseEntryInputs(){}
 @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,property="kind") @JsonSubTypes({@JsonSubTypes.Type(value=Quick.class,name="QUICK"),@JsonSubTypes.Type(value=Workout.class,name="WORKOUT")})
 public sealed interface Entry permits Quick,Workout{UUID clientEntryId();}
 public record Quick(UUID clientEntryId,Period period) implements Entry{@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record Workout(UUID clientEntryId,@JsonDeserialize(using=TextDeserializer.class) String title,List<Period> periods,@JsonDeserialize(using=StrictIdDeserializer.class) String originWorkoutId) implements Entry{public Workout{if(periods!=null)periods=Collections.unmodifiableList(new ArrayList<>(periods));}@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record Period(UUID clientEntryId,@JsonDeserialize(using=TextDeserializer.class) String periodKind,@JsonDeserialize(using=StrictIdDeserializer.class) String optionId,@JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal durationMinutes,Map<String,Object> equipmentDetails,List<Movement> movements,@JsonDeserialize(using=TextDeserializer.class) String calculationFingerprint){public Period{if(equipmentDetails!=null)equipmentDetails=Collections.unmodifiableMap(new LinkedHashMap<>(equipmentDetails));if(movements!=null)movements=Collections.unmodifiableList(new ArrayList<>(movements));}@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record Movement(@JsonDeserialize(using=StrictIdDeserializer.class) String catalogueId,@JsonDeserialize(using=StrictVersionDeserializer.class) Long setCount,@JsonDeserialize(using=StrictVersionDeserializer.class) Long repetitions,@JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal loadKg){@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record CreateRequest(UUID submissionId,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,@JsonDeserialize(using=SourceDeserializer.class) RecordSource source,@JsonDeserialize(using=TextDeserializer.class) String previewFingerprint,Entry entry){@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,property="kind") @JsonSubTypes({@JsonSubTypes.Type(value=QuickPreview.class,name="QUICK"),@JsonSubTypes.Type(value=WorkoutPreview.class,name="WORKOUT")})
 public sealed interface PreviewEntry permits QuickPreview,WorkoutPreview{UUID clientEntryId();}
 public record QuickPreview(UUID clientEntryId,PreviewPeriod period) implements PreviewEntry{@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record WorkoutPreview(UUID clientEntryId,@JsonDeserialize(using=TextDeserializer.class) String title,List<PreviewPeriod> periods,@JsonDeserialize(using=StrictIdDeserializer.class) String originWorkoutId) implements PreviewEntry{public WorkoutPreview{if(periods!=null)periods=Collections.unmodifiableList(new ArrayList<>(periods));}@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record PreviewPeriod(UUID clientEntryId,@JsonDeserialize(using=TextDeserializer.class) String periodKind,@JsonDeserialize(using=StrictIdDeserializer.class) String optionId,@JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal durationMinutes,Map<String,Object> equipmentDetails,List<Movement> movements){public PreviewPeriod{if(equipmentDetails!=null)equipmentDetails=Collections.unmodifiableMap(new LinkedHashMap<>(equipmentDetails));if(movements!=null)movements=Collections.unmodifiableList(new ArrayList<>(movements));}@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record PreviewRequest(LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,@JsonDeserialize(using=SourceDeserializer.class) RecordSource source,PreviewEntry entry){@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public static Entry withoutFingerprints(PreviewEntry entry){if(entry instanceof QuickPreview q)return new Quick(q.clientEntryId(),confirmed(q.period()));if(entry instanceof WorkoutPreview w)return new Workout(w.clientEntryId(),w.title(),w.periods()==null?null:w.periods().stream().map(ExerciseEntryInputs::confirmed).toList(),w.originWorkoutId());return null;}
 private static Period confirmed(PreviewPeriod p){return p==null?null:new Period(p.clientEntryId(),p.periodKind(),p.optionId(),p.durationMinutes(),p.equipmentDetails(),p.movements(),null);}
 public static PreviewEntry previewOf(Entry entry){if(entry instanceof Quick q)return new QuickPreview(q.clientEntryId(),preview(q.period()));var w=(Workout)entry;return new WorkoutPreview(w.clientEntryId(),w.title(),w.periods().stream().map(ExerciseEntryInputs::preview).toList(),w.originWorkoutId());}
 private static PreviewPeriod preview(Period p){return new PreviewPeriod(p.clientEntryId(),p.periodKind(),p.optionId(),p.durationMinutes(),p.equipmentDetails(),p.movements());}
 public record CorrectionRequest(@JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,Entry entry){@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 public record UpdateRequest(@JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,LocalDate recordDate,@JsonDeserialize(using=TextDeserializer.class) String zone,Entry entry,@JsonDeserialize(using=TextDeserializer.class) String previewFingerprint){@JsonAnySetter public void reject(String n,Object v){throw unknown();}}
 private static IllegalArgumentException unknown(){return new IllegalArgumentException("Unknown exercise submission input");}
}
