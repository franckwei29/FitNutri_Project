package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("integration-test")
class FoodCatalogueIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();static {Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @Autowired private ReferenceImportService importer;
    @Autowired private FoodReferenceService service;
    @Autowired private FoodDatasetRepository datasets;
    @Autowired private FoodReferenceRepository foods;
    private ReferenceImportService.ImportReport load(ReferenceFixture.Input i) {return importer.importManifest(i.manifest(),i.records(),i.sources());}
    private String unique() {return UUID.randomUUID().toString();}
    @Test void versionSwitchKeepsImmutableOldIdentityAndOldReplayCannotReactivateIt() throws Exception {
        String prefix=unique(),key="KEY-"+prefix;var one=ReferenceFixture.input(prefix+"-one",key,new BigDecimal("100"));var first=load(one);var old=service.getActiveByPublicKey(key);assertEquals(1,first.importedCount());assertEquals(old.referenceId(),service.getImmutableReference(Long.valueOf(old.referenceId())).referenceId());
        assertTrue(load(one).replayed());var next=load(ReferenceFixture.input(prefix+"-two",key,new BigDecimal("200")));var current=service.getActiveByPublicKey(key);assertNotEquals(first.datasetId(),next.datasetId());assertNotEquals(old.referenceId(),current.referenceId());assertFalse(service.getImmutableReference(Long.valueOf(old.referenceId())).active());assertEquals(new BigDecimal("100.000"),service.getImmutableReference(Long.valueOf(old.referenceId())).per100g().energyKj());
        assertTrue(load(one).replayed());assertEquals(current.referenceId(),service.getActiveByPublicKey(key).referenceId());
    }
    @Test void databasePagedSearchDistinguishesSameNameKeysAndEscapesWildcardInput() throws Exception {
        String prefix=unique();var input=ReferenceFixture.transform(ReferenceFixture.input(prefix,"KEY-A-"+prefix,BigDecimal.ONE),node->{
            var rows=(ArrayNode)node.get("foods");var second=((ObjectNode)rows.get(0)).deepCopy();second.put("publicFoodKey","KEY-B-"+prefix);second.put("surveyId","SURVEY-B-"+prefix);rows.add(second);
            var third=second.deepCopy();third.put("publicFoodKey","KEY-C-"+prefix);third.put("surveyId","SURVEY-C-"+prefix);third.put("displayName","100%_synthetic");rows.add(third);
        });load(input);
        var a=service.searchActive("SAME synthetic",0,1);var b=service.searchActive("same synthetic",1,1);assertEquals(2,a.totalMatches());assertTrue(a.hasMore());assertFalse(b.hasMore());assertNotEquals(a.results().getFirst().referenceId(),b.results().getFirst().referenceId());assertNotEquals(a.results().getFirst().publicFoodKey(),b.results().getFirst().publicFoodKey());
        assertEquals(1,service.searchActive("%_",0,20).totalMatches());assertEquals(0,service.searchActive("no-such-food",0,20).totalMatches());assertTrue(service.searchActive("",0,20).results().isEmpty());
    }
    @Test void lateUniqueConstraintFailureRollsBackNewDatasetAndOldActiveSwitch() throws Exception {
        String prefix=unique(),key="COLLISION-"+prefix;load(ReferenceFixture.input(prefix+"-one","OLD-"+prefix,BigDecimal.ONE));String old=service.getActiveByPublicKey("OLD-"+prefix).referenceId();
        try(var c=DB.connect();var s=c.prepareStatement("INSERT INTO food_reference(dataset_id,public_food_key,food_name,display_name,is_active) SELECT id,?,'Synthetic blocker','Synthetic blocker',true FROM food_dataset WHERE name='LEGACY_AFCD_UNVERIFIED'")){s.setString(1,key);s.executeUpdate();}
        var input=ReferenceFixture.input(prefix+"-two",key,BigDecimal.ONE);assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->load(input));
        assertTrue(datasets.findByNameAndReleaseAndRevision("SYNTHETIC_TEST_ONLY","test",prefix+"-two").isEmpty());assertTrue(service.getImmutableReference(Long.valueOf(old)).active());
    }
    @Test void incompleteCoreReferenceIsNotSelectableButHistoricalDetailsKeepNull() throws Exception {
        String prefix=unique();var result=load(ReferenceFixture.input(prefix,"READY-"+prefix,BigDecimal.ONE));
        try(var c=DB.connect();var s=c.prepareStatement("INSERT INTO food_reference(dataset_id,public_food_key,food_name,display_name,is_active) VALUES(? ,?,'Synthetic incomplete','Synthetic incomplete',true) RETURNING id")) {
            s.setLong(1,Long.parseLong(result.datasetId()));s.setString(2,"MISSING-"+prefix);try(var r=s.executeQuery()){r.next();assertNull(service.getImmutableReference(r.getLong(1)).per100g().energyKj());}
        }
        assertEquals(0,service.searchActive("Synthetic incomplete",0,20).totalMatches());
    }
    @Test void concurrentSameManifestHasOneImportAndOneReplay() throws Exception {
        var input=ReferenceFixture.input(unique(),"CONCURRENT-"+unique(),BigDecimal.ONE);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> work=()->{start.await();return load(input).importedCount();};var a=pool.submit(work);var b=pool.submit(work);start.countDown();var counts=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));Collections.sort(counts);assertEquals(List.of(0,1),counts);
        }
    }
    @Test void detachedImmutableReferenceCarriesItsDatasetForAuthoritativePreview() throws Exception {
        String key="DETACHED-"+unique();load(ReferenceFixture.input(unique(),key,new BigDecimal("123.456")));
        var selected=service.getActiveByPublicKey(key);var detached=service.requireImmutable(Long.valueOf(selected.referenceId()));
        var preview=new FoodCalculationService().preview(detached,new BigDecimal("0.334"));
        assertEquals(new BigDecimal("0.412"),preview.nutrients().energyKj());assertTrue(preview.reference().dataset().verified());assertEquals(selected.referenceId(),preview.reference().referenceId());
    }
    @Test void sameVersionDifferentHashDoesNotReplaceCommittedContent() throws Exception {
        String prefix=unique(),key="STABLE-"+prefix;load(ReferenceFixture.input(prefix,key,new BigDecimal("100")));String id=service.getActiveByPublicKey(key).referenceId();
        var error=assertThrows(BusinessException.class,()->load(ReferenceFixture.input(prefix,key,new BigDecimal("200"))));assertEquals("DATASET_VERSION_CONFLICT",error.code());assertEquals(id,service.getActiveByPublicKey(key).referenceId());assertEquals(new BigDecimal("100.000"),service.getActiveByPublicKey(key).per100g().energyKj());
    }
}
