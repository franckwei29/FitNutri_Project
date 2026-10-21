package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.support.PostgresIntegrationSupport;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
class FoodMetadataImportIT extends PostgresIntegrationSupport {
 @Autowired ReferenceImportService references;
 @Autowired FoodReferenceRepository foods;
 @Autowired FoodSearchMetadataImporter importer;
 @Autowired JdbcTemplate jdbc;
 @Test void reviewedMetadataDryRunImportReplayAndReferenceFailurePreserveOfficialNutrients()throws Exception {
  Path directory=Path.of(System.getenv().getOrDefault("TEST_REFERENCE_SOURCE_DIR",Path.of("..",".local","reference-sources").toString())).toAbsolutePath().normalize();Map<String,Resource> raw=new HashMap<>();
  for(String name:List.of("ausnut-2023-food-details.xlsx","ausnut-2023-food-nutrient-profiles.xlsx","ausnut-2023-list-of-nutrients.xlsx"))raw.put(name,new FileSystemResource(directory.resolve(name)));
  references.importManifest(new ClassPathResource("data/manifests/ausnut-2023-r1.json"),new ClassPathResource("data/ausnut-2023-foods-r1.json"),raw);
  var resource=new ClassPathResource("data/ux/food-search-metadata-v1.json");
  String before=jdbc.queryForObject("SELECT md5(string_agg(concat_ws('|',id,public_food_key,energy_kj_100g,protein_g_100g,fat_g_100g,carbs_g_100g),';' ORDER BY id)) FROM food_reference",String.class);
  assertTrue(importer.importRevision(resource,true).dryRun());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM food_search_profile",Integer.class));
  var rice=foods.findActiveByPublicFoodKey("F007661").orElseThrow();rice.setActive(false);foods.saveAndFlush(rice);
  assertEquals("UX_METADATA_REFERENCE_UNAVAILABLE",assertThrows(BusinessException.class,()->importer.importRevision(resource,false)).code());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM food_search_profile",Integer.class));
  rice.setActive(true);foods.saveAndFlush(rice);
  var report=importer.importRevision(resource,false);assertEquals(52,report.importedProfiles());assertFalse(report.replayed());assertTrue(report.importedAliases()>100);
  var replay=importer.importRevision(resource,false);assertTrue(replay.replayed());assertEquals(0,replay.importedProfiles());assertEquals(52,jdbc.queryForObject("SELECT count(*) FROM food_search_profile",Integer.class));
  assertEquals(3741,foods.countSelectable());assertEquals(before,jdbc.queryForObject("SELECT md5(string_agg(concat_ws('|',id,public_food_key,energy_kj_100g,protein_g_100g,fat_g_100g,carbs_g_100g),';' ORDER BY id)) FROM food_reference",String.class));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM food_search_profile p JOIN food_reference f ON f.id=p.reference_id WHERE f.public_food_key='F003718' AND facets->>'addedFat'='NO'",Integer.class));
 }
}
