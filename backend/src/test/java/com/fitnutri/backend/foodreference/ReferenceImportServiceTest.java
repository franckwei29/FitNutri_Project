package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferenceImportServiceTest {
    private final FoodDatasetRepository datasets=mock(FoodDatasetRepository.class);
    private final FoodReferenceRepository foods=mock(FoodReferenceRepository.class);
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final TransactionStatus status=mock(TransactionStatus.class);
    private final ReferenceManifestReader reader=new ReferenceManifestReader(JsonMapper.builder().build());
    private ReferenceImportService service() {
        when(transactions.getTransaction(any())).thenReturn(status);
        when(datasets.saveAndFlush(any())).thenAnswer(c->{FoodDataset d=c.getArgument(0);d.setId(9L);return d;});
        return new ReferenceImportService(reader,datasets,foods,jdbc,transactions,Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"),ZoneOffset.UTC));
    }
    @Test void validSourceHashesImportRealZeroWithOneTransactionAndClock() throws Exception {
        var input=ReferenceFixture.input("one","SYNTHETIC-1",BigDecimal.ZERO);var result=service().importManifest(input.manifest(),input.records(),input.sources());
        assertEquals("9",result.datasetId());assertEquals(1,result.importedCount());assertFalse(result.replayed());
        var captured=org.mockito.ArgumentCaptor.forClass(FoodDataset.class);verify(datasets).saveAndFlush(captured.capture());assertTrue(captured.getValue().isVerified());assertEquals("SYNTHETIC_TEST_ONLY",captured.getValue().getName());assertEquals(Instant.parse("2026-10-16T00:00:00Z"),captured.getValue().getImportedAt());verify(transactions).commit(status);
    }
    @Test void sourceOrNormalizedHashTamperingFailsBeforeAnyDatabaseInteraction() throws Exception {
        var input=ReferenceFixture.input("one","SYNTHETIC-1",BigDecimal.ONE);var importer=service();
        assertThrows(BusinessException.class,()->importer.importManifest(input.manifest(),new ByteArrayResource("{}".getBytes()),input.sources()));
        assertThrows(BusinessException.class,()->importer.importManifest(input.manifest(),input.records(),Map.of("source.txt",new ByteArrayResource("tampered".getBytes()))));
        verifyNoInteractions(datasets,foods,jdbc);verify(transactions,never()).getTransaction(any());
    }
    @Test void missingCoreNeverBecomesZeroAndInvalidRowsNeverStartTransaction() throws Exception {
        var input=ReferenceFixture.input("one","SYNTHETIC-1",null);assertThrows(BusinessException.class,()->service().importManifest(input.manifest(),input.records(),input.sources()));verifyNoInteractions(datasets,foods,jdbc);verify(transactions,never()).getTransaction(any());
    }
    @Test void sameVersionDifferentContentIsAnExplicitConflictWithoutActiveSwitch() throws Exception {
        var input=ReferenceFixture.input("one","SYNTHETIC-1",BigDecimal.ONE);var existing=new FoodDataset();existing.setId(8L);existing.setContentSha256("f".repeat(64));when(datasets.findByNameAndReleaseAndRevision("SYNTHETIC_TEST_ONLY","test","one")).thenReturn(Optional.of(existing));
        var error=assertThrows(BusinessException.class,()->service().importManifest(input.manifest(),input.records(),input.sources()));assertEquals("DATASET_VERSION_CONFLICT",error.code());verify(foods,never()).deactivateActiveVerified();verify(transactions).rollback(status);
    }
    @Test void persistenceFailureRollsBackRatherThanLeavingNewDatasetOrActiveSwitchCommitted() throws Exception {
        var input=ReferenceFixture.input("one","SYNTHETIC-1",BigDecimal.ONE);doThrow(new org.springframework.dao.DataIntegrityViolationException("synthetic failure")).when(foods).saveAllAndFlush(any());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service().importManifest(input.manifest(),input.records(),input.sources()));verify(transactions).rollback(status);verify(transactions,never()).commit(any());
    }
}
