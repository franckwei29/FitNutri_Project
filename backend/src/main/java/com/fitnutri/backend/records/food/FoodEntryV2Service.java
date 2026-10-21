package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.ProfileService;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.submission.*;
import com.fitnutri.backend.user.User;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import static com.fitnutri.backend.records.food.FoodEntryInputs.*;

/** SubmissionService owns the single outer transaction; no external calls and no nested creation transaction. */
@Service
public class FoodEntryV2Service {
 private final SubmissionService submissions;private final CanonicalEntryHasherV2 hasher;private final ProfileService profiles;private final FoodEntryPreviewService previews;private final FoodReferenceRepository references;private final FoodRecordRepository records;private final FoodDishRepository dishes;private final PortionNormalizer portions;private final Clock clock;
 public FoodEntryV2Service(SubmissionService submissions,CanonicalEntryHasherV2 hasher,ProfileService profiles,FoodEntryPreviewService previews,FoodReferenceRepository references,FoodRecordRepository records,FoodDishRepository dishes,PortionNormalizer portions,Clock clock){this.submissions=submissions;this.hasher=hasher;this.profiles=profiles;this.previews=previews;this.references=references;this.records=records;this.dishes=dishes;this.portions=portions;this.clock=clock;}
 public SubmissionReceiptV2Response createBatchV2(Long owner,BatchRequest request){
  var input=hasher.food(request);
  return submissions.withSubmissionV2(owner,input,user->{
   profiles.requireCompletedAdultSetup(owner);
   var ids=new TreeSet<Long>();for(var entry:request.entries())if(entry instanceof Single single)ids.add(FoodEntryInputValidator.referenceId(single.referenceId()));else for(var ingredient:((Dish)entry).ingredients())ids.add(FoodEntryInputValidator.referenceId(ingredient.referenceId()));
   Map<Long,FoodReference> loaded=new HashMap<>();for(var reference:references.lockAllById(ids))loaded.put(reference.getId(),reference);
   var preview=previews.previewNewBatch(owner,request.preview());if(!preview.fingerprint().equals(request.previewFingerprint()))throw new FoodCalculationChangedV2Exception(preview);
   Map<UUID,FoodSnapshotReader.Leaf> calculated=new HashMap<>();for(var root:preview.entries())for(var leaf:root.leaves())calculated.put(leaf.clientEntryId(),leaf);
   var roots=new ArrayList<SubmissionReceiptV2Response.Root>();var leaves=new ArrayList<SubmissionReceiptV2Response.Leaf>();var now=clock.instant();
   for(var entry:request.entries()){
    if(entry instanceof Single single){var row=leaf(user,request,single.clientEntryId(),single.referenceId(),single.mealType(),single.selectionKind(),single.searchMetadataRevision(),null,null,calculated.get(single.clientEntryId()),loaded,now);records.saveAndFlush(row);roots.add(new SubmissionReceiptV2Response.Root("SINGLE",row.getId().toString(),row.getClientEntryId(),row.getRecordDate(),0));leaves.add(identity(row,single.clientEntryId()));}
    else{var dish=(Dish)entry;var ratio=portions.normalisePortion(dish.inputMode(),dish.portion());var root=new FoodDishRecord();root.setUser(user);root.setClientEntryId(dish.clientEntryId());root.setName(dish.name());root.setRecordDate(request.recordDate());root.setMealType(dish.mealType());root.setInputMode(dish.inputMode());root.setPortionInputKind(ratio.kind());root.setPortionNumerator(ratio.numerator());root.setPortionDenominator(ratio.denominator());root.setPortionInput(ratio.input());root.setSource(request.source());root.setCreatedAt(now);root.setUpdatedAt(now);
     if(dish.originDishId()!=null)root.setOrigin(dishes.findOwnedIncludingDeleted(owner,FoodEntryInputValidator.referenceId(dish.originDishId())).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Source dish not found.")));
     dishes.saveAndFlush(root);roots.add(new SubmissionReceiptV2Response.Root("DISH",root.getId().toString(),root.getClientEntryId(),root.getRecordDate(),0));
     for(int position=0;position<dish.ingredients().size();position++){var ingredient=dish.ingredients().get(position);var row=leaf(user,request,ingredient.clientEntryId(),ingredient.referenceId(),dish.mealType(),ingredient.selectionKind(),ingredient.searchMetadataRevision(),root,position,calculated.get(ingredient.clientEntryId()),loaded,now);records.saveAndFlush(row);leaves.add(identity(row,root.getClientEntryId()));}
    }
   }
   records.flush();dishes.flush();return new SubmissionReceiptV2Response.Proof(2,roots,leaves);
  });
 }
 private FoodRecord leaf(User user,BatchRequest request,UUID uuid,String id,MealType meal,String selection,String metadata,FoodDishRecord dish,Integer position,FoodSnapshotReader.Leaf calculated,Map<Long,FoodReference> references,Instant now){
  var reference=references.get(FoodEntryInputValidator.referenceId(id));if(reference==null||calculated==null)throw new IllegalStateException("Missing validated food leaf.");var row=new FoodRecord();row.setUser(user);row.setClientEntryId(uuid);row.setSource(request.source());row.setCreatedAt(now);row.setUpdatedAt(now);row.setRecordedAt(now);row.setRecordDate(request.recordDate());row.setDataQuality(DataQuality.CURRENT);row.setReference(reference);row.setFoodName(reference.getDisplayName());row.setMealType(meal);row.setQuantityGramsSnapshot(calculated.consumedGrams());row.setEnergyKj(calculated.nutrients().energyKj());row.setProteinGramsSnapshot(calculated.nutrients().proteinGrams());row.setFatGramsSnapshot(calculated.nutrients().fatGrams());row.setCarbsGramsSnapshot(calculated.nutrients().carbsGrams());row.setSelectionKind(selection==null?"USER_SELECTED":selection);row.setSearchMetadataRevision(metadata);
  if(dish!=null){row.setDish(dish);row.setDishPosition(position);row.setIngredientInputGrams(calculated.inputGrams());row.setNutritionPolicy(DishCalculationService.POLICY);}return row;
 }
 private static SubmissionReceiptV2Response.Leaf identity(FoodRecord row,UUID root){return new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT",row.getId().toString(),row.getClientEntryId(),root,row.getRecordDate(),0);}
}
