package com.fitnutri.backend.submission;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.RecordSource;
import com.fitnutri.backend.records.food.*;
import com.fitnutri.backend.records.workout.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Pure V2 canonical input hashing. No source, profile, date-clock or reference lookup before receipt replay. */
@Component
public class CanonicalEntryHasherV2 {
 private final CanonicalV2Json canonical;private final ZoneResolver zones;private final FoodEntryInputValidator foodValidator;
 public CanonicalEntryHasherV2(CanonicalV2Json canonical,ObjectMapper json,ZoneResolver zones,FoodEntryInputValidator foodValidator){this.canonical=canonical;this.zones=zones;this.foodValidator=foodValidator;}
 public record ExpectedRoot(String kind,UUID clientEntryId,List<UUID> leaves){public ExpectedRoot{leaves=List.copyOf(leaves);}}
 public record Submission(UUID submissionId,SubmissionOperation operation,LocalDate recordDate,String zone,String requestSha256,String canonicalJson,List<ExpectedRoot> roots,List<UUID> allEntryIds){public Submission{roots=List.copyOf(roots);allEntryIds=List.copyOf(allEntryIds);}}
 public Submission food(FoodEntryInputs.BatchRequest request){
  if(request==null)throw invalid();var ids=foodValidator.validate(request.preview());var body=base(request.submissionId(),SubmissionOperation.FOOD_BATCH_V2,request.recordDate(),request.zone(),request.source(),request.previewFingerprint());var rows=new ArrayList<Map<String,Object>>();var roots=new ArrayList<ExpectedRoot>();
  for(var entry:request.entries()){
   var row=new LinkedHashMap<String,Object>();row.put("clientEntryId",entry.clientEntryId());row.put("mealType",entry.mealType());
   if(entry instanceof FoodEntryInputs.Single s){row.put("kind","SINGLE");row.put("referenceId",s.referenceId());row.put("quantityGrams",s.quantityGrams());selection(row,s.selectionKind(),s.searchMetadataRevision());roots.add(new ExpectedRoot("SINGLE",s.clientEntryId(),List.of(s.clientEntryId())));}
   else{var d=(FoodEntryInputs.Dish)entry;row.put("kind","DISH");row.put("name",d.name());row.put("inputMode",d.inputMode());row.put("portion",d.portion());row.put("originDishId",d.originDishId());var ingredients=new ArrayList<Map<String,Object>>();for(var i:d.ingredients()){var ingredient=new LinkedHashMap<String,Object>();ingredient.put("clientEntryId",i.clientEntryId());ingredient.put("referenceId",i.referenceId());ingredient.put("inputGrams",i.inputGrams());selection(ingredient,i.selectionKind(),i.searchMetadataRevision());ingredients.add(ingredient);}row.put("ingredients",ingredients);roots.add(new ExpectedRoot("DISH",d.clientEntryId(),d.ingredients().stream().map(FoodEntryInputs.Ingredient::clientEntryId).toList()));}
   rows.add(row);
  }
  body.put("entries",rows);return finish(request.submissionId(),SubmissionOperation.FOOD_BATCH_V2,request.recordDate(),body,roots,ids);
 }
 public Submission exercise(ExerciseEntryInputs.CreateRequest request){
  if(request==null)throw invalid();var ids=ExerciseEntryInputValidator.validate(request.entry(),true);var body=base(request.submissionId(),SubmissionOperation.EXERCISE_ENTRY_V2,request.recordDate(),request.zone(),request.source(),request.previewFingerprint());var entry=request.entry();var row=new LinkedHashMap<String,Object>();row.put("clientEntryId",entry.clientEntryId());ExpectedRoot root;
  if(entry instanceof ExerciseEntryInputs.Quick q){row.put("kind","QUICK");row.put("period",period(q.period()));root=new ExpectedRoot("QUICK",entry.clientEntryId(),List.of(entry.clientEntryId()));}
  else{var w=(ExerciseEntryInputs.Workout)entry;row.put("kind","WORKOUT");row.put("title",w.title()==null?"Gym workout":w.title());row.put("originWorkoutId",w.originWorkoutId());row.put("periods",w.periods().stream().map(CanonicalEntryHasherV2::period).toList());root=new ExpectedRoot("WORKOUT",entry.clientEntryId(),w.periods().stream().map(ExerciseEntryInputs.Period::clientEntryId).toList());}
  body.put("entry",row);return finish(request.submissionId(),SubmissionOperation.EXERCISE_ENTRY_V2,request.recordDate(),body,List.of(root),ids);
 }
 private static Map<String,Object> period(ExerciseEntryInputs.Period p){var row=new LinkedHashMap<String,Object>();row.put("clientEntryId",p.clientEntryId());row.put("periodKind",p.periodKind());row.put("optionId",p.optionId());row.put("durationMinutes",p.durationMinutes());row.put("calculationFingerprint",p.calculationFingerprint());var details=new TreeMap<String,Object>();p.equipmentDetails().forEach((k,v)->{if(v!=null)details.put(k,v);});row.put("equipmentDetails",details);var movements=new ArrayList<Map<String,Object>>();for(var m:p.movements()){var value=new LinkedHashMap<String,Object>();value.put("catalogueId",m.catalogueId());value.put("setCount",m.setCount());value.put("repetitions",m.repetitions());value.put("loadKg",m.loadKg());movements.add(value);}row.put("movements",movements);return row;}
 private LinkedHashMap<String,Object> base(UUID id,SubmissionOperation operation,LocalDate date,String zone,RecordSource source,String fingerprint){if(id==null||date==null||!date.toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")||zone==null||zone.isBlank()||zone.length()>100||source==null||source==RecordSource.PHOTO||!ExerciseEntryInputValidator.hash(fingerprint))throw invalid();var body=new LinkedHashMap<String,Object>();body.put("schemaVersion",2);body.put("operation",operation);body.put("submissionId",id);body.put("recordDate",date);body.put("zone",zones.resolve(zone).getId());body.put("source",source);body.put("previewFingerprint",fingerprint);return body;}
 private Submission finish(UUID id,SubmissionOperation operation,LocalDate date,Map<String,Object> body,List<ExpectedRoot> roots,List<UUID> ids){return new Submission(id,operation,date,(String)body.get("zone"),canonical.hash("submission-v2",body),canonical.canonical(body),roots,ids);}
 private static void selection(Map<String,Object> row,String kind,String revision){row.put("selectionKind",kind==null?"USER_SELECTED":kind);row.put("searchMetadataRevision",revision);}
 private static BusinessException invalid(){return BusinessException.invalid("request","Use a complete reviewed V2 submission and matching server preview.");}
}
