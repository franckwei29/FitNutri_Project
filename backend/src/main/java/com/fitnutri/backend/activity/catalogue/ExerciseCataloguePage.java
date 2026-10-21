package com.fitnutri.backend.activity.catalogue;
import java.util.List;
public record ExerciseCataloguePage<T>(List<T> results,long totalMatches,int page,int size,boolean hasMore){public ExerciseCataloguePage{results=List.copyOf(results);}}
