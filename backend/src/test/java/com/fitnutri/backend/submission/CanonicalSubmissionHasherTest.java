package com.fitnutri.backend.submission;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class CanonicalSubmissionHasherTest {
    private final CanonicalSubmissionHasher hasher=new CanonicalSubmissionHasher(JsonMapper.builder().build(),new ZoneResolver("Australia/Sydney"));
    private final UUID submission=UUID.fromString("0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd"),a=UUID.fromString("3d849f70-7c4e-4fdf-abd0-a53c1c807dd9"),b=UUID.fromString("1845f5ac-2e84-4357-99e4-037559e2880b");
    private CanonicalSubmissionHasher.FoodItem item(UUID client,String grams) {return new CanonicalSubmissionHasher.FoodItem(client,101L,new BigDecimal(grams),MealType.LUNCH);}
    private CanonicalSubmissionHasher.Submission food(List<CanonicalSubmissionHasher.FoodItem> items){return hasher.food(submission,LocalDate.of(2026,10,16),"Australia/Sydney",RecordSource.MANUAL,items);}
    @Test void numericEquivalenceAndDisplayOrderDoNotChangeBusinessHash(){var one=food(List.of(item(a,"150.000"),item(b,"0.001")));var two=food(List.of(item(b,"0.0010"),item(a,"150")));assertEquals(one.requestSha256(),two.requestSha256());assertEquals(one.canonicalJson(),two.canonicalJson());assertTrue(one.requestSha256().matches("[a-f0-9]{64}"));}
    @Test void referenceQuantityMealSourceDateAndZoneAreAllBoundToHash(){var original=food(List.of(item(a,"150"))).requestSha256();var date=LocalDate.of(2026,10,16);assertNotEquals(original,food(List.of(item(a,"151"))).requestSha256());assertNotEquals(original,food(List.of(new CanonicalSubmissionHasher.FoodItem(a,102L,new BigDecimal("150"),MealType.LUNCH))).requestSha256());assertNotEquals(original,food(List.of(new CanonicalSubmissionHasher.FoodItem(a,101L,new BigDecimal("150"),MealType.DINNER))).requestSha256());assertNotEquals(original,hasher.food(submission,date,"Australia/Sydney",RecordSource.IMAGE,List.of(item(a,"150"))).requestSha256());assertNotEquals(original,hasher.food(submission,date.minusDays(1),"Australia/Sydney",RecordSource.MANUAL,List.of(item(a,"150"))).requestSha256());assertNotEquals(original,hasher.food(submission,date,"UTC",RecordSource.MANUAL,List.of(item(a,"150"))).requestSha256());}
    @Test void exerciseFingerprintAndMinutesAreBoundWithoutCurrentWeightOrTime(){var date=LocalDate.of(2026,10,16);var one=hasher.exercise(submission,date,"Australia/Sydney",RecordSource.MANUAL,a,11L,new BigDecimal("10.00"),"review-a");var same=hasher.exercise(submission,date,"Australia/Sydney",RecordSource.MANUAL,a,11L,new BigDecimal("10"),"review-a");assertEquals(one.requestSha256(),same.requestSha256());assertNotEquals(one.requestSha256(),hasher.exercise(submission,date,"Australia/Sydney",RecordSource.MANUAL,a,11L,new BigDecimal("10"),"review-b").requestSha256());assertNotEquals(one.requestSha256(),food(List.of(item(a,"10"))).requestSha256());}
    @Test void malformedBasicShapeFailsWithoutInventingDefaults(){assertThrows(BusinessException.class,()->food(List.of()));assertThrows(BusinessException.class,()->food(Collections.singletonList(null)));assertThrows(BusinessException.class,()->hasher.food(submission,LocalDate.of(2026,10,16),"invalid",RecordSource.MANUAL,List.of(item(a,"1"))));assertThrows(BusinessException.class,()->hasher.food(submission,LocalDate.of(2026,10,16),"Australia/Sydney",RecordSource.PHOTO,List.of(item(a,"1"))));assertThrows(BusinessException.class,()->food(List.of(new CanonicalSubmissionHasher.FoodItem(a,null,BigDecimal.ONE,MealType.LUNCH))));}
    @Test void dateBusinessDependenciesAreNotRevalidatedByCanonicalHash(){assertNotNull(hasher.food(submission,LocalDate.of(2099,1,1),"Australia/Sydney",RecordSource.MANUAL,List.of(item(a,"1"))));}
    @Test void independentSharedFixtureHasExactCanonicalBytesAndExpectedDigest() throws Exception {
        var json=JsonMapper.builder().build();var fixture=json.readTree(Files.readAllBytes(Path.of("..","docs","api","examples","submission-canonicalization.json")));var request=fixture.get("food").get("request");var items=new ArrayList<CanonicalSubmissionHasher.FoodItem>();for(var i:request.get("items"))items.add(new CanonicalSubmissionHasher.FoodItem(UUID.fromString(i.get("clientEntryId").asText()),Long.valueOf(i.get("referenceId").asText()),i.get("quantityGrams").decimalValue(),MealType.valueOf(i.get("mealType").asText())));var result=hasher.food(UUID.fromString(request.get("submissionId").asText()),LocalDate.parse(request.get("recordDate").asText()),request.get("zone").asText(),RecordSource.valueOf(request.get("source").asText()),items);assertEquals(fixture.get("food").get("canonicalJson").asText(),result.canonicalJson());assertEquals(fixture.get("food").get("sha256").asText(),result.requestSha256());
    }
}
