package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeightService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WeightEntryRepository weights;
    private final RecordDatePolicy dates;
    private final Validator validator;
    private final Clock clock;
    public WeightService(UserRepository users,UserProfileRepository profiles,WeightEntryRepository weights,RecordDatePolicy dates,Validator validator,Clock clock) {
        this.users=users;this.profiles=profiles;this.weights=weights;this.dates=dates;this.validator=validator;this.clock=clock;
    }
    public record WeightResult(WeightEntryResponse weight,Long profileVersion,WeightEntryResponse currentWeight) {}
    public record UpsertOutcome(boolean created,WeightResult result) {}
    @Transactional
    public UpsertOutcome upsertByDate(Long userId,AddWeightRequest request) {
        validate(request);dates.requireDate(request.recordDate(),request.zone());
        requireUser(users.lockById(userId));
        WeightEntry current=weights.findByUserIdAndEntryDate(userId,request.recordDate()).orElse(null);
        if(current!=null) {
            if(request.expectedVersion()==null) {
                if(current.getWeightKg().compareTo(request.weightKg())==0) return outcome(false,userId,current);
                throw new WeightConflictException(current);
            }
            if(current.getVersion()!=request.expectedVersion()) throw new WeightConflictException(current);
            current.setWeightKg(request.weightKg().setScale(2));current.setUpdatedAt(clock.instant());
        } else {
            if(request.expectedVersion()!=null) throw new WeightConflictException(null);
            current=WeightEntry.of(users.getReferenceById(userId),request.weightKg(),request.recordDate());
            current.setCreatedAt(clock.instant());current.setUpdatedAt(clock.instant());
        }
        boolean creating=current.getId()==null;
        weights.saveAndFlush(current);
        WeightEntry latest=latestCurrent(userId).orElseThrow();
        profiles.findById(userId).ifPresent(p->{p.setWeightKg(latest.getWeightKg().doubleValue());p.setUpdatedAt(clock.instant());});
        profiles.flush();return outcome(creating,userId,current);
    }
    @Transactional(readOnly=true)
    public List<WeightEntryResponse> history(Long userId,LocalDate from,LocalDate to,String zone) {
        requireUser(users.findById(userId));dates.requireRange(from,to,zone);
        return weights.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId,from,to).stream().map(WeightEntryResponse::from).toList();
    }
    @Transactional(readOnly=true)
    public Optional<WeightEntry> latestOnOrBefore(Long userId,LocalDate recordDate) {return weights.findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(userId,recordDate);}
    @Transactional(readOnly=true)
    public Optional<WeightEntry> latestCurrent(Long userId) {return weights.findFirstByUserIdOrderByEntryDateDesc(userId);}
    private UpsertOutcome outcome(boolean created,Long userId,WeightEntry saved) {
        Long profileVersion=profiles.findById(userId).map(UserProfile::getVersion).orElse(null);
        return new UpsertOutcome(created,new WeightResult(WeightEntryResponse.from(saved),profileVersion,WeightEntryResponse.from(latestCurrent(userId).orElseThrow())));
    }
    private void validate(AddWeightRequest request) {
        if(request==null) throw BusinessException.invalid("body","Enter the measurement details.");
        var errors=validator.validate(request);
        if(!errors.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->new FieldViolation(e.getPropertyPath().toString(),null,"Enter a valid value for this field.")).toList());
    }
    private static void requireUser(Optional<User> candidate) {
        User user=candidate.filter(User::isEnabled).orElseThrow(()->new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue."));
        if(user.getRole()!=User.Role.USER) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");
    }
}
