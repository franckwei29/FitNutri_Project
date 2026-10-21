package com.fitnutri.backend.records;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.ProfileService;
import com.fitnutri.backend.submission.*;
import com.fitnutri.backend.submission.SubmissionReceiptResponse.RecordIdentity;
import jakarta.validation.Validator;
import org.springframework.transaction.annotation.Transactional;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Canonical creation entry; SubmissionService owns the transaction and user lock. */
@Service
public class FoodRecordService {
    private final SubmissionService submissions;private final CanonicalSubmissionHasher hasher;private final ProfileService profiles;private final RecordDatePolicy dates;private final FoodReferenceRepository references;private final FoodCalculationService calculator;private final FoodRecordRepository records;private final Clock clock;private final Validator validator;private final RecordMutationGuard guard;
    public FoodRecordService(SubmissionService submissions,CanonicalSubmissionHasher hasher,ProfileService profiles,RecordDatePolicy dates,FoodReferenceRepository references,FoodCalculationService calculator,FoodRecordRepository records,Clock clock,Validator validator,RecordMutationGuard guard){this.submissions=submissions;this.hasher=hasher;this.profiles=profiles;this.dates=dates;this.references=references;this.calculator=calculator;this.records=records;this.clock=clock;this.validator=validator;this.guard=guard;}
    public SubmissionReceiptResponse confirmSingleAdapter(Long owner,CreateFoodRecordRequest request){validate(request);return confirmBatch(owner,request.toBatch());}
    public SubmissionReceiptResponse confirmBatch(Long owner,FoodBatchRequest request){
        validate(request);var items=request.items().stream().map(i->new CanonicalSubmissionHasher.FoodItem(i.clientEntryId(),referenceId(i.referenceId()),i.quantityGrams(),i.mealType())).toList();
        var canonical=hasher.food(request.submissionId(),request.recordDate(),request.zone(),request.source(),items);
        return submissions.withSubmission(owner,canonical,user->{
            profiles.requireCompletedAdultSetup(owner);dates.requireDate(request.recordDate(),canonical.zone());
            var ids=items.stream().map(CanonicalSubmissionHasher.FoodItem::referenceId).distinct().sorted().toList();var loaded=references.lockAllById(ids);var byId=new HashMap<Long,FoodReference>();for(var f:loaded)byId.put(f.getId(),f);
            if(byId.size()!=ids.size()){var missing=items.stream().filter(i->!byId.containsKey(i.referenceId())).findFirst().orElseThrow();throw new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","A selected food reference no longer exists.",List.of(new FieldViolation("referenceId",missing.clientEntryId().toString(),"Choose an existing food reference.")));}
            var now=clock.instant();var rows=new ArrayList<FoodRecord>();
            for(var i:items){var reference=byId.get(i.referenceId());var snapshot=calculateItem(reference,i);var r=new FoodRecord();r.setUser(user);r.setReference(reference);r.setFoodName(reference.getDisplayName());r.setQuantityGramsSnapshot(i.quantityGrams().setScale(3,RoundingMode.UNNECESSARY));r.setEnergyKj(snapshot.energyKj());r.setProteinGramsSnapshot(snapshot.proteinGrams());r.setFatGramsSnapshot(snapshot.fatGrams());r.setCarbsGramsSnapshot(snapshot.carbsGrams());r.setMealType(i.mealType());r.setRecordDate(request.recordDate());r.setSource(request.source());r.setClientEntryId(i.clientEntryId());r.setRecordedAt(now);r.setCreatedAt(now);r.setUpdatedAt(now);r.setDataQuality(DataQuality.CURRENT);rows.add(r);}
            records.saveAllAndFlush(rows);return rows.stream().map(r->new RecordIdentity(r.getId().toString(),r.getClientEntryId(),r.getRecordDate(),r.getVersion())).toList();
        });
    }
    @Transactional public FoodRecordResponse updateOwned(Long owner,Long id,UpdateFoodRecordRequest request){
        validate(request);guard.id(id);guard.lockOwner(owner);var row=records.findOwnedActive(owner,id).orElseThrow(guard::missing);requireUngroupedForLegacyMutation(row);guard.version(row,request.expectedVersion());guard.zone(request.zone());
        Long selected=referenceId(request.referenceId());Long original=row.getReference()==null?null:row.getReference().getId();
        boolean sameReference=selected.equals(original),sameGrams=row.getQuantityGramsSnapshot().compareTo(request.quantityGrams())==0;
        if(sameReference&&sameGrams&&row.getMealType()==request.mealType()&&row.getRecordDate().equals(request.recordDate()))return FoodRecordResponse.from(row);
        profiles.requireCompletedAdultSetup(owner);dates.requireDate(request.recordDate(),request.zone());
        if(!sameReference||!sameGrams){
            var reference=sameReference?row.getReference():references.lockAllById(List.of(selected)).stream().findFirst().orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Food reference not found."));
            var snapshot=calculateReference(reference,request.quantityGrams(),row.getClientEntryId(),sameReference);
            row.setReference(reference);row.setFoodName(reference.getDisplayName());row.setQuantityGramsSnapshot(request.quantityGrams().setScale(3,RoundingMode.UNNECESSARY));row.setEnergyKj(snapshot.energyKj());row.setProteinGramsSnapshot(snapshot.proteinGrams());row.setFatGramsSnapshot(snapshot.fatGrams());row.setCarbsGramsSnapshot(snapshot.carbsGrams());row.setDataQuality(DataQuality.CURRENT);
        }
        row.setMealType(request.mealType());row.setRecordDate(request.recordDate());row.setUpdatedAt(clock.instant());records.flush();return FoodRecordResponse.from(row);
    }
    @Transactional public void softDeleteOwned(Long owner,Long id,Long expectedVersion){
        guard.id(id);guard.versionInput(expectedVersion);guard.lockOwner(owner);var row=records.findOwnedIncludingDeleted(owner,id).orElseThrow(guard::missing);requireUngroupedForLegacyMutation(row);if(row.getDeletedAt()!=null)return;guard.version(row,expectedVersion);row.setDeletedAt(clock.instant());row.setUpdatedAt(clock.instant());records.flush();
    }
    private static void requireUngroupedForLegacyMutation(FoodRecord row){if(row.getDish()!=null)throw new BusinessException(HttpStatus.CONFLICT,"GROUP_EDIT_REQUIRED","Review and edit the entire dish using its current parent version.");}
    private NutrientValues calculateItem(FoodReference reference,CanonicalSubmissionHasher.FoodItem item){return calculateReference(reference,item.quantityGrams(),item.clientEntryId(),false);}
    private NutrientValues calculateReference(FoodReference reference,java.math.BigDecimal grams,UUID clientEntryId,boolean historical){
        try{return historical?calculator.calculateHistorical(reference,grams):calculator.calculate(reference,grams);}catch(BusinessException e){
            var fields=Set.of("UNSUPPORTED_REFERENCE","REFERENCE_INACTIVE").contains(e.code())?List.of(new FieldViolation("referenceId",clientEntryId.toString(),"Review a complete verified food reference.")):e.fields().stream().map(f->new FieldViolation(f.field().equals("grams")?"quantityGrams":f.field(),clientEntryId.toString(),f.message())).toList();
            throw new BusinessException(e.status(),e.code(),e.getMessage(),fields);
        }
    }
    private static Long referenceId(String value){try{long id=Long.parseLong(value);if(id<1)throw new NumberFormatException();return id;}catch(NumberFormatException e){throw BusinessException.invalid("referenceId","Choose a valid immutable food reference identity.");}}
    private static FieldViolation violation(Object request,String path){
        if(request instanceof FoodBatchRequest batch&&batch.items()!=null){var match=java.util.regex.Pattern.compile("^items\\[(\\d+)]\\.(.+)$").matcher(path);if(match.matches()){int index=Integer.parseInt(match.group(1));if(index<batch.items().size()){var item=batch.items().get(index);return new FieldViolation(match.group(2),item==null||item.clientEntryId()==null?null:item.clientEntryId().toString(),"Enter a valid value for this field.");}}}
        return new FieldViolation(path,null,"Enter a valid value for this field.");
    }
    private void validate(Object request){if(request==null)throw BusinessException.invalid("request","Enter a complete reviewed food submission.");var errors=validator.validate(request);if(!errors.isEmpty())throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->violation(request,e.getPropertyPath().toString())).toList());}
}
