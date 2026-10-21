package com.fitnutri.backend.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.summary.SummaryService;
import com.fitnutri.backend.support.PostgresIntegrationSupport;
import com.fitnutri.backend.user.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Real owned PostgreSQL/services; actual QA HTTP/Session composition is separately required in A08. */
class QaContextEligibilityIntegrationTest extends PostgresIntegrationSupport {
 @Autowired UserRepository users;@Autowired ProfileService profileService;@Autowired UserProfileRepository profiles;@Autowired WeightEntryRepository weights;@Autowired FoodDatasetRepository datasets;@Autowired FoodReferenceRepository references;@Autowired ActivityDatasetRepository activityDatasets;@Autowired ExerciseTypeRepository types;@Autowired FoodRecordRepository foods;@Autowired FoodRecordService foodService;@Autowired SummaryService summaries;@Autowired QaContextBuilder context;@Autowired QaEligibilityService eligibility;@Autowired PlatformTransactionManager transactions;
 RecordFixtureFactory fixtures;
 @BeforeEach void fixtures(){fixtures=new RecordFixtureFactory(users,profileService,datasets,references,activityDatasets,types);}
 ProfileResponse settings(User u,boolean consent,BigDecimal target){var p=profileService.getCurrent(u.getId());return profileService.updateSettings(u.getId(),new ProfileSettingsRequest(p.user().version(),p.profileVersion(),p.user().displayName(),p.birthYear(),p.adultConfirmed(),p.sex(),BigDecimal.valueOf(p.heightCm()),p.activityLevel(),p.goalType(),target,p.energyUnit(),p.healthSafetyAnswer(),consent,"Australia/Sydney"));}
 FoodRecord legacy(User owner,int day,String energy){var row=new FoodRecord();row.setUser(owner);row.setFoodName("private synthetic food name never sent");row.setSource(RecordSource.PHOTO);row.setRecordDate(LocalDate.of(2020,1,day));row.setRecordedAt(Instant.parse("2026-10-16T10:00:00Z"));row.setClientEntryId(UUID.randomUUID());row.setQuantityGramsSnapshot(new BigDecimal("100.000"));row.setMealType(MealType.LUNCH);row.setEnergyKj(energy==null?null:new BigDecimal(energy));row.setCalories(12345.0);return foods.saveAndFlush(row);}
 @Test void consentRevocationDoesNotDisableOrdinaryRecordCreationOrFactualSummaries(){var owner=fixtures.owner();assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->eligibility.requireEligibility(owner.getId())).code());settings(owner,true,null);eligibility.requireEligibility(owner.getId());settings(owner,false,null);assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->eligibility.requireEligibility(owner.getId())).code());var ref=fixtures.food("100");foodService.confirmBatch(owner.getId(),new FoodBatchRequest(UUID.randomUUID(),LocalDate.of(2020,1,2),"Australia/Sydney",RecordSource.MANUAL,List.of(new FoodBatchRequest.Item(UUID.randomUUID(),ref.getId().toString(),new BigDecimal("100"),MealType.LUNCH))));assertEquals(new BigDecimal("100.000"),summaries.daily(owner.getId(),LocalDate.of(2020,1,2),"Australia/Sydney").day().intake().knownSum());}
 @Test void freshEligibilitySeesCommittedWithdrawalDespiteAnOuterRepeatableReadCachedProfile(){var owner=fixtures.owner();settings(owner,true,null);var outer=new TransactionTemplate(transactions);outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);var separate=new TransactionTemplate(transactions);separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);outer.executeWithoutResult(status->{var cached=profiles.findById(owner.getId()).orElseThrow();assertTrue(cached.getHealthSafetyConsent());eligibility.requireEligibility(owner.getId());separate.executeWithoutResult(update->{var p=profiles.findById(owner.getId()).orElseThrow();p.setHealthSafetyConsent(false);p.setAiQaEligible(true);profiles.saveAndFlush(p);});assertTrue(cached.getHealthSafetyConsent());assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->eligibility.requireEligibility(owner.getId())).code());status.setRollbackOnly();});assertFalse(profileService.getCurrent(owner.getId()).qaConsentGranted());}
 @Test void allowedFactsUseOnlyCurrentOwnerDateAggregatesAndExcludePrivateProfileWeightsNamesAndArbitraryFacts()throws Exception{
  var owner=fixtures.owner();var other=fixtures.owner();settings(owner,true,new BigDecimal("8400"));legacy(owner,1,"100");legacy(owner,2,null);legacy(owner,3,"0");legacy(owner,4,"777777");legacy(other,2,"999999");var deleted=legacy(owner,2,"888888");deleted.setDeletedAt(Instant.now());foods.saveAndFlush(deleted);weights.saveAndFlush(WeightEntry.of(owner,new BigDecimal("72"),LocalDate.of(2020,1,3)));
  var range=new QaRequest.RecordRange(LocalDate.of(2020,1,1),LocalDate.of(2020,1,3));assertTrue(summaries.period(owner.getId(),range.from(),range.to(),"Australia/Sydney").facts().stream().anyMatch(f->f.code().equals("RECORDED_WEIGHT_CHANGE")));
  var facts=context.buildAllowedFacts(owner.getId(),range,"Australia/Sydney");assertEquals(3,facts.days().size());assertEquals(new BigDecimal("100.000"),facts.totals().intakeKj().knownTotal());assertEquals(3,facts.totals().intakeKj().recordCount());assertEquals(2,facts.totals().intakeKj().knownValueCount());assertEquals(1,facts.totals().intakeKj().missingValueCount());assertNull(facts.days().get(1).recorded().intakeKj().knownTotal());assertEquals(new BigDecimal("0.000"),facts.days().get(2).recorded().intakeKj().knownTotal());assertEquals(new BigDecimal("8400.000"),facts.currentTarget().dailyTargetKj());assertTrue(facts.currentTarget().currentReferenceOnly());
  var json=new ObjectMapper();String serialized=json.writeValueAsString(facts);var root=json.readTree(serialized);assertEquals(Set.of("schemaVersion","rangeStart","rangeEnd","calendarDays","days","totals","currentTarget","notices"),json.convertValue(root,Map.class).keySet());for(String forbidden:List.of(owner.getEmail(),other.getEmail(),"password","displayName","healthSafety","qaConsent","birthYear","weightKg","changeKg","fromKg","toKg","userId","private synthetic food","999999","888888","777777"))assertFalse(serialized.contains(forbidden),forbidden);assertTrue(serialized.length()<64000);
 }
 @Test void emptySelectedRangeHasNoInventedTotalsOrTargetsAndThirtyOneDaysAreAllowed(){var owner=fixtures.owner();var facts=context.buildAllowedFacts(owner.getId(),new QaRequest.RecordRange(LocalDate.of(2019,12,1),LocalDate.of(2019,12,31)),"Australia/Sydney");assertEquals(31,facts.calendarDays());assertEquals(31,facts.days().size());assertNull(facts.totals().intakeKj().knownTotal());assertNull(facts.totals().activityMinutes());assertNull(facts.currentTarget());assertEquals(0,facts.totals().foodCount());assertTrue(facts.notices().stream().anyMatch(s->s.contains("Unrecorded")));}
}
