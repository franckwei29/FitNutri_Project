package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.support.PostgresIntegrationSupport;
import com.fitnutri.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

class WeightServiceIntegrationTest extends PostgresIntegrationSupport {
    @Autowired private WeightService service;
    @Autowired private UserRepository users;
    @Autowired private UserProfileRepository profiles;
    @Autowired private WeightEntryRepository weights;
    @Autowired private JdbcTemplate jdbc;
    private User user;
    @BeforeEach void arrange() {
        user=new User();user.setEmail("weight-"+UUID.randomUUID()+"@example.test");user.setDisplayName("Synthetic");user.setPasswordHash("test-not-a-login-hash");user=users.saveAndFlush(user);
    }
    private AddWeightRequest request(String date,String weight,Long version) {return new AddWeightRequest(LocalDate.parse(date),new BigDecimal(weight),version,"Australia/Sydney");}
    @Test void createSameValueCorrectionAndStaleVersionPreserveOneRow() {
        var created=service.upsertByDate(user.getId(),request("2020-01-01","70",null));assertTrue(created.created());assertEquals(0,created.result().weight().version());
        var same=service.upsertByDate(user.getId(),request("2020-01-01","70.00",null));assertFalse(same.created());assertEquals(created.result(),same.result());
        var conflict=assertThrows(WeightConflictException.class,()->service.upsertByDate(user.getId(),request("2020-01-01","71",null)));
        assertEquals("VERSION_CONFLICT",conflict.code());assertEquals(created.result().weight(),conflict.currentItem());
        var corrected=service.upsertByDate(user.getId(),request("2020-01-01","71",0L));assertEquals(1,corrected.result().weight().version());assertEquals(new BigDecimal("71.00"),corrected.result().weight().weightKg());
        assertThrows(WeightConflictException.class,()->service.upsertByDate(user.getId(),request("2020-01-01","71",0L)));
        assertEquals(1,weights.findByUserIdOrderByEntryDateDescCreatedAtDesc(user.getId()).size());
    }
    @Test void backfillUsesMeasurementDateAndReturnsActualProfileVersion() {
        var profile=new UserProfile();profile.setUser(user);profile=profiles.saveAndFlush(profile);
        var latest=service.upsertByDate(user.getId(),request("2020-02-01","72",null));
        var backfill=service.upsertByDate(user.getId(),request("2020-01-01","70",null));
        assertEquals(latest.result().weight(),backfill.result().currentWeight());assertTrue(backfill.result().profileVersion()>profile.getVersion());
        assertEquals(72,profiles.findById(user.getId()).orElseThrow().getWeightKg());
        assertTrue(service.latestOnOrBefore(user.getId(),LocalDate.of(2019,12,31)).isEmpty());
        assertEquals(new BigDecimal("70.00"),service.latestOnOrBefore(user.getId(),LocalDate.of(2020,1,15)).orElseThrow().getWeightKg());
        assertEquals(1,service.history(user.getId(),LocalDate.of(2020,1,1),LocalDate.of(2020,1,31),"Australia/Sydney").size());
    }
    @Test void missingVersionTargetAndInvalidPrecisionFutureDateDoNotWrite() {
        assertThrows(WeightConflictException.class,()->service.upsertByDate(user.getId(),request("2020-01-01","70",0L)));
        assertThrows(BusinessException.class,()->service.upsertByDate(user.getId(),request("2020-01-01","70.001",null)));
        assertThrows(BusinessException.class,()->service.upsertByDate(user.getId(),request("2090-01-01","70",null)));
        assertThrows(BusinessException.class,()->service.upsertByDate(user.getId(),request("2020-01-01","0",null)));
        assertTrue(weights.findByUserIdOrderByEntryDateDescCreatedAtDesc(user.getId()).isEmpty());assertTrue(profiles.findById(user.getId()).isEmpty());
    }
    @Test void concurrentNewDateCreatesExactlyOneAndDoesNotSilentlyOverwrite() throws Exception {
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> a=()->{start.await();try{return service.upsertByDate(user.getId(),request("2020-01-01","70",null)).created()?201:200;}catch(BusinessException e){return e.status().value();}};
            Callable<Integer> b=()->{start.await();try{return service.upsertByDate(user.getId(),request("2020-01-01","71",null)).created()?201:200;}catch(BusinessException e){return e.status().value();}};
            var first=pool.submit(a);var second=pool.submit(b);start.countDown();var codes=new ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));Collections.sort(codes);
            assertEquals(List.of(201,409),codes);assertEquals(1,weights.findByUserIdOrderByEntryDateDescCreatedAtDesc(user.getId()).size());
        }
    }
    @Test void anotherOwnerCannotReadOrCorrectMeasurements() {
        service.upsertByDate(user.getId(),request("2020-01-01","70",null));
        var other=new User();other.setEmail("other-"+UUID.randomUUID()+"@example.test");other.setDisplayName("Other synthetic");other.setPasswordHash("test-not-a-login-hash");other=users.saveAndFlush(other);
        assertTrue(service.history(other.getId(),LocalDate.of(2020,1,1),LocalDate.of(2020,1,31),"Australia/Sydney").isEmpty());
        long otherId=other.getId();
        assertThrows(WeightConflictException.class,()->service.upsertByDate(otherId,request("2020-01-01","71",0L)));
        assertEquals(new BigDecimal("70.00"),service.latestOnOrBefore(user.getId(),LocalDate.of(2020,1,1)).orElseThrow().getWeightKg());
    }
    @Test void correctionsDoNotRecomputeExistingExerciseSnapshot() {
        service.upsertByDate(user.getId(),request("2020-01-01","70",null));
        jdbc.update("INSERT INTO exercise_record(user_id,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,intensity,estimated_calories,created_at) VALUES (?,CURRENT_TIMESTAMP,'2020-01-01',gen_random_uuid(),'MANUAL',?,?,?,?,CURRENT_TIMESTAMP)",user.getId(),"Synthetic walk",30,"LOW",123.5);
        service.upsertByDate(user.getId(),request("2020-01-01","80",0L));
        assertEquals(123.5,jdbc.queryForObject("SELECT estimated_calories FROM exercise_record WHERE user_id=?",Double.class,user.getId()));
    }
}
