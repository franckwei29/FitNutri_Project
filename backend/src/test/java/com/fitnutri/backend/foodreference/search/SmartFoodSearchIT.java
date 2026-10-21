package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.support.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("integration-test") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmartFoodSearchIT {
 static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
 @Autowired ReferenceImportService source;@Autowired FoodSearchMetadataImporter metadata;@Autowired SmartFoodSearchService service;@Autowired JdbcTemplate jdbc;
 @BeforeAll void prepare()throws Exception{
  Path directory=Path.of(System.getenv().getOrDefault("TEST_REFERENCE_SOURCE_DIR",Path.of("..",".local","reference-sources").toString())).toAbsolutePath().normalize();Map<String,Resource> raw=new HashMap<>();
  for(String name:List.of("ausnut-2023-food-details.xlsx","ausnut-2023-food-nutrient-profiles.xlsx","ausnut-2023-list-of-nutrients.xlsx"))raw.put(name,new FileSystemResource(directory.resolve(name)));
  source.importManifest(new ClassPathResource("data/manifests/ausnut-2023-r1.json"),new ClassPathResource("data/ausnut-2023-foods-r1.json"),raw);metadata.importRevision(new ClassPathResource("data/ux/food-search-metadata-v1.json"),false);
 }
 @Test void riceWordBoundaryExplicitAttributesAndStableFullPagination(){
  var result=service.search("raw brown rice",Map.of(),"ALL",0,20,1L);assertFalse(result.results().isEmpty());assertEquals("F007648",result.results().getFirst().reference().publicFoodKey());
  var a=service.search("rice",Map.of(),"ALL",0,20,1L);var b=service.search("rice",Map.of(),"ALL",1,20,1L);
  assertTrue(a.totalMatches()>20);assertEquals(20,a.results().size());assertTrue(a.results().stream().noneMatch(x->x.reference().displayName().toLowerCase().contains("licorice")));
  var ids=new HashSet<>(a.results().stream().map(x->x.reference().referenceId()).toList());assertTrue(b.results().stream().noneMatch(x->ids.contains(x.reference().referenceId())));
  assertEquals(a,service.search("rice",Map.of(),"ALL",0,20,1L));
 }
 @Test void knownConditionsDoNotReaskAndUnknownChoiceDoesNotMeanNoFat(){
  var boiled=service.search("boiled egg",Map.of(),"SUGGESTED",0,5,1L);assertTrue(boiled.nextQuestion()==null||!boiled.nextQuestion().key().equals("cookingMethod"));
  var fried=service.search("fried egg",Map.of(),"SUGGESTED",0,5,1L);assertNotNull(fried.nextQuestion());assertEquals("addedFat",fried.nextQuestion().key());
  var approximate=service.search("fried egg",Map.of(),"APPROXIMATE",0,5,1L);assertNull(approximate.nextQuestion());assertTrue(approximate.results().stream().allMatch(x->x.matchKind().equals("APPROXIMATE")));
  assertTrue(approximate.results().stream().anyMatch(x->x.reference().publicFoodKey().equals("F003716")));assertTrue(approximate.results().stream().anyMatch(x->x.reference().publicFoodKey().equals("F003718")));
 }
 @Test void reviewedChineseAliasesAndRareOriginalNamesRemainReachable(){
  var chinese=service.search("糙米饭",Map.of(),"ALL",0,20,1L);assertTrue(chinese.results().stream().anyMatch(x->x.reference().publicFoodKey().equals("F007641")));
  var rare=jdbc.queryForMap("SELECT public_food_key,official_name FROM food_reference WHERE is_active AND public_food_key NOT IN (SELECT f.public_food_key FROM food_search_profile p JOIN food_reference f ON f.id=p.reference_id) ORDER BY id LIMIT 1");
  String name=rare.get("official_name").toString();assertTrue(name.length()<=80);
  var found=service.search(name,Map.of(),"ALL",0,100,1L);assertTrue(found.results().stream().anyMatch(x->x.reference().publicFoodKey().equals(rare.get("public_food_key"))));
  assertEquals(0,service.search("不存在的测试食物",Map.of(),"ALL",0,20,1L).totalMatches());
 }
 @Test void confirmedOwnerRecentPreferenceNeverOverridesExplicitState()throws Exception{
  long owner;try(var c=DB.connect()){owner=UxV3Fixtures.usersAndDatedWeights(c).ownerId();}
  jdbc.update("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type,food_reference_id) SELECT ?,now(),now(),DATE '2020-01-01',gen_random_uuid(),'MANUAL','Synthetic confirmed preference',100,'LUNCH',id FROM food_reference WHERE public_food_key='F007661'",owner);
  var raw=service.search("raw brown rice",Map.of(),"ALL",0,20,owner);assertEquals("F007648",raw.results().getFirst().reference().publicFoodKey());
  assertEquals("F007661",service.search("rice",Map.of(),"SUGGESTED",0,5,owner).results().getFirst().reference().publicFoodKey());
  long other;try(var c=DB.connect()){other=UxV3Fixtures.usersAndDatedWeights(c).otherOwnerId();}
  assertEquals(service.search("rice",Map.of(),"SUGGESTED",0,5,other),service.search("rice",Map.of(),"SUGGESTED",0,5,999999L));
 }
}
