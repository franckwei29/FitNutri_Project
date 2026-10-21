package com.fitnutri.backend.records;
import com.fitnutri.backend.foodreference.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
public record FoodRecordResponse(String id,UUID clientEntryId,LocalDate recordDate,String source,long version,DataQuality dataQuality,Instant createdAt,Instant updatedAt,ReferenceInfo reference,String foodName,BigDecimal quantityGrams,MealType mealType,NutrientValues nutrients){
    static FoodRecordResponse from(FoodRecord r){return new FoodRecordResponse(r.getId().toString(),r.getClientEntryId(),r.getRecordDate(),r.getSource().canonical().name(),r.getVersion(),r.getDataQuality(),r.getCreatedAt(),r.getUpdatedAt(),r.getReference()==null?null:ReferenceInfo.from(r.getReference()),r.getFoodName(),r.getQuantityGramsSnapshot()==null?null:r.getQuantityGramsSnapshot().setScale(12,java.math.RoundingMode.UNNECESSARY),r.getMealType(),new NutrientValues(r.getEnergyKj(),r.getProteinGramsSnapshot(),r.getFatGramsSnapshot(),r.getCarbsGramsSnapshot()));}
}
