package com.fitnutri.backend.foodreference.search;

import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import java.util.regex.Pattern;

/** Explicit conditions only; absence of evidence never means 'no added fat'. */
public record SmartFoodQuery(String normalised,Map<String,String> facets,List<String> baseWords,List<String> originalWords,boolean chinese,boolean prefix) {
 public static final Map<String,Set<String>> ALLOWED=Map.of("state",Set.of("RAW","COOKED"),"type",Set.of("WHITE","BROWN"),"part",Set.of("BREAST","THIGH"),"cookingMethod",Set.of("FRIED","BOILED"),"addedFat",Set.of("YES","NO"),"dairyFat",Set.of("REGULAR","REDUCED","SKIM"));
 public static SmartFoodQuery parse(String value,Map<String,String> selected) {
  if(value==null||value.length()>80)throw invalid();
  String text=FoodSearchMetadataReader.normalise(value);if(text.isBlank()||text.length()<2&&!text.matches(".*[\\p{IsHan}].*"))throw invalid();
  var facets=new LinkedHashMap<String,String>();
  detect(facets,"state","RAW",has(text,"raw|uncooked")||text.contains("生")||text.contains("未煮"));
  detect(facets,"state","COOKED",has(text,"cooked")||text.contains("熟"));
  detect(facets,"type","BROWN",has(text,"brown")||text.contains("糙米"));
  detect(facets,"type","WHITE",has(text,"white")||text.contains("白米"));
  detect(facets,"part","BREAST",has(text,"breast")||text.contains("鸡胸"));
  detect(facets,"part","THIGH",has(text,"thigh")||text.contains("鸡腿"));
  detect(facets,"cookingMethod","FRIED",has(text,"fried")||text.contains("煎"));
  detect(facets,"cookingMethod","BOILED",has(text,"boiled")||text.contains("水煮"));
  boolean no=has(text,"no added fat|no fat added|without added fat|no oil|without oil")||text.contains("无油")||text.contains("不加油");
  detect(facets,"addedFat","NO",no);detect(facets,"addedFat","YES",!no&&(has(text,"added fat|fat added|with oil")||text.contains("加油")));
  detect(facets,"dairyFat","SKIM",has(text,"skim|skimmed"));detect(facets,"dairyFat","REDUCED",has(text,"reduced fat|low fat"));detect(facets,"dairyFat","REGULAR",has(text,"whole milk|full fat"));
  if(selected==null)throw invalid();
  selected.forEach((k,v)->{if(!ALLOWED.containsKey(k)||!ALLOWED.get(k).contains(v))throw invalid();detect(facets,k,v,true);});
  String base=text.replaceAll("(?<![\\p{L}\\p{N}])(no added fat|no fat added|without added fat|without oil|no oil|added fat|fat added|with oil|reduced fat|low fat|full fat|raw|uncooked|cooked|brown|white|breast|thigh|fried|boiled|skimmed|skim|whole)(?![\\p{L}\\p{N}])"," ");
  return new SmartFoodQuery(text,Map.copyOf(facets),words(base),words(text),text.matches(".*[\\p{IsHan}].*"),!Character.isWhitespace(value.charAt(value.length()-1)));
 }
 private static List<String> words(String value){return Arrays.stream(value.split("[^a-z0-9]+" )).filter(s->!s.isBlank()).distinct().toList();}
 private static boolean has(String text,String expression){return Pattern.compile("(?<![\\p{L}\\p{N}])(?:"+expression+")(?![\\p{L}\\p{N}])").matcher(text).find();}
 private static void detect(Map<String,String> facets,String key,String value,boolean present){if(present&&facets.putIfAbsent(key,value)!=null&&!value.equals(facets.get(key)))throw invalid();}
 public String javaWholeWordPattern(String token){return "(?<![\\p{L}\\p{N}])"+Pattern.quote(token)+"(?![\\p{L}\\p{N}])";}
 private static BusinessException invalid(){return BusinessException.invalid("query","Choose a valid food query and consistent supported conditions (at most 80 characters).");}
}
