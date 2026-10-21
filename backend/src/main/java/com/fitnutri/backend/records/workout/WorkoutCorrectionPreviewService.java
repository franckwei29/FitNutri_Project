package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.RecordSource;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.fitnutri.backend.records.workout.ExerciseEntryInputs.*;
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class WorkoutCorrectionPreviewService{
 private final WorkoutSnapshotReader snapshots;private final WorkoutEstimateService estimates;
 public WorkoutCorrectionPreviewService(WorkoutSnapshotReader snapshots,WorkoutEstimateService estimates){this.snapshots=snapshots;this.estimates=estimates;}
 public WorkoutEstimateService.Response previewOwnedRoot(Long owner,String kind,long id,CorrectionRequest request){return calculateOwned(owner,kind,id,request).preview();}
 public WorkoutEstimateService.Calculation calculateOwned(Long owner,String kind,long id,CorrectionRequest request){if(request==null||request.expectedVersion()==null||request.expectedVersion()<0||request.entry()==null)throw BusinessException.invalid("request","Review a complete exercise correction.");var state=snapshots.state(owner,kind,id);if(state.root().version()!=request.expectedVersion())throw new BusinessException(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and compare the current exercise entry.");var old=state.root().input();if(!old.clientEntryId().equals(request.entry().clientEntryId())||old instanceof Quick!=request.entry() instanceof Quick||old instanceof Workout w&&request.entry() instanceof Workout incoming&&!Objects.equals(w.originWorkoutId(),incoming.originWorkoutId()))throw BusinessException.invalid("entry","Keep the original root identity, kind and source occurrence.");ExerciseEntryInputValidator.validate(request.entry(),false);return estimates.calculateScoped(owner,new PreviewRequest(request.recordDate(),request.zone(),RecordSource.valueOf(state.root().source()),previewOf(request.entry())),state.historical());}
}
