package com.fitnutri.backend.submission;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Hashes only canonical submitted inputs; no database lookup, clock, current profile or derived nutrient. */
@Component
public class CanonicalSubmissionHasher {
    private final ObjectMapper json;private final ZoneResolver zones;
    public CanonicalSubmissionHasher(ObjectMapper json,ZoneResolver zones){this.json=json;this.zones=zones;}
    public record FoodItem(UUID clientEntryId,Long referenceId,BigDecimal quantityGrams,MealType mealType) {}
    public record Submission(UUID submissionId,SubmissionOperation operation,LocalDate recordDate,String zone,String requestSha256,String canonicalJson,List<UUID> clientEntryIds) {public Submission{clientEntryIds=List.copyOf(clientEntryIds);}}
    public Submission food(UUID submissionId,LocalDate date,String zone,RecordSource source,List<FoodItem> items) {
        var body=base(SubmissionOperation.FOOD_BATCH,submissionId,date,zone,source);
        if(items==null||items.isEmpty()||items.size()>50)throw invalid("items");
        for(var i:items)if(i==null||i.clientEntryId()==null||i.referenceId()==null||i.referenceId()<1||i.quantityGrams()==null||i.mealType()==null)throw invalid("items");
        var ordered=items.stream().sorted(Comparator.comparing(i->i.clientEntryId().toString())).toList();var rows=new ArrayList<Map<String,Object>>();
        for(var i:ordered){var row=new LinkedHashMap<String,Object>();row.put("clientEntryId",i.clientEntryId().toString());row.put("referenceId",i.referenceId().toString());row.put("quantityGrams",decimal(i.quantityGrams()));row.put("mealType",i.mealType().name());rows.add(row);}body.put("items",rows);
        return finish(submissionId,SubmissionOperation.FOOD_BATCH,date,body,ordered.stream().map(FoodItem::clientEntryId).toList());
    }
    public Submission exercise(UUID submissionId,LocalDate date,String zone,RecordSource source,UUID clientEntryId,Long exerciseTypeId,BigDecimal minutes,String fingerprint) {
        var body=base(SubmissionOperation.EXERCISE_CREATE,submissionId,date,zone,source);if(clientEntryId==null||exerciseTypeId==null||exerciseTypeId<1||minutes==null||fingerprint==null||fingerprint.isBlank()||fingerprint.length()>128)throw invalid("exerciseTypeId");
        var item=new LinkedHashMap<String,Object>();item.put("clientEntryId",clientEntryId.toString());item.put("exerciseTypeId",exerciseTypeId.toString());item.put("durationMinutes",decimal(minutes));item.put("calculationFingerprint",fingerprint);body.put("item",item);return finish(submissionId,SubmissionOperation.EXERCISE_CREATE,date,body,List.of(clientEntryId));
    }
    private LinkedHashMap<String,Object> base(SubmissionOperation operation,UUID id,LocalDate date,String zone,RecordSource source){
        if(id==null||date==null||!date.toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")||zone==null||zone.isBlank()||source==null||source==RecordSource.PHOTO)throw invalid("submissionId");
        var body=new LinkedHashMap<String,Object>();body.put("operation",operation.name());body.put("submissionId",id.toString());body.put("recordDate",date.toString());body.put("zone",zones.resolve(zone).getId());body.put("source",source.name());return body;
    }
    private Submission finish(UUID id,SubmissionOperation operation,LocalDate date,LinkedHashMap<String,Object> body,List<UUID> entries){
        String canonical=json.writeValueAsString(body);
        try{return new Submission(id,operation,date,(String)body.get("zone"),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))),canonical,entries);}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
    private static String decimal(BigDecimal n){if(n.precision()>100||n.scale()>100||n.scale()< -100)throw invalid("quantity");return n.stripTrailingZeros().toPlainString();}
    private static BusinessException invalid(String field){return BusinessException.invalid(field,"Use a complete canonical submission with stable identities and explicit inputs.");}
}
