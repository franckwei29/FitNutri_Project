package com.fitnutri.backend.food;

import com.fitnutri.backend.foodreference.*;
import java.util.List;
import org.springframework.stereotype.Service;

/** Explainable name search only; IDs and source metadata always come from the real catalogue. */
@Service
public class FoodCandidateMatcher {
    private final FoodReferenceService references;
    public FoodCandidateMatcher(FoodReferenceService references){this.references=references;}
    public List<ReferenceInfo> match(String proposedName){
        String query=proposedName.replaceAll("[^\\p{L}\\p{N} ]"," ").strip().replaceAll("\\s+"," ");
        if(query.isBlank())return List.of();
        return references.searchActive(query,0,5).results().stream().map(row->new ReferenceInfo(row.referenceId(),row.publicFoodKey(),row.displayName(),row.dataset(),row.active())).toList();
    }
}
