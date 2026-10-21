package com.fitnutri.backend.foodreference;
import java.math.BigDecimal;
public record NutrientValues(BigDecimal energyKj,BigDecimal proteinGrams,BigDecimal fatGrams,BigDecimal carbsGrams) {
    public static NutrientValues per100g(FoodReference f) {return new NutrientValues(f.getEnergyKj100g(),f.getProteinG100g(),f.getFatG100g(),f.getCarbsG100g());}
}
