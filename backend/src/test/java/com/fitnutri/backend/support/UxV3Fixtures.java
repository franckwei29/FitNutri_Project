package com.fitnutri.backend.support;

import com.fitnutri.backend.foodreference.FoodDataset;
import com.fitnutri.backend.foodreference.FoodReference;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/** Explicit synthetic data, never an official source import or a production seed. */
public final class UxV3Fixtures {
    private UxV3Fixtures() {}
    public static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-17T01:00:00Z"),ZoneId.of("Australia/Sydney"));
    public static final BigDecimal WEIGHT_KG=new BigDecimal("70");
    public static List<FoodReference> syntheticFoodRefs() {
        return List.of(food(101L,"1000"),food(102L,"400"),food(103L,"500"));
    }
    public record OwnedWeights(long ownerId,long otherOwnerId) {}
    /** Only a connection from TestDatabase may be supplied; this creates test identities, never production users. */
    public static OwnedWeights usersAndDatedWeights(java.sql.Connection connection) throws java.sql.SQLException {
        String db=connection.getCatalog();
        if(db==null||!db.matches("fitnutri_it_[0-9a-f]{32}"))throw new IllegalArgumentException("Owned test database required");
        long[] ids=new long[2];
        for(int i=0;i<2;i++) {
            try(var insert=connection.prepareStatement("INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(?, ?, ?,true,'synthetic-only','USER') RETURNING id")) {
                insert.setObject(1,java.time.OffsetDateTime.ofInstant(CLOCK.instant(),java.time.ZoneOffset.UTC));
                insert.setString(2,"Synthetic UX owner "+i);insert.setString(3,"ux-"+java.util.UUID.randomUUID()+"@example.test");
                try(var result=insert.executeQuery()){result.next();ids[i]=result.getLong(1);}
            }
        }
        try(var insert=connection.prepareStatement("INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES(?,?,?,?)")) {
            for(int i=0;i<3;i++) {
                insert.setObject(1,java.time.OffsetDateTime.ofInstant(CLOCK.instant(),java.time.ZoneOffset.UTC));
                insert.setObject(2,java.time.LocalDate.of(2026,10,i==1?17:1));
                insert.setBigDecimal(3,new BigDecimal(i==0?"70":i==1?"75":"90"));
                insert.setLong(4,ids[i==2?1:0]);insert.addBatch();
            }
            insert.executeBatch();
        }
        return new OwnedWeights(ids[0],ids[1]);
    }
    private static FoodReference food(long id,String energy) {
        var dataset=new FoodDataset();dataset.setId(1L);dataset.setVerified(true);dataset.setContentSha256("a".repeat(64));
        var f=new FoodReference();f.setId(id);f.setDataset(dataset);f.setActive(true);
        f.setPublicFoodKey("SYNTHETIC-"+id);f.setOfficialName("Synthetic test reference "+id);f.setDisplayName(f.getOfficialName());
        f.setEnergyKj100g(new BigDecimal(energy));f.setProteinG100g(BigDecimal.ZERO);f.setFatG100g(BigDecimal.ZERO);f.setCarbsG100g(BigDecimal.ZERO);
        return f;
    }
}
