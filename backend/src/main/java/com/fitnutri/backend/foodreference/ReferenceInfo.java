package com.fitnutri.backend.foodreference;
public record ReferenceInfo(String referenceId,String publicFoodKey,String displayName,DatasetResponse dataset,boolean active) {
    public static ReferenceInfo from(FoodReference f) {return new ReferenceInfo(f.getId().toString(),f.getPublicFoodKey(),f.getDisplayName(),DatasetResponse.from(f.getDataset()),f.isActive());}
}
