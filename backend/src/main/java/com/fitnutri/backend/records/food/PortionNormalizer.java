package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Exact input-shape validation and reduced rational conversion; no percent approximation of a fraction. */
@Component
public class PortionNormalizer {
 public record Portion(String kind,long numerator,long denominator,Map<String,Object> input){
  public Portion{if(numerator<1||denominator<1||numerator>denominator||denominator>1000000||BigInteger.valueOf(numerator).gcd(BigInteger.valueOf(denominator)).longValue()!=1)throw invalid();input=Map.copyOf(input);}
 }
 public Portion normalisePortion(String mode,Map<String,?> request){
  if(request==null||!(request.get("kind") instanceof String kind))throw invalid();
  if("PERSONAL".equals(mode)){if(!kind.equals("PERSONAL")||!request.keySet().equals(Set.of("kind")))throw invalid();return new Portion(kind,1,1,Map.of());}
  if(!"FULL_RECIPE".equals(mode))throw invalid();BigInteger numerator,denominator;
  switch(kind){
   case "FRACTION"->{shape(request,"numerator","denominator");var n=number(request,"numerator",BigDecimal.valueOf(1000000),0);var d=number(request,"denominator",BigDecimal.valueOf(1000000),0);numerator=n.toBigIntegerExact();denominator=d.toBigIntegerExact();}
   case "PERCENT"->{shape(request,"percent");var percent=number(request,"percent",BigDecimal.valueOf(100),3);numerator=percent.movePointRight(3).toBigIntegerExact();denominator=BigInteger.valueOf(100000);}
   case "SERVINGS"->{shape(request,"total","eaten");var total=number(request,"total",BigDecimal.valueOf(1000),0);var eaten=number(request,"eaten",total,3);numerator=eaten.movePointRight(3).toBigIntegerExact();denominator=total.toBigIntegerExact().multiply(BigInteger.valueOf(1000));}
   default->throw invalid();
  }
  if(numerator.compareTo(denominator)>0)throw invalid();var gcd=numerator.gcd(denominator);var original=new LinkedHashMap<String,Object>();request.forEach((k,v)->{if(!k.equals("kind"))original.put(k,v);});return new Portion(kind,numerator.divide(gcd).longValueExact(),denominator.divide(gcd).longValueExact(),original);
 }
 private static void shape(Map<String,?> value,String... fields){var keys=new HashSet<>(List.of(fields));keys.add("kind");if(!value.keySet().equals(keys))throw invalid();}
 private static BigDecimal number(Map<String,?> value,String key,BigDecimal max,int scale){if(!(value.get(key) instanceof Number n))throw invalid();try{var decimal=new BigDecimal(n.toString());if(decimal.signum()<=0||decimal.compareTo(max)>0||decimal.stripTrailingZeros().scale()>scale)throw invalid();return decimal;}catch(NumberFormatException e){throw invalid();}}
 private static BusinessException invalid(){return BusinessException.invalid("portion","Choose one complete supported portion with positive values, within the recipe, and at most three decimal places.");}
}
