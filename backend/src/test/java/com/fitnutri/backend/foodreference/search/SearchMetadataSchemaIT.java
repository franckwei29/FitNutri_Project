package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.support.TestDatabase;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SearchMetadataSchemaIT {
    @Test void metadataMigrationPreservesReferencesAndEnforcesReviewForeignKeysAndActiveRevision() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("8").migrate();
            try(var c=db.connect();var q=c.createStatement()) {
                q.execute("INSERT INTO food_reference(public_food_key,dataset_id,display_name,energy_kj_100g) VALUES('SYNTHETIC-UX-1',1,'Synthetic preserved rice',1000)");
            }
            db.flyway().migrate();
            try(var c=db.connect();var q=c.createStatement()) {
                try(var r=q.executeQuery("SELECT display_name,energy_kj_100g FROM food_reference WHERE public_food_key='SYNTHETIC-UX-1'")){assertTrue(r.next());assertEquals("Synthetic preserved rice",r.getString(1));assertEquals("1000.000",r.getBigDecimal(2).toPlainString());}
                String good="INSERT INTO food_search_profile(reference_id,metadata_revision,is_active,review_status,short_name_en,concept_key,facets,common_rank,evidence) VALUES(1,'ux-r1',true,'REVIEWED','Synthetic rice','rice','{\"state\":\"COOKED\"}',20,'{\"field\":\"officialName\",\"text\":\"Synthetic fixture\"}')";
                q.execute(good);
                SQLException duplicate=assertThrows(SQLException.class,()->q.execute(good));assertEquals("23505",duplicate.getSQLState());
                SQLException activeRevision=assertThrows(SQLException.class,()->q.execute(good.replace("'ux-r1'","'ux-r2'")));assertEquals("23505",activeRevision.getSQLState());
                SQLException unreviewed=assertThrows(SQLException.class,()->q.execute(good.replace("'ux-r1'","'ux-draft'").replace("'REVIEWED'","'DRAFT'")));assertEquals("23514",unreviewed.getSQLState());
                SQLException orphan=assertThrows(SQLException.class,()->q.execute(good.replace("VALUES(1,","VALUES(999999,")));assertEquals("23503",orphan.getSQLState());
                q.execute("INSERT INTO food_search_alias(profile_id,locale,normalised_text,metadata_revision,review_status) VALUES(1,'zh','米饭','ux-r1','REVIEWED')");
                // One alias can target both a profile and its concept in separate rows, never both in one row.
                q.execute("INSERT INTO food_search_alias(concept_key,locale,normalised_text,metadata_revision,review_status) VALUES('rice','zh','米饭','ux-r1','REVIEWED')");
                SQLException ambiguous=assertThrows(SQLException.class,()->q.execute("INSERT INTO food_search_alias(profile_id,concept_key,locale,normalised_text,metadata_revision,review_status) VALUES(1,'rice','zh','bad','ux-r1','REVIEWED')"));assertEquals("23514",ambiguous.getSQLState());
                try(var r=q.executeQuery("SELECT count(*) FROM food_search_alias WHERE normalised_text='米饭'")){assertTrue(r.next());assertEquals(2,r.getInt(1));}
                assertEquals(0,db.flyway().migrate().migrationsExecuted);
            }
        }
    }
}
