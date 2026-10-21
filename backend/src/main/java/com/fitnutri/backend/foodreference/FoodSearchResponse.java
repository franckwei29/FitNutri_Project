package com.fitnutri.backend.foodreference;
import java.util.List;
public record FoodSearchResponse(List<FoodReferenceResponse> results,long totalMatches,int page,int size,boolean hasMore) {}
