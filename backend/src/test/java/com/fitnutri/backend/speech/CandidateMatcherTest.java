package com.fitnutri.backend.speech;

import com.fitnutri.backend.activity.ExerciseTypeResponse;
import com.fitnutri.backend.food.FoodCandidateMatcher;
import com.fitnutri.backend.records.ExerciseCatalog;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CandidateMatcherTest {
    final ExerciseCatalog catalogue=mock(ExerciseCatalog.class);final CandidateMatcher matcher=new CandidateMatcher(mock(FoodCandidateMatcher.class),catalogue);
    ExerciseTypeResponse type(String id,String name,boolean active,boolean verified){return new ExerciseTypeResponse(id,"synthetic-"+id,name,"99901","Synthetic source "+name,new BigDecimal("3.5"),new ExerciseTypeResponse.Dataset("1","SYNTHETIC_TEST_ONLY","test","1",null,"MET_REFERENCE",verified,19,59),active);}
    @Test void explainsCommonSpokenVerbMatchesAndExcludesInactiveUnverifiedOrUnrelatedTypes(){when(catalogue.listActiveTypes()).thenReturn(List.of(type("1","Walking for pleasure",true,true),type("2","Walking slowly",false,true),type("3","Walking",true,false),type("4","Running",true,true)));assertEquals(List.of("1"),matcher.exercise("walked").stream().map(ExerciseTypeResponse::id).toList());assertTrue(matcher.exercise("NoSuchActivityZZZZ").isEmpty());}
    @Test void boundsAmbiguousCandidatesWithoutInventingASelectionAndMissingNameDoesNotReadCatalogue(){assertTrue(matcher.exercise(null).isEmpty());verifyNoInteractions(catalogue);when(catalogue.listActiveTypes()).thenReturn(IntStream.rangeClosed(1,8).mapToObj(i->type(String.valueOf(i),"Cycling "+i,true,true)).toList());assertEquals(5,matcher.exercise("biked").size());}
}
