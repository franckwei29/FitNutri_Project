package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class FoodMetadataImporterTest {
 final FoodSearchMetadataReader reader=new FoodSearchMetadataReader(JsonMapper.builder().build());
 @Test void pinnedReviewedMetadataUsesRealSourceKeysAndExplicitNegatedConditions() {
  var document=reader.prepare(new ClassPathResource("data/ux/food-search-metadata-v1.json"));
  assertEquals(52,document.entries().size());assertEquals("2023",document.dataset().release());
  var noFat=document.entries().stream().filter(x->x.publicFoodKey().equals("F003718")).findFirst().orElseThrow();
  assertEquals("NO",noFat.facets().get("addedFat"));assertEquals("FRIED",noFat.facets().get("cookingMethod"));
  assertTrue(document.entries().stream().allMatch(x->x.reviewStatus().equals("REVIEWED")));
 }
 @Test void changedConditionsUnknownKeysAndUnreviewedArtifactsCannotActivate()throws Exception {
  String source=new String(new ClassPathResource("data/ux/food-search-metadata-v1.json").getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
  for(String changed:new String[]{source.replace("\"NO\"","\"YES\""),source.replace("F003718","NOT-REAL"),source.replace("REVIEWED","DRAFT"),source.replace("\"commonRank\": 20","\"energyKj100g\": 0")})
   assertEquals("UX_METADATA_INVALID",assertThrows(BusinessException.class,()->reader.prepare(new ByteArrayResource(changed.getBytes(java.nio.charset.StandardCharsets.UTF_8)))).code());
 }
 @Test void bilingualNormalisationIsDeterministicAndNoSubstringClassificationIsImplied() {
  assertEquals("fried egg",FoodSearchMetadataReader.normalise("  ＦＲＩＥＤ-Egg! "));
  assertEquals("米饭",FoodSearchMetadataReader.normalise(" 米饭 "));
  assertNotEquals(FoodSearchMetadataReader.normalise("rice"),FoodSearchMetadataReader.normalise("licorice"));
 }
}
