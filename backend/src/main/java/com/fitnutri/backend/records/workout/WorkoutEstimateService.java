package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.activity.catalogue.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.ProfileService;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.records.food.FoodEntryInputValidator;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.fitnutri.backend.records.workout.ExerciseEntryInputs.*;

/** One dated estimate per period; title, equipment details and movement count never multiply energy. */
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class WorkoutEstimateService {
 private final ExerciseOptionRepository options;private final MovementCatalogueRepository movements;private final ExerciseEstimateService estimates;private final ProfileService profiles;private final RecordDatePolicy dates;private final EquipmentDetailsValidator details;private final CanonicalV2Json canonical;private final JdbcTemplate jdbc;
 public WorkoutEstimateService(ExerciseOptionRepository options,MovementCatalogueRepository movements,ExerciseEstimateService estimates,ProfileService profiles,RecordDatePolicy dates,EquipmentDetailsValidator details,CanonicalV2Json canonical,JdbcTemplate jdbc){this.options=options;this.movements=movements;this.estimates=estimates;this.profiles=profiles;this.dates=dates;this.details=details;this.canonical=canonical;this.jdbc=jdbc;}
 public record PeriodPreview(UUID clientEntryId,BigDecimal durationMinutes,String status,BigDecimal estimatedKj,String calculationFingerprint,String unavailableReason){}
 public record Response(List<PeriodPreview> periods,BigDecimal totalMinutes,BigDecimal knownEnergyKj,boolean partial,String fingerprint,String inputSignature){public Response{periods=List.copyOf(periods);}}
 public record SelectedMovement(Movement input,MovementCatalogue catalogue){}
 public record CalculatedPeriod(Period input,ExerciseOption option,Map<String,Object> equipmentDetails,List<SelectedMovement> movements,ExerciseEstimateResponse estimate,PeriodPreview preview){public CalculatedPeriod{equipmentDetails=Map.copyOf(equipmentDetails);movements=List.copyOf(movements);}}
 public record Calculation(Response preview,List<CalculatedPeriod> periods){public Calculation{periods=List.copyOf(periods);}}
 public record HistoricalPeriod(java.time.LocalDate date,Long optionId,BigDecimal minutes,ExerciseEstimateResponse estimate,PeriodPreview preview,Set<Long>movements){}
 public Response previewNew(Long owner,PreviewRequest request){return calculateNew(owner,request).preview();}
 public Calculation calculateConfirmed(Long owner,CreateRequest request){return calculateNew(owner,new PreviewRequest(request.recordDate(),request.zone(),request.source(),previewOf(request.entry())));}
 public Calculation calculateNew(Long owner,PreviewRequest request){return calculateScoped(owner,request,Map.of());}
 Calculation calculateScoped(Long owner,PreviewRequest request,Map<UUID,HistoricalPeriod> historical){
  if(request==null||request.recordDate()==null||request.zone()==null||request.zone().isBlank()||request.zone().length()>100||request.source()==null||request.source()==RecordSource.PHOTO)throw BusinessException.invalid("request","Review complete exercise inputs and date.");var entry=withoutFingerprints(request.entry());ExerciseEntryInputValidator.validate(entry,false);dates.requireDate(request.recordDate(),request.zone());profiles.requireCompletedAdultSetup(owner);var periods=entry instanceof Quick q?List.of(q.period()):((Workout)entry).periods();
  try{
   if(entry instanceof Workout w&&w.originWorkoutId()!=null&&jdbc.queryForObject("SELECT count(*) FROM workout_record WHERE user_id=? AND id=?",Long.class,owner,FoodEntryInputValidator.referenceId(w.originWorkoutId()))!=1)throw new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Source workout not found.");
   Map<Long,ExerciseOption> selected=new HashMap<>();var optionIds=periods.stream().map(p->FoodEntryInputValidator.referenceId(p.optionId())).distinct().sorted().toList();for(var option:options.findAllWithReferenceById(optionIds))selected.put(option.getId(),option);
   var movementIds=periods.stream().flatMap(p->p.movements().stream()).map(m->FoodEntryInputValidator.referenceId(m.catalogueId())).distinct().sorted().toList();Map<Long,MovementCatalogue> catalogue=new HashMap<>();if(!movementIds.isEmpty())for(var movement:movements.findAllWithDefaultById(movementIds))catalogue.put(movement.getId(),movement);
   var calculated=new ArrayList<CalculatedPeriod>();BigDecimal minutes=BigDecimal.ZERO,energy=BigDecimal.ZERO.setScale(3);int available=0;var fingerprints=new ArrayList<Object>();
   for(var period:periods){
    try{var option=selected.get(FoodEntryInputValidator.referenceId(period.optionId()));if(option==null)throw new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Exercise option not found.");var old=historical.get(period.clientEntryId());boolean sameOriginal=old!=null&&old.optionId().equals(option.getId());if((!option.isActive()&&!sameOriginal)||!"REVIEWED".equals(option.getReviewStatus()))throw new BusinessException(HttpStatus.CONFLICT,"REFERENCE_INACTIVE","Choose an active reviewed exercise option.");if(!option.getPeriodKind().equals(period.periodKind()))throw BusinessException.invalid("periodKind","Use the period kind declared by this option.");
     var equipment=details.normalise(option,period.equipmentDetails());var selectedMoves=new ArrayList<SelectedMovement>();for(var input:period.movements()){var movement=catalogue.get(FoodEntryInputValidator.referenceId(input.catalogueId()));details.movement(option,movement,input,sameOriginal&&old.movements().contains(FoodEntryInputValidator.referenceId(input.catalogueId())));selectedMoves.add(new SelectedMovement(input,movement));}
     ExerciseEstimateResponse estimate=null;BigDecimal value=null;String status,reason;var calculation=new LinkedHashMap<String,Object>();calculation.put("optionId",option.getId().toString());calculation.put("optionRevision",option.getRevision());calculation.put("mappingKind",option.getMappingKind());calculation.put("recordDate",request.recordDate());calculation.put("minutes",period.durationMinutes());
     boolean retained=sameOriginal&&old.date().equals(request.recordDate())&&old.minutes().compareTo(period.durationMinutes())==0;
     if(retained){estimate=old.estimate();status=old.preview().status();reason=old.preview().unavailableReason();value=old.preview().estimatedKj();}
     else if("UNAVAILABLE".equals(option.getMappingKind())){if(option.getReference()!=null)throw unsupported();status="NO_APPLICABLE_REFERENCE";reason=option.getMappingEvidence().get("reason");calculation.put("policy","NO_APPLICABLE_OPTION_REFERENCE_V1");}
     else{if(!Set.of("SPECIFIC_REFERENCE","GENERAL_ACTIVITY_ESTIMATE").contains(option.getMappingKind())||option.getReference()==null)throw unsupported();estimate=sameOriginal?estimates.estimateOriginalReference(owner,option.getReference(),request.recordDate(),period.durationMinutes(),request.zone()):estimates.estimateLocked(owner,option.getReference(),request.recordDate(),period.durationMinutes(),request.zone());status=switch(estimate.estimateStatus()){case AVAILABLE->"AVAILABLE";case MISSING_WEIGHT->"NO_WEIGHT";case NO_APPLICABLE_REFERENCE->"UNSUPPORTED_REFERENCE";default->throw unsupported();};value=estimate.estimatedKj();reason=estimate.unavailableReason();calculation.put("referenceFingerprint",estimate.calculationFingerprint());}
     String fingerprint=retained?old.preview().calculationFingerprint():canonical.hash("workout-period-v2",calculation);var preview=new PeriodPreview(period.clientEntryId(),period.durationMinutes(),status,value,fingerprint,reason);calculated.add(new CalculatedPeriod(period,option,equipment,selectedMoves,estimate,preview));minutes=minutes.add(period.durationMinutes());if(status.equals("AVAILABLE")){available++;energy=energy.add(value);}fingerprints.add(Map.of("clientEntryId",period.clientEntryId(),"calculationFingerprint",fingerprint));
    }catch(BusinessException e){var fields=e.fields().isEmpty()?List.of(new FieldViolation("optionId",period.clientEntryId().toString(),"Review this exercise option.")):e.fields().stream().map(f->new FieldViolation(f.field(),period.clientEntryId().toString(),f.message())).toList();throw new BusinessException(e.status(),e.code(),e.getMessage(),fields);}
   }
   var response=new Response(calculated.stream().map(CalculatedPeriod::preview).toList(),minutes,available==0?null:energy,available>0&&available<calculated.size(),canonical.hash("workout-calculation-v2",fingerprints),canonical.hash("workout-preview-input-v2",request));return new Calculation(response,calculated);
  }catch(DataAccessException e){var error=new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","Exercise source data is temporarily unavailable. Retry the preview.");error.initCause(e);throw error;}
 }
 private static BusinessException unsupported(){return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_REFERENCE","This option does not have a valid reviewed activity mapping.");}
}
