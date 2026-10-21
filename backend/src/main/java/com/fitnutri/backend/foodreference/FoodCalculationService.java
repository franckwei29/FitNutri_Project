package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** One authority for preview, creation and explicit correction. No binary floating-point arithmetic. */
@Service
public class FoodCalculationService {
    private static final BigDecimal HUNDRED=new BigDecimal("100"), MAX_GRAMS=new BigDecimal("10000"), MAX_SNAPSHOT=new BigDecimal("999999999.999");
    public NutrientValues calculate(FoodReference reference,BigDecimal grams) {return calculate(reference,grams,false);}
    /** Caller must first establish ownership and that this is the existing record's original immutable reference. */
    public NutrientValues calculateHistorical(FoodReference reference,BigDecimal grams) {return calculate(reference,grams,true);}
    /** Shared validation for rational scaling, without prematurely rounding the full recipe. */
    public void validateSelection(FoodReference reference,BigDecimal grams){validateGrams(grams);validateReference(reference,false);}
    /** Only an owned original immutable reference may be passed by a correction service. */
    public void validateHistoricalSelection(FoodReference reference,BigDecimal grams){validateGrams(grams);validateReference(reference,true);}
    private NutrientValues calculate(FoodReference reference,BigDecimal grams,boolean allowInactiveOriginal) {
        validateGrams(grams);validateReference(reference,allowInactiveOriginal);
        return new NutrientValues(portion(reference.getEnergyKj100g(),grams),portion(reference.getProteinG100g(),grams),portion(reference.getFatG100g(),grams),portion(reference.getCarbsG100g(),grams));
    }
    public FoodNutritionResponse preview(FoodReference reference,BigDecimal grams) {var nutrients=calculate(reference,grams);return new FoodNutritionResponse(ReferenceInfo.from(reference),grams,nutrients,fingerprint(reference,grams));}
    /** Owning original-reference scope is established by OwnedPreviewService before invoking this method. */
    public FoodNutritionResponse previewHistorical(FoodReference reference,BigDecimal grams){var nutrients=calculateHistorical(reference,grams);return new FoodNutritionResponse(ReferenceInfo.from(reference),grams,nutrients,fingerprint(reference,grams));}
    public String fingerprint(FoodReference reference,BigDecimal grams) {
        validateGrams(grams);validateReference(reference,true);
        // Length framing prevents delimiter ambiguity; labels, display units and current activation are not calculation inputs.
        var values=List.of("food-portion-v1",reference.getId().toString(),reference.getDataset().getId().toString(),reference.getDataset().getContentSha256(),canonical(grams),canonical(reference.getEnergyKj100g()),canonical(reference.getProteinG100g()),canonical(reference.getFatG100g()),canonical(reference.getCarbsG100g()));
        StringBuilder framed=new StringBuilder();for(String value:values) framed.append(value.length()).append(':').append(value);
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(framed.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
    private static BigDecimal portion(BigDecimal per100g,BigDecimal grams) {var result=per100g.multiply(grams).divide(HUNDRED).setScale(3,RoundingMode.HALF_UP);if(result.compareTo(MAX_SNAPSHOT)>0) throw unsupported();return result;}
    private static void validateGrams(BigDecimal grams) {if(grams==null||grams.signum()<=0||grams.compareTo(MAX_GRAMS)>0||grams.stripTrailingZeros().scale()>3) throw BusinessException.invalid("grams","Enter grams greater than zero and at most 10000, with at most three decimal places.");}
    private static void validateReference(FoodReference f,boolean allowInactive) {
        if(f==null||f.getId()==null||f.getDataset()==null||f.getDataset().getId()==null||!f.getDataset().isVerified()||f.getDataset().getContentSha256()==null) throw unsupported();
        if(!allowInactive&&!f.isActive())throw new BusinessException(HttpStatus.CONFLICT,"REFERENCE_INACTIVE","This reference version is no longer selectable. Retain the other reviewed entries and choose an active reference.");
        for(var value:Arrays.asList(f.getEnergyKj100g(),f.getProteinG100g(),f.getFatG100g(),f.getCarbsG100g())) if(value==null||value.signum()<0||value.compareTo(MAX_SNAPSHOT)>0||value.stripTrailingZeros().scale()>3) throw unsupported();
    }
    private static String canonical(BigDecimal value) {return value.stripTrailingZeros().toPlainString();}
    private static BusinessException unsupported() {return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"UNSUPPORTED_REFERENCE","This reference cannot provide a verified complete nutrient snapshot for the selected portion.");}
}
