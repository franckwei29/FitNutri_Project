package com.fitnutri.backend.records;

import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ExerciseEstimateService {
    public static final String FORMULA_VERSION="MET_KG_MIN_KJ_V1";
    private final ExerciseCatalog catalogue;private final ProfileService profiles;private final WeightEntryRepository weights;private final RecordDatePolicy dates;
    public ExerciseEstimateService(ExerciseCatalog catalogue,ProfileService profiles,WeightEntryRepository weights,RecordDatePolicy dates){this.catalogue=catalogue;this.profiles=profiles;this.weights=weights;this.dates=dates;}
    public ExerciseEstimateResponse estimate(Long owner,Long typeId,LocalDate day,BigDecimal minutes,String zone){validateInput(day,minutes,zone);return compute(owner,catalogue.requireImmutable(typeId),day,minutes,false);}
    /** The caller holds its user lock and this immutable reference's shared lock throughout creation. */
    public ExerciseEstimateResponse estimateLocked(Long owner,ExerciseType type,LocalDate day,BigDecimal minutes,String zone){validateInput(day,minutes,zone);return compute(owner,type,day,minutes,false);}
    /** Only the owning correction service may supply an existing record's original reference here. */
    public ExerciseEstimateResponse estimateOriginalReference(Long owner,ExerciseType type,LocalDate day,BigDecimal minutes,String zone){validateInput(day,minutes,zone);return compute(owner,type,day,minutes,true);}
    private ExerciseEstimateResponse compute(Long owner,ExerciseType type,LocalDate day,BigDecimal minutes,boolean allowInactiveOriginal){
        validateReference(type,allowInactiveOriginal);var profile=profiles.requireCompletedAdultSetup(owner);var d=type.getDataset();Integer birth=profile.getBirthYear();Integer min=d.getMinAge(),max=d.getMaxAge();
        // Only birth year is collected: this is a calendar-year age approximation, not clinical age determination.
        boolean applicable=birth!=null&&min!=null&&max!=null&&day.getYear()-birth>=min&&day.getYear()-birth<=max;
        BigDecimal energy=null,kg=null,met=null;LocalDate weightDate=null;EstimateStatus status;String reason;
        if(!applicable){status=EstimateStatus.NO_APPLICABLE_REFERENCE;reason="The reference does not apply to the approximate age in the activity year, or age information is unavailable. Activity can be recorded without an energy estimate.";}
        else{var weight=weights.findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(owner,day);if(weight.isEmpty()){status=EstimateStatus.MISSING_WEIGHT;reason="No weight measurement exists on or before this activity date. Activity can be recorded without an energy estimate.";}
            else{kg=weight.get().getWeightKg();weightDate=weight.get().getEntryDate();met=type.getMetValue();if(kg==null||kg.compareTo(new BigDecimal("20"))<0||kg.compareTo(new BigDecimal("400"))>0||kg.stripTrailingZeros().scale()>2||weightDate==null||weightDate.isAfter(day))throw new IllegalStateException("Invalid stored dated weight.");energy=met.multiply(kg).multiply(minutes).multiply(new BigDecimal("4.184")).divide(new BigDecimal("60"),3,RoundingMode.HALF_UP);status=EstimateStatus.AVAILABLE;reason=null;}}
        String fingerprint=fingerprint(type,day,minutes,status,kg,weightDate);
        return new ExerciseEstimateResponse(ExerciseTypeResponse.from(type),day,minutes,energy,status,reason,kg,weightDate,met,FORMULA_VERSION,fingerprint);
    }
    private void validateInput(LocalDate day,BigDecimal minutes,String zone){if(minutes==null||minutes.signum()<=0||minutes.compareTo(new BigDecimal("1440"))>0||minutes.stripTrailingZeros().scale()>2)throw BusinessException.invalid("durationMinutes","Enter minutes greater than zero and at most1440, with at most two decimal places.");dates.requireDate(day,zone);}
    private static void validateReference(ExerciseType t,boolean allowInactive){if(t==null||t.getId()==null||t.getDataset()==null||t.getDataset().getId()==null||!t.getDataset().isVerified()||t.getDataset().getContentSha256()==null||t.getSourceActivityCode()==null||t.getSourceActivityCode().isBlank()||t.getSourceDescription()==null||t.getSourceDescription().isBlank()||t.getMetValue()==null||t.getMetValue().signum()<=0||t.getMetValue().compareTo(new BigDecimal("9999.99"))>0||t.getMetValue().stripTrailingZeros().scale()>2)throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_REFERENCE","Choose a verified activity reference.");if(!allowInactive&&!t.isActive())throw new BusinessException(HttpStatus.CONFLICT,"REFERENCE_INACTIVE","This activity version is no longer selectable. Choose an active reference.");}
    private static String fingerprint(ExerciseType t,LocalDate day,BigDecimal minutes,EstimateStatus status,BigDecimal kg,LocalDate weightDate){
        var d=t.getDataset();var values=List.of(FORMULA_VERSION,t.getId().toString(),d.getId().toString(),d.getContentSha256(),t.getMetValue().stripTrailingZeros().toPlainString(),String.valueOf(d.getMinAge()),String.valueOf(d.getMaxAge()),day.toString(),minutes.stripTrailingZeros().toPlainString(),status.name(),kg==null?"null":kg.stripTrailingZeros().toPlainString(),weightDate==null?"null":weightDate.toString());var framed=new StringBuilder();for(String value:values)framed.append(value.length()).append(':').append(value);
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(framed.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
}
