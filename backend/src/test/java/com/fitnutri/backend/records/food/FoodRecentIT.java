package com.fitnutri.backend.records.food;
import com.fitnutri.backend.support.*;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("integration-test")
class FoodRecentIT {
 static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
 @Autowired JdbcTemplate jdbc;@Autowired FoodRecentService recent;@Autowired FoodSnapshotReader snapshots;@Autowired PlatformTransactionManager manager;
 long owner,other,reference;
 @BeforeEach void setup()throws Exception{try(var c=DB.connect()){var users=UxV3Fixtures.usersAndDatedWeights(c);owner=users.ownerId();other=users.otherOwnerId();}reference=jdbc.queryForObject("INSERT INTO food_reference(public_food_key,dataset_id,display_name,is_active) VALUES(?,1,'Synthetic food',true) RETURNING id",Long.class,"SYNTHETIC-"+UUID.randomUUID());}
 long single(long user,String created,String date,String grams){return jdbc.queryForObject("INSERT INTO food_record(user_id,created_at,updated_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type,food_reference_id,data_quality,energy_kj,protein_grams,fat_grams,carbs_grams) VALUES(?,?::timestamptz,now(),now(),?::date,gen_random_uuid(),'MANUAL','Synthetic food',?::numeric,'LUNCH',?,'CURRENT',1700,1,2,3) RETURNING id",Long.class,user,created,date,grams,reference);}
 long dish(){return new TransactionTemplate(manager).execute(status->{long id=jdbc.queryForObject("INSERT INTO food_dish_record(user_id,client_entry_id,name,recorded_date,meal_type,input_mode,portion_input_kind,portion_numerator,portion_denominator,portion_input_json,source) VALUES(?,gen_random_uuid(),'Same dish name','2026-10-01','DINNER','PERSONAL','PERSONAL',1,1,'{}','MANUAL') RETURNING id",Long.class,owner);jdbc.update("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type,food_reference_id,data_quality,energy_kj,protein_grams,fat_grams,carbs_grams,dish_id,dish_position,ingredient_input_grams,nutrition_policy) VALUES(?,now(),now(),DATE '2026-10-01',gen_random_uuid(),'MANUAL','Synthetic ingredient',100,'DINNER',?,'CURRENT',1700,1,2,3,?,0,100,'RECIPE_PORTION_RATIONAL_V1')",owner,reference,id);return id;});}
 @Test void backdatedConfirmedUsageWinsWithoutUpdatedAtInfluenceAndDeduplicates(){
  long older=single(owner,"2026-10-01T01:00Z","2026-10-17","100");long latest=single(owner,"2026-10-17T01:00Z","2020-01-01","180");jdbc.update("UPDATE food_record SET updated_at=now() WHERE id=?",older);
  var page=recent.recentSinglesAndDishes(owner,10);assertEquals(1,page.results().size());var root=page.results().getFirst();assertEquals(Long.toString(latest),root.id());assertEquals(0,new java.math.BigDecimal("180").compareTo((java.math.BigDecimal)root.input().get("quantityGrams")));assertEquals("2020-01-01",root.recordDate().toString());assertEquals("1700.000",root.preview().totals().energyKj().toString());
 }
 @Test void dishesStayIndependentAndDoNotPolluteSinglesAndReadDoesNotWrite(){
  long first=dish();long second=dish();long before=jdbc.queryForObject("SELECT count(*) FROM food_record",Long.class);var page=recent.recentSinglesAndDishes(owner,10);assertEquals(2,page.results().size());assertTrue(page.results().stream().allMatch(x->x.input().get("kind").equals("DISH")));assertNotEquals(first,second);assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM food_record",Long.class));assertEquals(1,snapshots.detailForReuse(owner,"dish",first).preview().leaves().size());assertThrows(BusinessException.class,()->snapshots.detailForReuse(other,"dish",first));
 }
 @Test void deletedOtherOwnerAndInactiveReferenceHaveCorrectVisibility(){
  long own=single(owner,"2026-10-17T01:00Z","2026-10-17","180");single(other,"2026-10-17T02:00Z","2026-10-17","999");jdbc.update("UPDATE food_reference SET is_active=false WHERE id=?",reference);
  assertEquals(Long.toString(own),recent.recentSinglesAndDishes(owner,10).results().getFirst().id());assertThrows(BusinessException.class,()->snapshots.detailForReuse(other,"single",own));jdbc.update("UPDATE food_record SET deleted_at=now() WHERE id=?",own);assertTrue(recent.recentSinglesAndDishes(owner,10).results().isEmpty());assertThrows(BusinessException.class,()->snapshots.detailForReuse(owner,"single",own));
 }
 @Test void emptyAndBoundsAreExplicit(){assertTrue(recent.recentSinglesAndDishes(owner,5).results().isEmpty());for(int limit:List.of(0,101))assertThrows(BusinessException.class,()->recent.recentSinglesAndDishes(owner,limit));}
}
