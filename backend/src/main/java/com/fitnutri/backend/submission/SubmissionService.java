package com.fitnutri.backend.submission;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.submission.SubmissionReceiptResponse.RecordIdentity;
import com.fitnutri.backend.user.*;
import com.fitnutri.backend.records.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import tools.jackson.databind.ObjectMapper;

/** The outer transaction returns only after records and minimal creation proof commit successfully.
 * Creation callbacks perform only local database validation/calculation/writes in this transaction; never external model calls.
 */
@Service
public class SubmissionService {
    private final UserRepository users;private final SubmissionReceiptRepository receipts;private final TransactionTemplate transaction;private final Clock clock;private final ObjectMapper json;private final FoodRecordRepository foods;private final ExerciseRecordRepository exercises;private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;
    public SubmissionService(UserRepository users,SubmissionReceiptRepository receipts,PlatformTransactionManager manager,Clock clock,ObjectMapper json,FoodRecordRepository foods,ExerciseRecordRepository exercises,org.springframework.jdbc.core.JdbcTemplate jdbc){this.users=users;this.receipts=receipts;transaction=new TransactionTemplate(manager);this.clock=clock;this.json=json;this.foods=foods;this.exercises=exercises;this.jdbc=new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc);}
    private record Proof(List<RecordIdentity> records) {}
    public SubmissionReceiptResponse findReceipt(Long owner,UUID submissionId){return receipts.findByUserIdAndSubmissionId(owner,submissionId).filter(r->r.getCommittedAt()!=null&&(r.getOperation()==SubmissionOperation.FOOD_BATCH||r.getOperation()==SubmissionOperation.EXERCISE_CREATE)).map(r->response(r,true)).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","No committed receipt is currently visible. An in-flight request may still commit; retain the same submission identity."));}
    public SubmissionReceiptResponse withSubmission(Long owner,CanonicalSubmissionHasher.Submission input,Function<User,List<RecordIdentity>> create){
        // Prevent a caller from accidentally joining an outer transaction and returning proof before that transaction commits.
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("SubmissionService must own the outer creation transaction.");
        return transaction.execute(status->{
            var user=users.lockById(owner).orElseThrow(SubmissionService::unauthenticated);if(!user.isEnabled())throw unauthenticated();if(user.getRole()!=User.Role.USER)throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");
            var existing=receipts.findByUserIdAndSubmissionId(owner,input.submissionId());
            if(existing.isPresent()){var r=existing.get();if(r.getOperation()!=input.operation()||!r.getRequestSha256().equals(input.requestSha256()))throw new BusinessException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","This submission identity was used for different inputs. Review the original receipt.");if(r.getCommittedAt()==null)throw new IllegalStateException("Committed receipt is incomplete.");return response(r,true);}
            var receipt=new SubmissionReceipt();receipt.setUser(user);receipt.setSubmissionId(input.submissionId());receipt.setOperation(input.operation());receipt.setRequestSha256(input.requestSha256());receipt.setCreatedAt(clock.instant());receipts.saveAndFlush(receipt);
            if(new HashSet<>(input.clientEntryIds()).size()!=input.clientEntryIds().size())throw BusinessException.invalid("items","Every entry in a submission must have a distinct clientEntryId.");
            if(foods.existsByUserIdAndClientEntryIdIn(owner,input.clientEntryIds())||exercises.existsByUserIdAndClientEntryIdIn(owner,input.clientEntryIds()))throw new BusinessException(HttpStatus.CONFLICT,"CLIENT_ENTRY_CONFLICT","An entry identity already belongs to another submission, including any deleted record. Review the original creation proof.");
            var created=List.copyOf(create.apply(user));validateProof(input,created);
            // JSONB/Hibernate deep copies decode small JSON numbers as Integer: preserve that representation
            // (creation version is always zero), otherwise a Long0 map can appear dirty again after final flush.
            var rows=created.stream().map(r->Map.<String,Object>of("id",r.id(),"clientEntryId",r.clientEntryId().toString(),"recordDate",r.recordDate().toString(),"versionAtCreation",Math.toIntExact(r.versionAtCreation()))).toList();receipt.setResultJson(Map.of("records",rows));receipt.setCommittedAt(clock.instant());receipts.saveAndFlush(receipt);return new SubmissionReceiptResponse(input.submissionId(),input.operation(),receipt.getCommittedAt(),false,created);
        });
    }
    public SubmissionReceiptV2Response findReceiptV2(Long owner,UUID id){return receipts.findByUserIdAndSubmissionId(owner,id).filter(r->r.getCommittedAt()!=null&&(r.getOperation()==SubmissionOperation.FOOD_BATCH_V2||r.getOperation()==SubmissionOperation.EXERCISE_ENTRY_V2)).map(r->responseV2(r,true)).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","No committed V2 receipt is currently visible. An in-flight request may still commit; retain its identity."));}
    public SubmissionReceiptV2Response withSubmissionV2(Long owner,CanonicalEntryHasherV2.Submission input,Function<User,SubmissionReceiptV2Response.Proof> create){
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("SubmissionService must own the outer creation transaction.");
        return transaction.execute(status->{
            var user=users.lockById(owner).orElseThrow(SubmissionService::unauthenticated);if(!user.isEnabled())throw unauthenticated();if(user.getRole()!=User.Role.USER)throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");
            var existing=receipts.findByUserIdAndSubmissionId(owner,input.submissionId());
            if(existing.isPresent()){var r=existing.get();if(r.getOperation()!=input.operation()||!r.getRequestSha256().equals(input.requestSha256()))throw new BusinessException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","This submission identity was used for different inputs. Review the original receipt.");if(r.getCommittedAt()==null)throw new IllegalStateException("Committed receipt is incomplete.");return responseV2(r,true);}
            var receipt=new SubmissionReceipt();receipt.setUser(user);receipt.setSubmissionId(input.submissionId());receipt.setOperation(input.operation());receipt.setRequestSha256(input.requestSha256());receipt.setCreatedAt(clock.instant());receipts.saveAndFlush(receipt);
            requireUnusedEntryIdentities(owner,input.allEntryIds());
            var proof=create.apply(user);SubmissionReceiptV2Response.validateTypedProof(input,proof);
            // Round-trip uses the same Integer0 JSON representation as JSONB/Hibernate, retaining immutable proof guards.
            @SuppressWarnings("unchecked") Map<String,Object> stored=json.convertValue(proof,Map.class);receipt.setResultJson(stored);receipt.setCommittedAt(clock.instant());receipts.saveAndFlush(receipt);return responseV2(receipt,false);
        });
    }
    /** Caller must hold the same owner lock; includes soft-deleted identities across all entry domains. */
    public void requireUnusedEntryIdentities(Long owner,List<UUID> ids){
        if(ids.isEmpty())return;
        var args=new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("owner",owner).addValue("ids",ids);
        boolean collision=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM food_record WHERE user_id=:owner AND client_entry_id IN (:ids) UNION ALL SELECT 1 FROM exercise_record WHERE user_id=:owner AND client_entry_id IN (:ids) UNION ALL SELECT 1 FROM food_dish_record WHERE user_id=:owner AND client_entry_id IN (:ids) UNION ALL SELECT 1 FROM workout_record WHERE user_id=:owner AND client_entry_id IN (:ids))",args,Boolean.class));
        if(collision)throw new BusinessException(HttpStatus.CONFLICT,"CLIENT_ENTRY_CONFLICT","An entry identity already belongs to a recorded submission, including deleted records.");
    }
    private SubmissionReceiptV2Response responseV2(SubmissionReceipt r,boolean replayed){var proof=json.convertValue(r.getResultJson(),SubmissionReceiptV2Response.Proof.class);return new SubmissionReceiptV2Response(2,r.getSubmissionId(),r.getOperation(),r.getCommittedAt(),replayed,proof.roots(),proof.leaves());}
    private static void validateProof(CanonicalSubmissionHasher.Submission input,List<RecordIdentity> records){
        var expected=new HashSet<>(input.clientEntryIds());var actual=new HashSet<UUID>();var ids=new HashSet<String>();
        if(records.isEmpty()||records.size()!=expected.size())throw new IllegalStateException("Creation result does not match submitted entries.");
        for(var r:records)if(r==null||r.id()==null||!r.id().matches("[1-9][0-9]*")||!ids.add(r.id())||r.clientEntryId()==null||!actual.add(r.clientEntryId())||!input.recordDate().equals(r.recordDate())||r.versionAtCreation()!=0)throw new IllegalStateException("Creation result contains invalid identity proof.");
        if(!expected.equals(actual))throw new IllegalStateException("Creation result has unexpected entries.");
    }
    private SubmissionReceiptResponse response(SubmissionReceipt r,boolean replayed){return new SubmissionReceiptResponse(r.getSubmissionId(),r.getOperation(),r.getCommittedAt(),replayed,json.convertValue(r.getResultJson(),Proof.class).records());}
    private static BusinessException unauthenticated(){return new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue.");}
}
