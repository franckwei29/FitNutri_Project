package com.fitnutri.backend.support;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UxV3OwnedFixtureIT {
    @Test void datedWeightFixtureKeepsIndependentOwnersAndHistoricalWeights() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flyway().migrate();
            try(var c=db.connect()) {
                var owners=UxV3Fixtures.usersAndDatedWeights(c);
                assertNotEquals(owners.ownerId(),owners.otherOwnerId());
                try(var q=c.prepareStatement("SELECT weight_kg FROM weight_entry WHERE user_id=? AND entry_date<=DATE '2026-10-16' ORDER BY entry_date DESC LIMIT 1")) {
                    q.setLong(1,owners.ownerId());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(0,new BigDecimal("70").compareTo(r.getBigDecimal(1)));assertFalse(r.next());}
                    q.setLong(1,owners.otherOwnerId());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(0,new BigDecimal("90").compareTo(r.getBigDecimal(1)));}
                }
                try(var q=c.createStatement();var r=q.executeQuery("SELECT count(*) FROM weight_entry")){assertTrue(r.next());assertEquals(3,r.getInt(1));}
            }
        }
    }
}
