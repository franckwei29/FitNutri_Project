package com.fitnutri.backend.speech;

import com.fitnutri.backend.activity.ExerciseTypeResponse;
import com.fitnutri.backend.food.FoodCandidateMatcher;
import com.fitnutri.backend.foodreference.ReferenceInfo;
import com.fitnutri.backend.records.ExerciseCatalog;
import java.util.*;
import org.springframework.stereotype.Service;

/** Server directories supply IDs/source values; model text never becomes an ID or a MET. */
@Service
public class CandidateMatcher {
    private final FoodCandidateMatcher foods;private final ExerciseCatalog activities;
    public CandidateMatcher(FoodCandidateMatcher foods,ExerciseCatalog activities){this.foods=foods;this.activities=activities;}
    public List<ReferenceInfo> food(String name){return foods.match(name);}
    public List<ExerciseTypeResponse> exercise(String name){
        if(name==null)return List.of();var query=words(name);if(query.isEmpty())return List.of();
        return activities.listActiveTypes().stream().filter(t->t.active()&&t.dataset().verified()).filter(t->words(t.displayName()+" "+t.sourceDescription()).containsAll(query)).limit(5).toList();
    }
    private Set<String> words(String text){var result=new HashSet<String>();for(String word:text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")){if(word.isBlank()||Set.of("for","a","an","the","of","at","i","did","went").contains(word))continue;result.add(switch(word){case "walking","walked"->"walk";case "running","ran"->"run";case "cycling","cycled","bicycling","bike","biking","biked"->"cycle";case "swimming","swam"->"swim";case "jogging","jogged"->"jog";default->word;});}return result;}
}
