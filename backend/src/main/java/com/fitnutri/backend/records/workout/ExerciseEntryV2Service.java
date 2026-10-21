package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.activity.ExerciseTypeRepository;
import com.fitnutri.backend.activity.catalogue.ExerciseOptionRepository;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.records.food.FoodEntryInputValidator;
import com.fitnutri.backend.submission.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import static com.fitnutri.backend.records.workout.ExerciseEntryInputs.*;
/** The submission strategy owns the transaction, owner lock and committed receipt, including all movement details. */
@Service
public class ExerciseEntryV2Service{
 private final SubmissionService submissions;private final CanonicalEntryHasherV2 hasher;private final WorkoutEstimateService estimates;private final ExerciseOptionRepository options;private final ExerciseTypeRepository references;private final ExerciseRecordRepository records;private final WorkoutRepository workouts;private final WorkoutMovementRepository movements;private final JdbcTemplate jdbc;private final Clock clock;
 public ExerciseEntryV2Service(SubmissionService submissions,CanonicalEntryHasherV2 hasher,WorkoutEstimateService estimates,ExerciseOptionRepository options,ExerciseTypeRepository references,ExerciseRecordRepository records,WorkoutRepository workouts,WorkoutMovementRepository movements,JdbcTemplate jdbc,Clock clock){this.submissions=submissions;this.hasher=hasher;this.estimates=estimates;this.options=options;this.references=references;this.records=records;this.workouts=workouts;this.movements=movements;this.jdbc=jdbc;this.clock=clock;}
 public SubmissionReceiptV2Response createEntryV2(Long owner,CreateRequest request){var canonical=hasher.exercise(request);return submissions.withSubmissionV2(owner,canonical,user->{
  var periods=request.entry() instanceof Quick q?List.of(q.period()):((Workout)request.entry()).periods();lockAuthority(periods);
  var calculation=estimates.calculateConfirmed(owner,request);if(!calculation.preview().fingerprint().equals(request.previewFingerprint())||calculation.periods().stream().anyMatch(p->!p.preview().calculationFingerprint().equals(periods.stream().filter(i->i.clientEntryId().equals(p.input().clientEntryId())).findFirst().orElseThrow().calculationFingerprint())))throw new ExerciseCalculationChangedV2Exception(calculation.preview());
  var now=clock.instant();WorkoutRecord root=null;
  if(request.entry() instanceof Workout input){root=new WorkoutRecord();root.setUser(user);root.setClientEntryId(input.clientEntryId());root.setTitle(input.title()==null?"Gym workout":input.title());root.setRecordDate(request.recordDate());root.setSource(request.source());root.setCreatedAt(now);root.setUpdatedAt(now);if(input.originWorkoutId()!=null)root.setOrigin(workouts.findOwnedIncludingDeleted(owner,FoodEntryInputValidator.referenceId(input.originWorkoutId())).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Source workout not found.")));workouts.saveAndFlush(root);}
  var leaves=new ArrayList<SubmissionReceiptV2Response.Leaf>();ExerciseRecord quick=null;int position=0;
  for(var period:calculation.periods()){var row=new ExerciseRecord();row.setUser(user);row.setClientEntryId(period.input().clientEntryId());row.setSource(request.source());row.setDataQuality(DataQuality.CURRENT);row.setCreatedAt(now);row.setUpdatedAt(now);row.setRecordedAt(now);row.setRecordDate(request.recordDate());row.setExerciseType(period.option().getDisplayName());row.setDurationMinutes(period.input().durationMinutes());row.setWorkout(root);row.setPeriodPosition(root==null?null:position++);row.setPeriodKind(period.input().periodKind());row.setOption(period.option());row.setOptionNameSnapshot(period.option().getDisplayName());row.setEquipmentDetails(period.equipmentDetails());var estimate=period.estimate();
   if(estimate==null){row.setEstimateStatus(EstimateStatus.NO_APPLICABLE_REFERENCE);}else{row.setReference(period.option().getReference());row.setEstimatedKj(estimate.estimatedKj());row.setEstimateStatus(estimate.estimateStatus());row.setWeightKgUsed(estimate.weightKgUsed());row.setWeightDateUsed(estimate.weightDateUsed());row.setMetUsed(estimate.metUsed());row.setFormulaVersion(estimate.formulaVersion());}
   records.saveAndFlush(row);for(int i=0;i<period.movements().size();i++){var selected=period.movements().get(i);var move=new WorkoutMovementLog();move.setPeriod(row);move.setMovement(selected.catalogue());move.setPosition(i);move.setNameSnapshot(selected.catalogue().getDisplayName());move.setLoadBasisSnapshot(selected.catalogue().getLoadBasis());move.setSetCount(selected.input().setCount()==null?null:Math.toIntExact(selected.input().setCount()));move.setRepetitions(selected.input().repetitions()==null?null:Math.toIntExact(selected.input().repetitions()));move.setLoadKg(selected.input().loadKg());movements.saveAndFlush(move);}quick=row;leaves.add(new SubmissionReceiptV2Response.Leaf("ACTIVITY_PERIOD",row.getId().toString(),row.getClientEntryId(),request.entry().clientEntryId(),row.getRecordDate(),0));
  }
  var proofRoot=root==null?new SubmissionReceiptV2Response.Root("QUICK",quick.getId().toString(),quick.getClientEntryId(),quick.getRecordDate(),0):new SubmissionReceiptV2Response.Root("WORKOUT",root.getId().toString(),root.getClientEntryId(),root.getRecordDate(),0);return new SubmissionReceiptV2Response.Proof(2,List.of(proofRoot),leaves);
 });}
 /** Lock metadata rows separately: nullable reference joins cannot be FOR SHARE targets. Sorted owner-first order. */
 public void lockAuthority(List<Period>periods){var ids=periods.stream().map(p->FoodEntryInputValidator.referenceId(p.optionId())).distinct().sorted().toList();for(var id:ids)jdbc.queryForList("SELECT id FROM exercise_option WHERE id=? FOR SHARE",id);var loaded=options.findAllWithReferenceById(ids);var refs=loaded.stream().filter(o->o.getReference()!=null).map(o->o.getReference().getId()).distinct().sorted().toList();if(!refs.isEmpty())references.lockAllById(refs);var moves=periods.stream().flatMap(p->p.movements().stream()).map(m->FoodEntryInputValidator.referenceId(m.catalogueId())).distinct().sorted().toList();for(var id:moves)jdbc.queryForList("SELECT id FROM movement_catalogue WHERE id=? FOR SHARE",id);}
}
