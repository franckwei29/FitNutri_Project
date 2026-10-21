package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.RecordSource;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import static com.fitnutri.backend.records.food.FoodEntryInputs.*;
/** Clock/database-independent structural validation, shared by preview and pre-replay hashing. */
@Component
public class FoodEntryInputValidator {
 private final PortionNormalizer portions;
 public FoodEntryInputValidator(PortionNormalizer portions){this.portions=portions;}
 public List<UUID> validate(PreviewRequest request){
  if(request==null||request.recordDate()==null||!request.recordDate().toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")||request.zone()==null||request.zone().isBlank()||request.zone().length()>100||request.source()==null||request.source()==RecordSource.PHOTO||request.entries()==null||request.entries().isEmpty()||request.entries().size()>20)throw invalid("entries");
  var ids=new LinkedHashSet<UUID>();int leaves=0;
  for(var entry:request.entries()){
   if(entry==null||entry.clientEntryId()==null||entry.mealType()==null||!ids.add(entry.clientEntryId()))throw invalid("clientEntryId");
   if(entry instanceof Single s){referenceId(s.referenceId());grams(s.quantityGrams(),s.clientEntryId(),"quantityGrams");selection(s.selectionKind(),s.searchMetadataRevision());leaves++;}
   else if(entry instanceof Dish d){
    if(d.name()==null||d.name().isBlank()||d.name().length()>100||d.ingredients()==null||d.ingredients().isEmpty()||d.ingredients().size()>20)throw invalid("ingredients");
    portions.normalisePortion(d.inputMode(),d.portion());if(d.originDishId()!=null)referenceId(d.originDishId());
    for(var i:d.ingredients()){if(i==null||i.clientEntryId()==null||!ids.add(i.clientEntryId()))throw invalid("clientEntryId");referenceId(i.referenceId());grams(i.inputGrams(),i.clientEntryId(),"inputGrams");selection(i.selectionKind(),i.searchMetadataRevision());leaves++;}
   }
  }
  if(leaves>50)throw invalid("ingredients");return List.copyOf(ids);
 }
 public static long referenceId(String value){try{if(value==null||!value.matches("[1-9][0-9]{0,18}"))throw new NumberFormatException();return Long.parseLong(value);}catch(NumberFormatException e){throw invalid("referenceId");}}
 private static void grams(BigDecimal value,UUID id,String field){if(value==null||value.signum()<=0||value.compareTo(BigDecimal.valueOf(10000))>0||value.stripTrailingZeros().scale()>3)throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",List.of(new FieldViolation(field,id.toString(),"Enter positive grams up to 10000 with three decimal places.")));}
 private static void selection(String kind,String revision){if(kind!=null&&!Set.of("EXACT","RELATED","APPROXIMATE","USER_SELECTED").contains(kind)||revision!=null&&revision.length()>100)throw invalid("selectionKind");}
 private static BusinessException invalid(String field){return BusinessException.invalid(field,"Use complete bounded food inputs and stable unique identities.");}
}
