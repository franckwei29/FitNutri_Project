package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController @RequestMapping("/api/food-reference")
public class SmartFoodSearchController {
 private final SmartFoodSearchService search;private final CurrentUserService current;private final ObjectMapper json;
 public SmartFoodSearchController(SmartFoodSearchService search,CurrentUserService current,ObjectMapper json){this.search=search;this.current=current;this.json=json;}
 @GetMapping("/smart-search") public ResponseEntity<SmartFoodSearchService.Response> search(@RequestParam String query,@RequestParam(defaultValue="SUGGESTED") String mode,@RequestParam(required=false) String facets,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="5") int size,Authentication auth){
  var owner=current.requireGeneralUser(auth);Map<String,String> selected=new LinkedHashMap<>();
  if(facets!=null){if(facets.length()>1000)throw invalid();try{var tree=json.readTree(facets);if(!tree.isObject())throw invalid();for(String key:tree.propertyNames()){var v=tree.get(key);if(!v.isString())throw invalid();selected.put(key,v.asText());}}catch(tools.jackson.core.JacksonException ex){throw invalid();}}
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(search.search(query,selected,mode,page,size,owner.getId()));
 }
 private static BusinessException invalid(){return BusinessException.invalid("facets","Choose supported food conditions as a JSON object.");}
}
