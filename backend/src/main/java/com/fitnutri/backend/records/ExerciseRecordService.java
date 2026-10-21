package com.fitnutri.backend.records;

import com.fitnutri.backend.activity.ExerciseTypeRepository;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.submission.*;
import com.fitnutri.backend.submission.SubmissionReceiptResponse.RecordIdentity;
import jakarta.validation.Validator;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseRecordService {
    private final SubmissionService submissions;private final CanonicalSubmissionHasher hasher;private final ExerciseEstimateService estimates;private final ExerciseTypeRepository types;private final ExerciseRecordRepository records;private final Clock clock;private final Validator validator;private final RecordMutationGuard guard;
    public ExerciseRecordService(SubmissionService submissions,CanonicalSubmissionHasher hasher,ExerciseEstimateService estimates,ExerciseTypeRepository types,ExerciseRecordRepository records,Clock clock,Validator validator,RecordMutationGuard guard){this.submissions=submissions;this.hasher=hasher;this.estimates=estimates;this.types=types;this.records=records;this.clock=clock;this.validator=validator;this.guard=guard;}
    public SubmissionReceiptResponse confirmExercise(Long owner,CreateExerciseRecordRequest request){
        validate(request);Long typeId=id(request.exerciseTypeId());var canonical=hasher.exercise(request.submissionId(),request.recordDate(),request.zone(),request.source(),request.clientEntryId(),typeId,request.durationMinutes(),request.calculationFingerprint());
        return submissions.withSubmission(owner,canonical,user->{
            var type=types.lockById(typeId).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Activity reference not found."));var estimate=estimates.estimateLocked(owner,type,request.recordDate(),request.durationMinutes(),canonical.zone());
            if(!estimate.calculationFingerprint().equals(request.calculationFingerprint()))throw new CalculationChangedException(estimate);
            var now=clock.instant();var r=new ExerciseRecord();r.setUser(user);r.setReference(type);r.setExerciseType(type.getDisplayName());r.setDurationMinutes(request.durationMinutes().setScale(2,RoundingMode.UNNECESSARY));r.setIntensity(null);r.setRecordDate(request.recordDate());r.setClientEntryId(request.clientEntryId());r.setSource(request.source());r.setRecordedAt(now);r.setCreatedAt(now);r.setUpdatedAt(now);r.setDataQuality(DataQuality.CURRENT);r.setEstimatedKj(estimate.estimatedKj());r.setEstimateStatus(estimate.estimateStatus());r.setWeightKgUsed(estimate.weightKgUsed());r.setWeightDateUsed(estimate.weightDateUsed());r.setMetUsed(estimate.metUsed());r.setFormulaVersion(estimate.formulaVersion());records.saveAndFlush(r);return List.of(new RecordIdentity(r.getId().toString(),r.getClientEntryId(),r.getRecordDate(),r.getVersion()));
        });
    }
    @Transactional public ExerciseRecordResponse updateOwned(Long owner,Long id,UpdateExerciseRecordRequest request){
        validateUpdate(request);guard.id(id);guard.lockOwner(owner);var row=records.findOwnedActive(owner,id).orElseThrow(guard::missing);requireStandalonePeriod(row);guard.version(row,request.expectedVersion());guard.zone(request.zone());
        Long selected=id(request.exerciseTypeId()),original=row.getReference()==null?null:row.getReference().getId();boolean sameReference=selected.equals(original);
        if(sameReference&&row.getDurationMinutes().compareTo(request.durationMinutes())==0&&row.getRecordDate().equals(request.recordDate()))return ExerciseRecordResponse.from(row);
        if(request.calculationFingerprint()==null||request.calculationFingerprint().isBlank())throw BusinessException.invalid("calculationFingerprint","Review the changed calculation inputs before confirming.");
        var type=sameReference?row.getReference():types.lockById(selected).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Activity reference not found."));
        var estimate=sameReference?estimates.estimateOriginalReference(owner,type,request.recordDate(),request.durationMinutes(),request.zone()):estimates.estimateLocked(owner,type,request.recordDate(),request.durationMinutes(),request.zone());
        if(!estimate.calculationFingerprint().equals(request.calculationFingerprint()))throw new CalculationChangedException(estimate);
        row.setReference(type);row.setExerciseType(type.getDisplayName());row.setDurationMinutes(request.durationMinutes().setScale(2,RoundingMode.UNNECESSARY));row.setRecordDate(request.recordDate());row.setEstimatedKj(estimate.estimatedKj());row.setEstimateStatus(estimate.estimateStatus());row.setWeightKgUsed(estimate.weightKgUsed());row.setWeightDateUsed(estimate.weightDateUsed());row.setMetUsed(estimate.metUsed());row.setFormulaVersion(estimate.formulaVersion());row.setDataQuality(DataQuality.CURRENT);row.setUpdatedAt(clock.instant());records.flush();return ExerciseRecordResponse.from(row);
    }
    @Transactional public void softDeleteOwned(Long owner,Long id,Long expectedVersion){guard.id(id);guard.versionInput(expectedVersion);guard.lockOwner(owner);var row=records.findOwnedIncludingDeleted(owner,id).orElseThrow(guard::missing);requireStandalonePeriod(row);if(row.getDeletedAt()!=null)return;guard.version(row,expectedVersion);row.setDeletedAt(clock.instant());row.setUpdatedAt(clock.instant());records.flush();}
    private static void requireStandalonePeriod(ExerciseRecord row){if(row.getWorkout()!=null)throw new BusinessException(HttpStatus.CONFLICT,"GROUP_EDIT_REQUIRED","Edit or delete this period through its Workout root.");}
    private void validateUpdate(UpdateExerciseRecordRequest request){if(request==null)throw BusinessException.invalid("request","Enter a complete correction.");var errors=validator.validate(request);if(!errors.isEmpty())throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->new FieldViolation(e.getPropertyPath().toString(),null,"Enter a valid value for this field.")).toList());}
    private static Long id(String value){try{long id=Long.parseLong(value);if(id<1)throw new NumberFormatException();return id;}catch(NumberFormatException e){throw BusinessException.invalid("exerciseTypeId","Choose a valid activity identity.");}}
    private void validate(CreateExerciseRecordRequest request){if(request==null)throw BusinessException.invalid("request","Enter a reviewed exercise submission.");var errors=validator.validate(request);if(!errors.isEmpty())throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->new FieldViolation(e.getPropertyPath().toString(),request.clientEntryId()==null?null:request.clientEntryId().toString(),"Enter a valid value for this field.")).toList());}
}
