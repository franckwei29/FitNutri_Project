package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SmartFoodSearchTest {
 @Test void explicitAndNegatedConditionsAreParsedWithoutInventingUnknownValues(){
  var query=SmartFoodQuery.parse("Fried egg, no added fat",Map.of());
  assertEquals("NO",query.facets().get("addedFat"));assertEquals("FRIED",query.facets().get("cookingMethod"));assertFalse(query.facets().containsKey("state"));
  var raw=SmartFoodQuery.parse("raw brown rice",Map.of());assertEquals("RAW",raw.facets().get("state"));assertEquals("BROWN",raw.facets().get("type"));
  assertTrue(SmartFoodQuery.parse("rice",Map.of()).facets().isEmpty());
 }
 @Test void contradictoryUnsupportedAndOverlongConditionsAreRejected(){
  for(String query:List.of("raw cooked rice","a".repeat(81),"%"))assertThrows(BusinessException.class,()->SmartFoodQuery.parse(query,Map.of()));
  assertThrows(BusinessException.class,()->SmartFoodQuery.parse("rice",Map.of("state","GUESS")));
  assertThrows(BusinessException.class,()->SmartFoodQuery.parse("cooked rice",Map.of("state","RAW")));
 }
 @Test void bilingualAndPrefixNormalisationUsesWholeWordPatterns(){
  assertEquals("COOKED",SmartFoodQuery.parse("熟糙米饭",Map.of()).facets().get("state"));
  assertEquals("NO",SmartFoodQuery.parse("煎蛋 无油",Map.of()).facets().get("addedFat"));
  var query=SmartFoodQuery.parse("ＲＩＣＥ",Map.of());assertEquals("rice",query.normalised());
  assertFalse(java.util.regex.Pattern.compile(query.javaWholeWordPattern("rice")).matcher("licorice").find());
 }
 @Test void invalidPagingAndModesFailBeforeDatabaseAndNoCatalogueIsServiceUnavailable(){
  var repo=org.mockito.Mockito.mock(com.fitnutri.backend.foodreference.FoodReferenceRepository.class);
  var service=new SmartFoodSearchService(org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class),repo,new tools.jackson.databind.ObjectMapper());
  for(String mode:List.of("UNKNOWN",""))assertThrows(BusinessException.class,()->service.search("rice",Map.of(),mode,0,5,1L));
  for(int size:List.of(0,101))assertThrows(BusinessException.class,()->service.search("rice",Map.of(),"ALL",0,size,1L));
  assertThrows(BusinessException.class,()->service.search("rice",Map.of(),"ALL",-1,5,1L));
  org.mockito.Mockito.verifyNoInteractions(repo);
  assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,assertThrows(BusinessException.class,()->service.search("rice",Map.of(),"ALL",0,5,1L)).status());
 }
 @Test void databaseFailureIsNotReportedAsAnEmptyMatch(){
  var repo=org.mockito.Mockito.mock(com.fitnutri.backend.foodreference.FoodReferenceRepository.class);
  org.mockito.Mockito.when(repo.countSelectable()).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private internal error"));
  var service=new SmartFoodSearchService(org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class),repo,new tools.jackson.databind.ObjectMapper());
  var error=assertThrows(BusinessException.class,()->service.search("rice",Map.of(),"ALL",0,5,1L));
  assertEquals("CATALOGUE_UNAVAILABLE",error.code());assertFalse(error.getMessage().contains("private"));
 }
}
