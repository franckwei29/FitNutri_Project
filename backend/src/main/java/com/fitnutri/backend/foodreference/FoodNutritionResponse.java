package com.fitnutri.backend.foodreference;
import java.math.BigDecimal;
public record FoodNutritionResponse(ReferenceInfo reference,BigDecimal quantityGrams,NutrientValues nutrients,String calculationFingerprint) {}
