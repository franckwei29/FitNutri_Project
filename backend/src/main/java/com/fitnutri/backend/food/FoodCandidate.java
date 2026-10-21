package com.fitnutri.backend.food;

import com.fitnutri.backend.foodreference.ReferenceInfo;
import java.math.BigDecimal;
import java.util.List;

/** Pending suggestion only. No record identity, confirmed meal or authoritative nutrients. */
public record FoodCandidate(String candidateKey,String proposedName,BigDecimal suggestedGrams,List<ReferenceInfo> referenceMatches,List<String> warnings) {
    public FoodCandidate {referenceMatches=List.copyOf(referenceMatches);warnings=List.copyOf(warnings);}
}
