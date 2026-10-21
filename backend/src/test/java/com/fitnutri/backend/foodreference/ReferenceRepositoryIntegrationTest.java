package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.support.TestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Verified flags here describe synthetic fixture validation only, never official production provenance. */
@SpringBootTest @ActiveProfiles("integration-test")
class ReferenceRepositoryIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();
    static {Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void isolatedCatalogue(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @Autowired private FoodReferenceRepository foods;
    @Autowired private FoodDatasetRepository datasets;
    @Autowired private ExerciseTypeRepository exercises;
    @Autowired private ApplicationContext context;
    @Autowired private PlatformTransactionManager transactions;
    private FoodDataset dataset(boolean verified) {
        var d=new FoodDataset();d.setName("SYNTHETIC_TEST_ONLY_"+UUID.randomUUID());d.setRelease("test-only");d.setRevision("one");d.setNutrientPolicy("SYNTHETIC_TEST_ONLY");d.setVerified(verified);
        if(verified){d.setContentSha256("a".repeat(64));d.setSourceUrl("https://example.test/synthetic-only");}
        return datasets.saveAndFlush(d);
    }
    private FoodReference food(FoodDataset d,String key) {
        var f=new FoodReference();f.setDataset(d);f.setPublicFoodKey(key);f.setFoodName("Synthetic");f.setOfficialName("Synthetic fixture only");f.setDisplayName("Synthetic fixture");f.setEnergyKj100g(new BigDecimal("100.000"));f.setActive(true);return foods.saveAndFlush(f);
    }
    @Test void longIdentityAndExplicitStringKeyQueriesExcludeUnverifiedCatalogues() {
        assertFalse(context.containsBean("foodReferenceDataLoader"));
        String key="synthetic-"+UUID.randomUUID(),unverifiedKey="unverified-"+UUID.randomUUID();
        var verified=food(dataset(true),key);food(dataset(false),unverifiedKey);
        assertNotNull(verified.getId());assertEquals(key,foods.findById(verified.getId()).orElseThrow().getPublicFoodKey());
        var lookup=foods.findActiveByPublicFoodKey(key).orElseThrow();assertEquals(verified.getId(),lookup.getId());assertTrue(lookup.getDataset().isVerified());
        assertTrue(foods.findActiveByPublicFoodKey(unverifiedKey).isEmpty());assertTrue(foods.findByActiveTrueAndDatasetVerifiedTrue().stream().noneMatch(f->f.getPublicFoodKey().equals(unverifiedKey)));
        assertTrue(exercises.findByActiveTrueAndDatasetVerifiedTrue().isEmpty());
    }
    @Test void activeSwitchPreservesIdWhileCoreNutrientEditsRollback() {
        var row=food(dataset(false),"immutable-"+UUID.randomUUID());long id=row.getId();
        new TransactionTemplate(transactions).executeWithoutResult(status->{var f=foods.findById(id).orElseThrow();f.setActive(false);foods.flush();});
        assertFalse(foods.findById(id).orElseThrow().isActive());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->new TransactionTemplate(transactions).executeWithoutResult(status->{var f=foods.findById(id).orElseThrow();f.setEnergyKj100g(new BigDecimal("999"));foods.flush();}));
        var preserved=foods.findById(id).orElseThrow();assertEquals(new BigDecimal("100.000"),preserved.getEnergyKj100g());assertEquals(id,preserved.getId());
    }
}
