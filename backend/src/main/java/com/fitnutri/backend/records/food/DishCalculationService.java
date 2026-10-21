package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Scales original grams with an exact ratio; only final persisted leaf values are rounded. */
@Service
public class DishCalculationService {
 public static final String POLICY="RECIPE_PORTION_RATIONAL_V1";
 private static final BigDecimal MAX=new BigDecimal("999999999.999");
 private final FoodCalculationService foods;
 public DishCalculationService(FoodCalculationService foods){this.foods=foods;}
 public record Result(BigDecimal inputGrams,BigDecimal consumedGrams,NutrientValues nutrients,String calculationPolicy){}
 /** historical is permitted only after owner + existing leaf/reference scope has been established. */
 public Result calculateScaledReference(FoodReference reference,BigDecimal inputGrams,PortionNormalizer.Portion portion,boolean historical){
  if(portion==null)throw BusinessException.invalid("portion","Choose a valid portion.");
  if(historical)foods.validateHistoricalSelection(reference,inputGrams);else foods.validateSelection(reference,inputGrams);
  var n=BigDecimal.valueOf(portion.numerator());var d=BigDecimal.valueOf(portion.denominator());var multiplier=inputGrams.multiply(n);var divisor=d.multiply(BigDecimal.valueOf(100));
  return new Result(inputGrams,multiplier.divide(d,12,RoundingMode.HALF_UP),new NutrientValues(scaled(reference.getEnergyKj100g(),multiplier,divisor),scaled(reference.getProteinG100g(),multiplier,divisor),scaled(reference.getFatG100g(),multiplier,divisor),scaled(reference.getCarbsG100g(),multiplier,divisor)),POLICY);
 }
 private static BigDecimal scaled(BigDecimal nutrient,BigDecimal multiplier,BigDecimal divisor){var value=nutrient.multiply(multiplier).divide(divisor,3,RoundingMode.HALF_UP);if(value.compareTo(MAX)>0)throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_REFERENCE","This portion exceeds the supported nutrient snapshot range.");return value;}
 public static NutrientValues sumRoundedLeaves(List<NutrientValues> leaves){return new NutrientValues(sum(leaves,NutrientValues::energyKj),sum(leaves,NutrientValues::proteinGrams),sum(leaves,NutrientValues::fatGrams),sum(leaves,NutrientValues::carbsGrams));}
 private static BigDecimal sum(List<NutrientValues> leaves,java.util.function.Function<NutrientValues,BigDecimal> field){var result=BigDecimal.ZERO.setScale(3);for(var leaf:leaves){var value=field.apply(leaf);if(value==null)return null;result=result.add(value);}return result;}
}
