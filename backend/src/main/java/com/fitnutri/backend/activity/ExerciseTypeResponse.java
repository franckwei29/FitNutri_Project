package com.fitnutri.backend.activity;
import java.math.BigDecimal;
public record ExerciseTypeResponse(String id,String activityKey,String displayName,String sourceActivityCode,String sourceDescription,BigDecimal metValue,Dataset dataset,boolean active) {
    public record Dataset(String id,String name,String release,String revision,String sourceUrl,String nutrientPolicy,boolean verified,Integer minAge,Integer maxAge) {}
    public static ExerciseTypeResponse from(ExerciseType t){var d=t.getDataset();return new ExerciseTypeResponse(t.getId().toString(),t.getActivityKey(),t.getDisplayName(),t.getSourceActivityCode(),t.getSourceDescription(),t.getMetValue(),new Dataset(d.getId().toString(),d.getName(),d.getRelease(),d.getRevision(),d.getSourceUrl(),"MET_REFERENCE",d.isVerified(),d.getMinAge(),d.getMaxAge()),t.isActive());}
}
