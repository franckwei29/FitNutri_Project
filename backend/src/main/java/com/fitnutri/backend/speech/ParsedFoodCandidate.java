package com.fitnutri.backend.speech;
import com.fitnutri.backend.foodreference.ReferenceInfo;
import com.fitnutri.backend.records.MealType;
import java.math.BigDecimal;
import java.util.List;
public record ParsedFoodCandidate(String candidateKey,String proposedName,BigDecimal suggestedGrams,MealType mealType,List<ReferenceInfo> referenceMatches,List<String> warnings){}
