package com.fitnutri.backend.food;

import java.util.List;

public record FoodRecognitionResponse(List<FoodCandidate> candidates,List<String> notices,String requestId) {
    public FoodRecognitionResponse {candidates=List.copyOf(candidates);notices=List.copyOf(notices);}
}
