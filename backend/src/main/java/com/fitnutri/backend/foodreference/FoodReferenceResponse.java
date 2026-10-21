package com.fitnutri.backend.foodreference;
public record FoodReferenceResponse(String referenceId,String publicFoodKey,String displayName,DatasetResponse dataset,boolean active,NutrientValues per100g) {
    public static FoodReferenceResponse from(FoodReference f) {return new FoodReferenceResponse(f.getId().toString(),f.getPublicFoodKey(),f.getDisplayName(),DatasetResponse.from(f.getDataset()),f.isActive(),NutrientValues.per100g(f));}
}
