package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.records.*;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.fitnutri.backend.records.food.FoodEntryInputs.*;

@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class FoodEntryPreviewService {
 public static final String POLICY="FOOD_ENTRY_V2_REFERENCE_RATIONAL_V1";
 private final FoodReferenceRepository references;private final FoodCalculationService foods;private final DishCalculationService dishes;private final PortionNormalizer portions;private final RecordDatePolicy dates;private final FoodSnapshotReader snapshots;private final JdbcTemplate jdbc;private final CanonicalV2Json canonical;private final FoodEntryInputValidator validator;
 public FoodEntryPreviewService(FoodReferenceRepository references,FoodCalculationService foods,DishCalculationService dishes,PortionNormalizer portions,RecordDatePolicy dates,FoodSnapshotReader snapshots,JdbcTemplate jdbc,CanonicalV2Json canonical,FoodEntryInputValidator validator){this.references=references;this.foods=foods;this.dishes=dishes;this.portions=portions;this.dates=dates;this.snapshots=snapshots;this.jdbc=jdbc;this.canonical=canonical;this.validator=validator;}
 public record Response(List<FoodSnapshotReader.Preview> entries,NutrientValues totals,String fingerprint,String calculationPolicy,String inputSignature){}
 public Response previewNewBatch(Long owner,PreviewRequest request){return preview(owner,request,null);}
 public Response previewOwnedRoot(Long owner,String kind,long id,CorrectionRequest request){
  var old=snapshots.detailForReuse(owner,kind,id);
  if(request==null||request.expectedVersion()==null||request.expectedVersion()<0)throw invalid("expectedVersion");
  if(request.expectedVersion()!=old.version())throw new BusinessException(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and review the current food entry.");
  if(request.entry()==null||!Objects.equals(request.entry().clientEntryId(),old.input().get("clientEntryId"))||!(kind.equals("single")&&request.entry() instanceof Single||kind.equals("dish")&&request.entry() instanceof Dish))throw invalid("entry");
  if(request.entry() instanceof Dish dish&&!Objects.equals(dish.originDishId(),old.input().get("originDishId")))throw invalid("originDishId");
  return preview(owner,new PreviewRequest(request.recordDate(),request.zone(),RecordSource.valueOf(old.source()),List.of(request.entry())),old);
 }
 /** Structural validation is clock/database independent and is also used before canonical idempotency lookup. */
 public List<UUID> validateInputs(PreviewRequest request){return validator.validate(request);}
 private Response preview(Long owner,PreviewRequest request,FoodSnapshotReader.Root original){
  validateInputs(request);dates.requireDate(request.recordDate(),request.zone());
  var referenceIds=new TreeSet<Long>();for(var entry:request.entries())if(entry instanceof Single s)referenceIds.add(referenceId(s.referenceId()));else for(var i:((Dish)entry).ingredients())referenceIds.add(referenceId(i.referenceId()));
  Map<Long,FoodReference> loaded=new HashMap<>();for(var f:references.findAllWithDatasetById(referenceIds))loaded.put(f.getId(),f);
  Map<UUID,FoodSnapshotReader.Leaf> oldLeaves=new HashMap<>();if(original!=null)for(var leaf:original.preview().leaves())oldLeaves.put(leaf.clientEntryId(),leaf);
  PortionNormalizer.Portion oldPortion=original!=null&&original.input().get("kind").equals("DISH")?portions.normalisePortion((String)original.input().get("inputMode"),castPortion(original.input().get("portion"))):null;
  var previews=new ArrayList<FoodSnapshotReader.Preview>();var calculationInputs=new ArrayList<Object>();var allNutrients=new ArrayList<NutrientValues>();
  for(var entry:request.entries()){
   var leaves=new ArrayList<FoodSnapshotReader.Leaf>();String kind=entry instanceof Single?"SINGLE":"DISH";
   if(entry instanceof Single s){var leaf=calculate(s.clientEntryId(),s.referenceId(),s.quantityGrams(),null,loaded,oldLeaves,null,calculationInputs);leaves.add(leaf);}
   else {var d=(Dish)entry;if(d.originDishId()!=null&&original==null&&jdbc.queryForObject("SELECT count(*) FROM food_dish_record WHERE user_id=? AND id=?",Long.class,owner,referenceId(d.originDishId()))!=1)throw new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Source dish not found.");var portion=portions.normalisePortion(d.inputMode(),d.portion());for(var ingredient:d.ingredients())leaves.add(calculate(ingredient.clientEntryId(),ingredient.referenceId(),ingredient.inputGrams(),portion,loaded,oldLeaves,oldPortion,calculationInputs));}
   var nutrients=leaves.stream().map(FoodSnapshotReader.Leaf::nutrients).toList();allNutrients.addAll(nutrients);previews.add(new FoodSnapshotReader.Preview(kind,entry.clientEntryId(),List.copyOf(leaves),DishCalculationService.sumRoundedLeaves(nutrients)));
  }
  return new Response(List.copyOf(previews),DishCalculationService.sumRoundedLeaves(allNutrients),canonical.hash("food-calculation-v2",calculationInputs),POLICY,canonical.hash("food-preview-input-v2",request));
 }
 private FoodSnapshotReader.Leaf calculate(UUID uuid,String id,BigDecimal grams,PortionNormalizer.Portion portion,Map<Long,FoodReference> loaded,Map<UUID,FoodSnapshotReader.Leaf> oldLeaves,PortionNormalizer.Portion oldPortion,List<Object> calculationInputs){
  try{
   var reference=loaded.get(referenceId(id));if(reference==null)throw new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Food reference not found.");var old=oldLeaves.get(uuid);boolean historical=old!=null&&id.equals(old.referenceId());
   if(historical)foods.validateHistoricalSelection(reference,grams);else foods.validateSelection(reference,grams);
   boolean sameRatio=portion==null?oldPortion==null:oldPortion!=null&&portion.numerator()==oldPortion.numerator()&&portion.denominator()==oldPortion.denominator();
   var key=new LinkedHashMap<String,Object>();key.put("clientEntryId",uuid);key.put("referenceFingerprint",foods.fingerprint(reference,grams));key.put("policy",portion==null?"REFERENCE_GRAMS_V1":DishCalculationService.POLICY);key.put("numerator",portion==null?1:portion.numerator());key.put("denominator",portion==null?1:portion.denominator());calculationInputs.add(key);
   if(historical&&sameRatio&&old.inputGrams().compareTo(grams)==0)return old;
   if(portion==null)return new FoodSnapshotReader.Leaf(uuid,id,grams,grams.setScale(12,RoundingMode.UNNECESSARY),historical?foods.calculateHistorical(reference,grams):foods.calculate(reference,grams));
   var result=dishes.calculateScaledReference(reference,grams,portion,historical);return new FoodSnapshotReader.Leaf(uuid,id,grams,result.consumedGrams(),result.nutrients());
  }catch(BusinessException e){var fields=e.fields().isEmpty()?List.of(new FieldViolation("referenceId",uuid.toString(),"Review this food reference.")):e.fields().stream().map(f->new FieldViolation(f.field().equals("grams")?(portion==null?"quantityGrams":"inputGrams"):f.field(),uuid.toString(),f.message())).toList();throw new BusinessException(e.status(),e.code(),e.getMessage(),fields);}
 }
 @SuppressWarnings("unchecked") private static Map<String,Object> castPortion(Object value){return (Map<String,Object>)value;}
 public static long referenceId(String value){return FoodEntryInputValidator.referenceId(value);}
 private static BusinessException invalid(String field){return BusinessException.invalid(field,"Use complete bounded food inputs and stable unique identities.");}
}
