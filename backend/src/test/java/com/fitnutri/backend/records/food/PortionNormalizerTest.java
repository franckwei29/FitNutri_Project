package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PortionNormalizerTest {
 private final PortionNormalizer normalizer=new PortionNormalizer();
 @Test void fractionsPercentAndServingsAreExactReducedRationals(){
  var fraction=normalizer.normalisePortion("FULL_RECIPE",Map.of("kind","FRACTION","numerator",2,"denominator",6));assertEquals(1,fraction.numerator());assertEquals(3,fraction.denominator());assertEquals(2,fraction.input().get("numerator"));
  var percent=normalizer.normalisePortion("FULL_RECIPE",Map.of("kind","PERCENT","percent",new BigDecimal("33.3")));assertEquals(333,percent.numerator());assertEquals(1000,percent.denominator());
  var serving=normalizer.normalisePortion("FULL_RECIPE",Map.of("kind","SERVINGS","total",3,"eaten",1));assertEquals(1,serving.numerator());assertEquals(3,serving.denominator());
  var decimal=normalizer.normalisePortion("FULL_RECIPE",Map.of("kind","SERVINGS","total",4,"eaten",new BigDecimal("1.5")));assertEquals(3,decimal.numerator());assertEquals(8,decimal.denominator());
  var personal=normalizer.normalisePortion("PERSONAL",Map.of("kind","PERSONAL"));assertEquals(1,personal.numerator());assertEquals(1,personal.denominator());assertTrue(personal.input().isEmpty());
 }
 @Test void invalidMissingConflictingOrUnboundedInputsNeverBecomePersonalOne(){
  var bad=List.of(Map.of("kind","FRACTION","numerator",0,"denominator",3),Map.of("kind","FRACTION","numerator",4,"denominator",3),Map.of("kind","FRACTION","numerator",1,"denominator",0),Map.of("kind","FRACTION","numerator",1,"denominator",1000001),Map.of("kind","FRACTION","numerator",1.5,"denominator",3),Map.of("kind","PERCENT","percent",0),Map.of("kind","PERCENT","percent",100.001),Map.of("kind","PERCENT","percent",0.0001),Map.of("kind","PERCENT","percent","33.3"),Map.of("kind","SERVINGS","total",3,"eaten",4),Map.of("kind","SERVINGS","total",1.5,"eaten",1),Map.of("kind","SERVINGS","total",1001,"eaten",1),Map.of("kind","SERVINGS","total",3,"eaten",Double.NaN),Map.of("kind","FRACTION","numerator",1,"denominator",3,"percent",33.3),Map.of("kind","PERSONAL"),Map.of("kind","OTHER"));
  for(var input:bad)assertThrows(BusinessException.class,()->normalizer.normalisePortion("FULL_RECIPE",input),input.toString());
  assertThrows(BusinessException.class,()->normalizer.normalisePortion("PERSONAL",Map.of("kind","PERSONAL","percent",100)));
  assertThrows(BusinessException.class,()->normalizer.normalisePortion("PERSONAL",null));assertThrows(BusinessException.class,()->normalizer.normalisePortion("UNKNOWN",Map.of("kind","PERSONAL")));
 }
}
