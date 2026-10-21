package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.records.ExerciseCatalog;
import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("integration-test")
class ActivityCatalogueIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @Autowired ActivityReferenceImportService importer;@Autowired ActivityDatasetRepository datasets;@Autowired ExerciseTypeRepository types;@Autowired ExerciseCatalog catalogue;
    private ActivityReferenceImportService.ImportReport load(ActivityFixture.Input i){return importer.importActivityManifest(i.manifest(),i.records(),i.sources());}
    private String unique(){return "test-"+UUID.randomUUID();}
    private ExerciseTypeResponse selected(String key){return catalogue.listActiveTypes().stream().filter(t->t.activityKey().equals(key)).findFirst().orElseThrow();}
    @Test void versionActivationRetainsImmutableOldMetAndReplayCannotReactivateOldVersion() throws Exception {
        String key=unique(),revision=unique();var input=ActivityFixture.input(revision,key,"3.5");var first=load(input);var old=selected(key);assertEquals(1,first.importedCount());assertTrue(load(input).replayed());load(ActivityFixture.input(unique(),key,"4.5"));var current=selected(key);assertNotEquals(old.id(),current.id());assertEquals(new BigDecimal("4.50"),current.metValue());assertEquals(19,current.dataset().minAge());assertEquals(59,current.dataset().maxAge());assertEquals("99901",current.sourceActivityCode());assertFalse(catalogue.requireImmutable(Long.valueOf(old.id())).isActive());assertEquals(new BigDecimal("3.50"),catalogue.requireImmutable(Long.valueOf(old.id())).getMetValue());assertTrue(load(input).replayed());assertEquals(current.id(),selected(key).id());
    }
    @Test void sameVersionChangedMetConflictsInsteadOfOverwritingReference() throws Exception {
        String key=unique(),revision=unique();load(ActivityFixture.input(revision,key,"3.5"));String id=selected(key).id();assertEquals("DATASET_VERSION_CONFLICT",assertThrows(BusinessException.class,()->load(ActivityFixture.input(revision,key,"4.5"))).code());assertEquals(id,selected(key).id());assertEquals(new BigDecimal("3.50"),selected(key).metValue());
    }
    @Test void badMetFailsBeforeDatasetCreationOrActivation() throws Exception {
        String key=unique(),revision=unique();load(ActivityFixture.input(unique(),key,"3.5"));String id=selected(key).id();long count=datasets.count();assertThrows(BusinessException.class,()->load(ActivityFixture.input(revision,key,"0")));assertEquals(count,datasets.count());assertEquals(id,selected(key).id());
    }
    @Test void lateActiveKeyConflictRollsBackNewDatasetAndRestoresOldSelection() throws Exception {
        String oldKey=unique(),collision=unique(),revision=unique();load(ActivityFixture.input(unique(),oldKey,"3.5"));String old=selected(oldKey).id();
        try(var c=DB.connect();var s=c.prepareStatement("INSERT INTO exercise_type(dataset_id,activity_key,display_name,met_value,is_active) SELECT id,?,'Synthetic blocker',3.5,true FROM activity_dataset WHERE name='LEGACY_CONFIG_UNVERIFIED'")){s.setString(1,collision);s.executeUpdate();}
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->load(ActivityFixture.input(revision,collision,"4.5")));assertTrue(datasets.findByNameAndReleaseAndRevision("SYNTHETIC_ACTIVITY_TEST_ONLY","test",revision).isEmpty());assertTrue(catalogue.requireImmutable(Long.valueOf(old)).isActive());
    }
    @Test void concurrentIdenticalImportCreatesOneVersionAndOneReplay() throws Exception {
        var input=ActivityFixture.input(unique(),unique(),"3.5");var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){Callable<Integer> work=()->{start.await();return load(input).importedCount();};var a=pool.submit(work);var b=pool.submit(work);start.countDown();var counts=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));Collections.sort(counts);assertEquals(List.of(0,1),counts);}
    }
    @Test void detachedReferenceIncludesFullProvenanceAndDatabaseRejectsImmutableMetUpdates() throws Exception {
        String key=unique();load(ActivityFixture.input(unique(),key,"3.5"));var dto=selected(key);var detached=catalogue.requireImmutable(Long.valueOf(dto.id()));assertTrue(detached.getDataset().isVerified());assertEquals(59,detached.getDataset().getMaxAge());
        try(var c=DB.connect();var s=c.prepareStatement("UPDATE exercise_type SET met_value=4 WHERE id=?")){s.setLong(1,Long.parseLong(dto.id()));assertThrows(java.sql.SQLException.class,s::executeUpdate);}assertEquals(new BigDecimal("3.50"),selected(key).metValue());
    }
}
