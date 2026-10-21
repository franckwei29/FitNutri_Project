package com.fitnutri.backend.submission;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.records.food.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TypedCreationProofV2Test {
 final UUID single=UUID.randomUUID(),dish=UUID.randomUUID(),ingredient=UUID.randomUUID();final LocalDate date=LocalDate.of(2020,1,1);
 CanonicalEntryHasherV2.Submission input(){var json=new tools.jackson.databind.ObjectMapper();var hasher=new CanonicalEntryHasherV2(new CanonicalV2Json(json),json,new ZoneResolver("Australia/Sydney"),new FoodEntryInputValidator(new PortionNormalizer()));return hasher.food(new FoodEntryInputs.BatchRequest(UUID.randomUUID(),date,"Australia/Sydney",RecordSource.MANUAL,List.of(new FoodEntryInputs.Single(single,"101",BigDecimal.ONE,MealType.LUNCH,null,null),new FoodEntryInputs.Dish(dish,"Synthetic recipe",MealType.LUNCH,"PERSONAL",Map.of("kind","PERSONAL"),List.of(new FoodEntryInputs.Ingredient(ingredient,"101",BigDecimal.ONE,null,null)),null)),"f".repeat(64)));}
 SubmissionReceiptV2Response.Proof proof(){return new SubmissionReceiptV2Response.Proof(2,List.of(new SubmissionReceiptV2Response.Root("SINGLE","1",single,date,0),new SubmissionReceiptV2Response.Root("DISH","1",dish,date,0)),List.of(new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","1",single,single,date,0),new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","2",ingredient,dish,date,0)));}
 @Test void numericIdsMayCoincideAcrossPhysicalTablesButEverySubmittedIdentityIsProved(){assertDoesNotThrow(()->SubmissionReceiptV2Response.validateTypedProof(input(),proof()));}
 @Test void missingExtraWrongParentDateKindVersionAndDuplicateIdsNeverProveSuccess(){var input=input();var original=proof();var badLeaves=List.of(new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","2",ingredient,single,date,0),new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","2",ingredient,dish,date.plusDays(1),0),new SubmissionReceiptV2Response.Leaf("ACTIVITY_PERIOD","2",ingredient,dish,date,0),new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","2",ingredient,dish,date,1),new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","1",ingredient,dish,date,0),new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","2",UUID.randomUUID(),dish,date,0));for(var bad:badLeaves)assertThrows(IllegalStateException.class,()->SubmissionReceiptV2Response.validateTypedProof(input,new SubmissionReceiptV2Response.Proof(2,original.roots(),List.of(original.leaves().getFirst(),bad))));assertThrows(IllegalStateException.class,()->SubmissionReceiptV2Response.validateTypedProof(input,new SubmissionReceiptV2Response.Proof(2,original.roots(),List.of(original.leaves().getFirst()))));assertThrows(IllegalStateException.class,()->SubmissionReceiptV2Response.validateTypedProof(input,new SubmissionReceiptV2Response.Proof(1,original.roots(),original.leaves())));}
}
