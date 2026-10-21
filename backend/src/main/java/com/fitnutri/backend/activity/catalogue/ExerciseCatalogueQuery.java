package com.fitnutri.backend.activity.catalogue;
import com.fitnutri.backend.common.BusinessException;
import java.text.Normalizer;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
/** Small reviewed directories stay in PostgreSQL; parameters never become SQL identifiers or patterns unescaped. */
final class ExerciseCatalogueQuery {
 private ExerciseCatalogueQuery(){}
 static final Set<String> CATEGORIES=Set.of("CARDIO","STRENGTH","SPORTS","OUTDOOR_OTHER");
 static final Set<String> BODY_REGIONS=Set.of("CHEST","BACK","SHOULDERS","LEGS_GLUTES","ARMS","CORE");
 static final Set<String> EQUIPMENT=Set.of("BARBELL","DUMBBELL","KETTLEBELL","SMITH_MACHINE","MACHINE","CABLE","BODYWEIGHT","ASSISTED_MACHINE");
 static String validate(String query,int page,int size){if(page<0||size<1||size>100)throw BusinessException.invalid("page","Choose a nonnegative page and size from 1 to 100.");if(query!=null&&query.length()>80)throw BusinessException.invalid("query","Use at most 80 characters.");String value=query==null?"":Normalizer.normalize(query,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").strip();if(query!=null&&!query.isBlank()&&value.isBlank())throw BusinessException.invalid("query","Use an exercise name or equipment alias.");return value;}
 static void field(String name,String value,Set<String> allowed){if(value!=null&&!allowed.contains(value))throw BusinessException.invalid(name,"Choose a supported catalogue filter.");}
 static String matches(String query,String name,String aliases,MapSqlParameterSource args){if(query.isEmpty())return "true";var parts=new ArrayList<String>();int i=0;for(String word:query.split("\\s+")){String parameter="word"+i++;boolean chinese=word.matches(".*[\\p{IsHan}].*");args.addValue(parameter,chinese?word:"(^|[^[:alnum:]])"+word);parts.add(chinese?"(position(:"+parameter+" in lower("+name+"))>0 OR EXISTS(SELECT 1 FROM jsonb_array_elements_text("+aliases+") a WHERE position(:"+parameter+" in lower(a))>0))":"(lower("+name+") ~ :"+parameter+" OR EXISTS(SELECT 1 FROM jsonb_array_elements_text("+aliases+") a WHERE lower(a) ~ :"+parameter+"))");}return String.join(" AND ",parts);}
 static String selectableOption(String alias){return alias+".active AND "+alias+".review_status='REVIEWED' AND ("+alias+".mapping_kind='UNAVAILABLE' OR EXISTS(SELECT 1 FROM exercise_type t JOIN activity_dataset d ON d.id=t.dataset_id WHERE t.id="+alias+".reference_id AND t.is_active AND d.verified AND d.min_age IS NOT NULL AND d.max_age IS NOT NULL AND t.source_activity_code IS NOT NULL AND t.source_description IS NOT NULL))";}
 static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","The reviewed exercise directory is unavailable. Try again later.");}
}
