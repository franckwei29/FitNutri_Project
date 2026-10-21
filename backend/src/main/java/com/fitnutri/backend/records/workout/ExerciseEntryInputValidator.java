package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.records.food.FoodEntryInputValidator;
import java.math.BigDecimal;
import java.util.*;
import static com.fitnutri.backend.records.workout.ExerciseEntryInputs.*;
/** Pure input bounds only: this cannot grant an option/movement/reference permission or a valid estimate. */
public final class ExerciseEntryInputValidator {
 private ExerciseEntryInputValidator(){}
 public static List<UUID> validate(Entry entry,boolean confirmed){
  if(entry==null||entry.clientEntryId()==null)throw invalid();var ids=new LinkedHashSet<UUID>();ids.add(entry.clientEntryId());List<Period> periods;
  if(entry instanceof Quick q){if(q.period()==null||!entry.clientEntryId().equals(q.period().clientEntryId()))throw invalid();periods=List.of(q.period());}
  else{var workout=(Workout)entry;if(workout.title()!=null&&(workout.title().isBlank()||workout.title().length()>100))throw invalid();if(workout.originWorkoutId()!=null)FoodEntryInputValidator.referenceId(workout.originWorkoutId());periods=workout.periods();}
  if(periods==null||periods.isEmpty()||periods.size()>12)throw invalid();int movements=0;var total=BigDecimal.ZERO;
  for(var p:periods){try{if(p==null||p.clientEntryId()==null||entry instanceof Workout&&!ids.add(p.clientEntryId())||!Set.of("STRENGTH","ACTIVITY").contains(p.periodKind()==null?"":p.periodKind())||confirmed&&!hash(p.calculationFingerprint()))throw invalid();FoodEntryInputValidator.referenceId(p.optionId());decimal(p.durationMinutes(),BigDecimal.ZERO,BigDecimal.valueOf(1440),false);total=total.add(p.durationMinutes());
   if(p.equipmentDetails()==null||p.movements()==null||p.movements().size()>30||p.periodKind().equals("ACTIVITY")&&!p.movements().isEmpty())throw invalid();
   validateEquipment(p.equipmentDetails());
   movements+=p.movements().size();for(var movement:p.movements()){if(movement==null)throw invalid();FoodEntryInputValidator.referenceId(movement.catalogueId());if(movement.setCount()!=null&&(movement.setCount()<1||movement.setCount()>100)||movement.repetitions()!=null&&(movement.repetitions()<1||movement.repetitions()>1000))throw invalid();if(movement.loadKg()!=null)decimal(movement.loadKg(),BigDecimal.ZERO,BigDecimal.valueOf(1000),true);}
  }catch(BusinessException e){var fields=e.fields().stream().map(f->new com.fitnutri.backend.common.FieldViolation(f.field().equals("entry")?"period":f.field(),p==null||p.clientEntryId()==null?null:p.clientEntryId().toString(),f.message())).toList();throw new BusinessException(e.status(),e.code(),e.getMessage(),fields);}}
  if(total.compareTo(BigDecimal.valueOf(1440))>0||movements>100)throw invalid();return List.copyOf(ids);
 }
 public static Map<String,Object> validateEquipment(Map<String,Object> details){if(details==null)throw invalid();var normalised=new LinkedHashMap<String,Object>();
  for(var field:details.entrySet()){var key=field.getKey();if(!Set.of("speedKmh","inclinePercent","distanceMetres","machineLevel").contains(key))throw invalid();if(field.getValue()==null)continue;if(!(field.getValue() instanceof Number n))throw invalid();BigDecimal value;try{value=new BigDecimal(n.toString());}catch(NumberFormatException e){throw invalid();}normalised.put(key,value);switch(key){case "speedKmh"->decimal(value,BigDecimal.ZERO,BigDecimal.valueOf(100),false);case "inclinePercent"->decimal(value,BigDecimal.valueOf(-20),BigDecimal.valueOf(40),true);case "distanceMetres"->decimal(value,BigDecimal.ZERO,BigDecimal.valueOf(1000000),false);default->{decimal(value,BigDecimal.ZERO,BigDecimal.valueOf(100),true);if(value.stripTrailingZeros().scale()>0)throw invalid();}}}
  return Collections.unmodifiableMap(normalised);
 }
 public static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
 private static void decimal(BigDecimal value,BigDecimal min,BigDecimal max,boolean inclusive){if(value==null||value.compareTo(min)<0||!inclusive&&value.compareTo(min)==0||value.compareTo(max)>0||value.stripTrailingZeros().scale()>2)throw invalid();}
 private static BusinessException invalid(){return BusinessException.invalid("entry","Use complete bounded workout periods and valid optional details with stable identities.");}
}
