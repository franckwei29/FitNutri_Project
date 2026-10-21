package com.fitnutri.backend.support;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
/** Synthetic values only; never an official import, production seed or provider result. */
public final class FoodV2TestFixtures {
 private FoodV2TestFixtures(){}
 public static User owner(UserRepository users,ProfileService profiles){var user=new User();user.setEmail("food-v2-"+UUID.randomUUID()+"@example.test");user.setDisplayName("Synthetic V2 owner");user.setPasswordHash("synthetic-only");user=users.saveAndFlush(user);profiles.completeOnboarding(user.getId(),new CompleteOnboardingRequest(user.getVersion(),null,"Synthetic V2 owner",1990,true,Sex.PREFER_NOT_TO_SAY,new BigDecimal("170"),ActivityLevel.LIGHT,null,null,EnergyUnit.KJ,HealthSafetyAnswer.NO,false,"Australia/Sydney",new CompleteOnboardingRequest.InitialWeight(LocalDate.of(2020,1,1),new BigDecimal("70"))));return user;}
 public static FoodReference reference(FoodDatasetRepository datasets,FoodReferenceRepository references,String energy){var dataset=new FoodDataset();dataset.setName("SYNTHETIC_V2_ONLY");dataset.setRelease("test");dataset.setRevision(UUID.randomUUID().toString());dataset.setSourceUrl("https://example.test/synthetic");dataset.setContentSha256("a".repeat(64));dataset.setNutrientPolicy("SYNTHETIC_CORE4");dataset.setSourceNote("Synthetic fixture only, not official food data");dataset.setVerified(true);datasets.saveAndFlush(dataset);var ref=new FoodReference();ref.setDataset(dataset);ref.setPublicFoodKey("SYNTHETIC-"+UUID.randomUUID());ref.setDisplayName("Synthetic V2 food");ref.setOfficialName("Synthetic V2 food");ref.setEnergyKj100g(new BigDecimal(energy));ref.setProteinG100g(BigDecimal.ONE);ref.setFatG100g(BigDecimal.ZERO);ref.setCarbsG100g(BigDecimal.ONE);ref.setActive(true);return references.saveAndFlush(ref);}
}
