package com.fitnutri.backend.speech;
import com.fitnutri.backend.activity.ExerciseTypeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record ParsedEntryResponse(ParseSpeechRequest.Kind kind,LocalDate recordDate,boolean dateWasDefaulted,List<ParsedFoodCandidate> items,ParsedExercise exercise,List<String> missingFields,List<String> warnings,String requestId){
    public record ParsedExercise(String proposedName,BigDecimal durationMinutes,List<ExerciseTypeResponse> typeMatches){}
    public ParsedEntryResponse withRequestId(String id){return new ParsedEntryResponse(kind,recordDate,dateWasDefaulted,items,exercise,missingFields,warnings,id);}
}
