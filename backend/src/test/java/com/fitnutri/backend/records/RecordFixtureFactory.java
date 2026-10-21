package com.fitnutri.backend.records;

import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Test-only synthetic persistence fixtures; never substituted for official source integration checks. */
public final class RecordFixtureFactory {
    private final UserRepository users;private final ProfileService profiles;private final FoodDatasetRepository foodDatasets;private final FoodReferenceRepository foods;private final ActivityDatasetRepository activityDatasets;private final ExerciseTypeRepository types;
    public RecordFixtureFactory(UserRepository users,ProfileService profiles,FoodDatasetRepository foodDatasets,FoodReferenceRepository foods,ActivityDatasetRepository activityDatasets,ExerciseTypeRepository types){this.users=users;this.profiles=profiles;this.foodDatasets=foodDatasets;this.foods=foods;this.activityDatasets=activityDatasets;this.types=types;}
    public User owner(){var u=new User();u.setEmail("record-fixture-"+UUID.randomUUID()+"@example.test");u.setDisplayName("Synthetic record fixture");u.setPasswordHash("not-a-login-credential");u=users.saveAndFlush(u);profiles.completeOnboarding(u.getId(),new CompleteOnboardingRequest(u.getVersion(),null,"Synthetic record fixture",1990,true,Sex.PREFER_NOT_TO_SAY,new BigDecimal("170"),ActivityLevel.LIGHT,null,null,EnergyUnit.KJ,HealthSafetyAnswer.NO,false,"Australia/Sydney",new CompleteOnboardingRequest.InitialWeight(LocalDate.of(2020,1,1),new BigDecimal("70"))));return u;}
    public FoodReference food(String energy){var d=new FoodDataset();d.setName("SYNTHETIC_RECORD_FIXTURE_ONLY");d.setRelease("test");d.setRevision(UUID.randomUUID().toString());d.setSourceUrl("https://example.test/synthetic-food");d.setContentSha256(hash("synthetic-food-"+energy));d.setNutrientPolicy("CORE4_REQUIRED");d.setSourceNote("Synthetic tests only; not official food facts");d.setVerified(true);foodDatasets.saveAndFlush(d);var f=new FoodReference();f.setDataset(d);f.setPublicFoodKey("synthetic-"+UUID.randomUUID());f.setFoodName("Synthetic food fixture");f.setOfficialName("Synthetic food fixture");f.setDisplayName("Synthetic food fixture");f.setEnergyKj100g(new BigDecimal(energy));f.setProteinG100g(new BigDecimal("3.1"));f.setFatG100g(new BigDecimal("0.2"));f.setCarbsG100g(new BigDecimal("34.7"));f.setActive(true);return foods.saveAndFlush(f);}
    public ExerciseType exercise(String met){var d=new ActivityDataset();d.setName("SYNTHETIC_RECORD_FIXTURE_ONLY");d.setRelease("test");d.setRevision(UUID.randomUUID().toString());d.setSourceUrl("https://example.test/synthetic-activity");d.setContentSha256(hash("synthetic-met-"+met));d.setSourceNote("Synthetic tests only; not official activity facts");d.setMinAge(19);d.setMaxAge(59);d.setVerified(true);activityDatasets.saveAndFlush(d);var t=new ExerciseType();t.setDataset(d);t.setActivityKey("synthetic-"+UUID.randomUUID());t.setDisplayName("Synthetic exercise fixture");t.setSourceActivityCode("99901");t.setSourceDescription("Synthetic activity only");t.setMetValue(new BigDecimal(met));t.setActive(true);return types.saveAndFlush(t);}
    private static String hash(String s){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
