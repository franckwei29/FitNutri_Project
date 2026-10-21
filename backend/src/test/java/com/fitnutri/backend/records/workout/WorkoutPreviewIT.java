package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.ProfileService;
import com.fitnutri.backend.records.RecordSource;
import com.fitnutri.backend.support.*;
import com.fitnutri.backend.user.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("integration-test")
class WorkoutPreviewIT {
 static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
 @Autowired WorkoutEstimateService service;@Autowired UserRepository users;@Autowired ProfileService profiles;@Autowired JdbcTemplate jdbc;
 User owner;WorkoutV2TestFixtures.Catalogue catalogue;final LocalDate date=LocalDate.of(2020,1,2);
 @BeforeEach void setup(){owner=FoodV2TestFixtures.owner(users,profiles);catalogue=WorkoutV2TestFixtures.catalogue(jdbc);}
 ExerciseEntryInputs.PreviewPeriod period(long option,String kind,String minutes){return new ExerciseEntryInputs.PreviewPeriod(UUID.randomUUID(),kind,Long.toString(option),new BigDecimal(minutes),Map.of(),List.of());}
 ExerciseEntryInputs.PreviewRequest request(List<ExerciseEntryInputs.PreviewPeriod> periods,LocalDate day){return new ExerciseEntryInputs.PreviewRequest(day,"Australia/Sydney",RecordSource.MANUAL,new ExerciseEntryInputs.WorkoutPreview(UUID.randomUUID(),"Synthetic workout",periods,null));}
 @Test void actualDatedWeightAndNoWriteSeventyMinuteExample(){var strength=period(catalogue.strengthOption(),"STRENGTH","50");var cardio=period(catalogue.cardioOption(),"ACTIVITY","20");jdbc.update("INSERT INTO weight_entry(user_id,created_at,entry_date,weight_kg) VALUES(?,now(),DATE '2020-01-03',75)",owner.getId());var input=request(List.of(strength,cardio),date);var first=service.previewNew(owner.getId(),input);assertEquals(new BigDecimal("1342.366"),first.knownEnergyKj());assertEquals(new BigDecimal("70"),first.totalMinutes());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM exercise_record",Long.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM workout_record",Long.class));var moves=catalogue.movements().stream().map(id->new ExerciseEntryInputs.Movement(id.toString(),3L,8L,new BigDecimal("40"))).toList();var detailed=new ExerciseEntryInputs.PreviewPeriod(strength.clientEntryId(),strength.periodKind(),strength.optionId(),strength.durationMinutes(),Map.of(),moves);var changed=new ExerciseEntryInputs.PreviewRequest(date,input.zone(),input.source(),new ExerciseEntryInputs.WorkoutPreview(input.entry().clientEntryId(),"Renamed",List.of(detailed,cardio),null));assertEquals(first.fingerprint(),service.previewNew(owner.getId(),changed).fingerprint());jdbc.update("UPDATE weight_entry SET weight_kg=80 WHERE user_id=? AND entry_date=DATE '2020-01-01'",owner.getId());assertNotEquals(first.fingerprint(),service.previewNew(owner.getId(),input).fingerprint());}
 @Test void missingWeightAgeAndUnmappedPeriodsAreExplicitNotFailedRequests(){var mapped=period(catalogue.strengthOption(),"STRENGTH","50");var none=period(catalogue.unavailableOption(),"ACTIVITY","20");var partial=service.previewNew(owner.getId(),request(List.of(mapped,none),date));assertTrue(partial.partial());assertEquals(new BigDecimal("854.233"),partial.knownEnergyKj());assertEquals("NO_APPLICABLE_REFERENCE",partial.periods().get(1).status());assertNull(partial.periods().get(1).estimatedKj());var old=service.previewNew(owner.getId(),request(List.of(mapped),LocalDate.of(2019,12,31)));assertEquals("NO_WEIGHT",old.periods().getFirst().status());assertNull(old.knownEnergyKj());var age=service.previewNew(owner.getId(),request(List.of(mapped),LocalDate.of(2000,1,1)));assertEquals("UNSUPPORTED_REFERENCE",age.periods().getFirst().status());assertNotNull(age.periods().getFirst().unavailableReason());}
 @Test void defaultOptionAndWhitelistCannotBeBypassed(){long other=catalogue.cardioOption();var movement=new ExerciseEntryInputs.Movement(catalogue.movements().getFirst().toString(),1L,1L,null);var wrong=new ExerciseEntryInputs.PreviewPeriod(UUID.randomUUID(),"ACTIVITY",Long.toString(other),BigDecimal.TEN,Map.of(),List.of(movement));assertThrows(BusinessException.class,()->service.previewNew(owner.getId(),request(List.of(wrong),date)));var strength=new ExerciseEntryInputs.PreviewPeriod(UUID.randomUUID(),"STRENGTH",Long.toString(catalogue.strengthOption()),BigDecimal.TEN,Map.of("speedKmh",new BigDecimal("5")),List.of());assertThrows(BusinessException.class,()->service.previewNew(owner.getId(),request(List.of(strength),date)));var tooLong=List.of(period(catalogue.unavailableOption(),"ACTIVITY","1000"),period(catalogue.unavailableOption(),"ACTIVITY","500"));assertThrows(BusinessException.class,()->service.previewNew(owner.getId(),request(tooLong,date)));}
}
