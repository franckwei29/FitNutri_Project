package com.fitnutri.backend.foodreference;
public record DatasetResponse(String id,String name,String release,String revision,String sourceUrl,String nutrientPolicy,boolean verified) {
    public static DatasetResponse from(FoodDataset d) {return new DatasetResponse(d.getId().toString(),d.getName(),d.getRelease(),d.getRevision(),d.getSourceUrl(),d.getNutrientPolicy(),d.isVerified());}
}
