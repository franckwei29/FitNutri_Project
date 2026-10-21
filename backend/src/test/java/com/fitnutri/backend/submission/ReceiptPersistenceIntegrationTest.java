package com.fitnutri.backend.submission;

import com.fitnutri.backend.records.*;
import com.fitnutri.backend.support.PostgresIntegrationSupport;
import com.fitnutri.backend.user.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class ReceiptPersistenceIntegrationTest extends PostgresIntegrationSupport {
    @Autowired private UserRepository users;
    @Autowired private SubmissionReceiptRepository receipts;
    @Autowired private FoodRecordRepository foods;
    @Autowired private PlatformTransactionManager transactions;
    private User owner() {var u=new User();u.setEmail("receipt-"+UUID.randomUUID()+"@example.test");u.setDisplayName("Synthetic");u.setPasswordHash("test-only-hash");return users.saveAndFlush(u);}
    @Test void jsonbMinimalProofCommitsOnceAndOwnerLookupDoesNotExposeItToAnotherAccount() {
        User user=owner();UUID submission=UUID.randomUUID();long id=new TransactionTemplate(transactions).execute(status->{
            var receipt=new SubmissionReceipt();receipt.setUser(user);receipt.setSubmissionId(submission);receipt.setOperation(SubmissionOperation.FOOD_BATCH);receipt.setRequestSha256("b".repeat(64));receipts.saveAndFlush(receipt);
            receipt.setResultJson(Map.of("records",List.of(Map.of("id","9007199254740993","clientEntryId",UUID.randomUUID().toString(),"recordDate","2020-01-01","versionAtCreation",0))));receipt.setCommittedAt(Instant.parse("2026-10-16T00:00:00Z"));receipts.flush();return receipt.getId();
        });
        var saved=receipts.findByUserIdAndSubmissionId(user.getId(),submission).orElseThrow();assertEquals(id,saved.getId());assertNotNull(saved.getCommittedAt());assertTrue(saved.getResultJson().get("records") instanceof List<?>);assertTrue(receipts.findByUserIdAndSubmissionId(owner().getId(),submission).isEmpty());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->new TransactionTemplate(transactions).executeWithoutResult(status->{receipts.findById(id).orElseThrow().setRequestSha256("c".repeat(64));receipts.flush();}));assertEquals("b".repeat(64),receipts.findById(id).orElseThrow().getRequestSha256());
    }
    private FoodRecord food(User user,UUID clientId) {var r=new FoodRecord();r.setUser(user);r.setFoodName("Synthetic legacy");r.setQuantityGrams(100.0);r.setQuantityGramsSnapshot(new java.math.BigDecimal("100.000"));r.setRecordDate(java.time.LocalDate.of(2020,1,1));r.setMealType(MealType.LUNCH);r.setSource(RecordSource.MANUAL);r.setRecordedAt(Instant.parse("2020-01-01T00:00:00Z"));r.setClientEntryId(clientId);return r;}
    @Test void softDeletedEntityIsHiddenFromExistingOwnerQueriesButRetainsRetryIdentity() {
        User user=owner();UUID clientId=UUID.randomUUID();var saved=foods.saveAndFlush(food(user,clientId));assertEquals(1,foods.findByUserIdOrderByRecordedAtDesc(user.getId()).size());
        saved.setDeletedAt(Instant.parse("2026-10-16T00:00:00Z"));saved=foods.saveAndFlush(saved);assertEquals(1,saved.getVersion());assertTrue(foods.findByUserIdOrderByRecordedAtDesc(user.getId()).isEmpty());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->foods.saveAndFlush(food(user,clientId)));assertTrue(foods.findById(saved.getId()).orElseThrow().getDeletedAt()!=null);
    }
}
