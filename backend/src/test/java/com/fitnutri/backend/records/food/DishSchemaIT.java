package com.fitnutri.backend.records.food;
import com.fitnutri.backend.support.*;
import java.sql.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DishSchemaIT {
    private TestDatabase database() throws Exception {
        var db=TestDatabase.create();db.flywayAtVersion("9").migrate();
        try(var c=db.connect();var s=c.createStatement()) {
            UxV3Fixtures.usersAndDatedWeights(c);
            s.execute("INSERT INTO food_reference(public_food_key,dataset_id,display_name) VALUES('SYNTHETIC-DISH',1,'Synthetic preserved reference')");
            s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type) VALUES(1,now(),now(),'2026-10-17',gen_random_uuid(),'MANUAL','Synthetic old single',123.456,'LUNCH')");
        }
        db.flywayAtVersion("10").migrate();return db;
    }
    private void dish(Statement s,String name,String portion,String kind,long owner) throws Exception {
        s.execute("INSERT INTO food_dish_record(user_id,client_entry_id,name,recorded_date,meal_type,input_mode,portion_input_kind,portion_numerator,portion_denominator,portion_input_json,source) VALUES("+owner+",gen_random_uuid(),'"+name+"','2026-10-17','LUNCH','FULL_RECIPE','"+kind+"',1,3,'"+portion+"','MANUAL')");
    }
    private String leaf(long owner,String grams) {
        return "INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type,food_reference_id,data_quality,dish_id,dish_position,ingredient_input_grams,nutrition_policy) VALUES("+owner+",now(),now(),'2026-10-17',gen_random_uuid(),'MANUAL','Synthetic ingredient',"+grams+",'LUNCH',1,'CURRENT',1,0,0.001,'RECIPE_PORTION_RATIONAL_V1')";
    }
    @Test void migrationKeepsLegacySingleAndSmallDerivedPositiveQuantities() throws Exception {
        try(var db=database();var c=db.connect();var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT quantity_grams,dish_id,ingredient_input_grams FROM food_record WHERE id=1")) {
                assertTrue(r.next());assertEquals(0,new BigDecimal("123.456").compareTo(r.getBigDecimal(1)));assertNull(r.getObject(2));assertNull(r.getObject(3));
            }
            dish(s,"Synthetic recipe","{\"numerator\":1,\"denominator\":3}","FRACTION",1);
            s.execute(leaf(1,"0.000333333333"));
            try(var r=s.executeQuery("SELECT quantity_grams FROM food_record WHERE dish_id=1")){assertTrue(r.next());assertEquals(new BigDecimal("0.000333333333"),r.getBigDecimal(1));}
            s.execute("UPDATE food_record SET quantity_grams=0.000000000010 WHERE dish_id=1");
            try(var r=s.executeQuery("SELECT quantity_grams FROM food_record WHERE dish_id=1")){assertTrue(r.next());assertTrue(r.getBigDecimal(1).signum()>0);}
        }
    }
    @Test void crossOwnerLeafFailsAtCommitAndDoesNotChangeTheOtherHistory() throws Exception {
        try(var db=database();var c=db.connect();var s=c.createStatement()) {
            dish(s,"First occurrence","{\"numerator\":1,\"denominator\":3}","FRACTION",1);
            dish(s,"Second occurrence","{\"numerator\":1,\"denominator\":3}","FRACTION",1);
            c.setAutoCommit(false);s.execute(leaf(2,"0.001"));
            assertEquals("23503",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();c.setAutoCommit(true);
            s.execute("UPDATE food_dish_record SET name='Changed one occurrence' WHERE id=2");
            try(var r=s.executeQuery("SELECT name FROM food_dish_record WHERE id=1")){assertTrue(r.next());assertEquals("First occurrence",r.getString(1));}
            try(var r=s.executeQuery("SELECT count(*) FROM food_record")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
        }
    }
    @Test void portionShapeRatioAndBoundsAreEnforcedByDatabase() throws Exception {
        try(var db=database();var c=db.connect();var s=c.createStatement()) {
            dish(s,"Good","{\"numerator\":1,\"denominator\":3}","FRACTION",1);
            for(String patch:new String[]{"portion_denominator=0","portion_numerator=4","portion_input_json='{\"percent\":33.3}'","portion_input_json='{\"numerator\":1,\"denominator\":3,\"private\":1}'","input_mode='PERSONAL'","portion_numerator=2,portion_denominator=6"})
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_dish_record SET "+patch+" WHERE id=1")).getSQLState(),patch);
            s.execute("UPDATE food_dish_record SET portion_input_kind='PERCENT',portion_numerator=333,portion_denominator=1000,portion_input_json='{\"percent\":33.3}' WHERE id=1");
            s.execute("UPDATE food_dish_record SET portion_input_kind='SERVINGS',portion_numerator=1,portion_denominator=4,portion_input_json='{\"total\":4,\"eaten\":1}' WHERE id=1");
            s.execute("UPDATE food_dish_record SET input_mode='PERSONAL',portion_input_kind='PERSONAL',portion_numerator=1,portion_denominator=1,portion_input_json='{}' WHERE id=1");
        }
    }
}
