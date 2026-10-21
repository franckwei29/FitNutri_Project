package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;

/** Pure preparation before opening a database transaction. All source/data bytes and rows must validate. */
@Component
public class ReferenceManifestReader {
    private static final int MAX_BYTES=20*1024*1024;
    private final ObjectMapper json;
    public ReferenceManifestReader(ObjectMapper json) {this.json=json;}
    public record SourceFile(String filename,String url,String sha256) {}
    public record Manifest(String format,String datasetName,String release,String revision,String sourceUrl,String nutrientPolicy,String recordsFile,String recordsSha256,int expectedCount,List<SourceFile> sourceFiles,Map<String,Object> mapping,String sourceNote) {}
    public record FoodRow(String publicFoodKey,String surveyId,String officialName,String displayName,String description,BigDecimal energyKj100g,BigDecimal proteinG100g,BigDecimal fatG100g,BigDecimal carbsG100g,BigDecimal fiberG100g,BigDecimal sugarG100g,BigDecimal sodiumMg100g) {}
    public record Records(String format,List<FoodRow> foods) {}
    public record Prepared(Manifest manifest,List<FoodRow> foods,String manifestSha256) {}
    public Prepared prepare(Resource manifestResource,Resource recordsResource,Map<String,Resource> rawSources) {
        try {
            byte[] manifestBytes=bytes(manifestResource);
            Manifest m=json.readerFor(Manifest.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(manifestBytes);
            if(!"fitnutri.food-manifest.v1".equals(m.format())||!text(m.datasetName(),128)||!text(m.release(),128)||!text(m.revision(),128)||!url(m.sourceUrl())||!filename(m.recordsFile())||!sha(m.recordsSha256())||m.expectedCount()<1||m.expectedCount()>20000||m.mapping()==null||m.mapping().isEmpty()||!text(m.sourceNote(),8000)) throw invalid();
            if(!Set.of("CORE4_REQUIRED","AUSNUT2023_ENERGY_WITH_FIBRE_CARB_EXCL_POLYOLS_CORE4_REQUIRED").contains(m.nutrientPolicy())) throw invalid();
            if(m.sourceFiles()==null||m.sourceFiles().isEmpty()||m.sourceFiles().size()>10||rawSources==null) throw invalid();
            Set<String> filenames=new HashSet<>();boolean primary=false;
            for(var source:m.sourceFiles()) {
                if(source==null||!filename(source.filename())||!filenames.add(source.filename())||!url(source.url())||!sha(source.sha256())) throw invalid();
                primary|=source.url().equals(m.sourceUrl());Resource raw=rawSources.get(source.filename());
                if(raw==null||!source.sha256().equals(digest(bytes(raw)))) throw invalid();
            }
            if(!primary||!rawSources.keySet().equals(filenames)) throw invalid();
            byte[] data=bytes(recordsResource);if(!m.recordsSha256().equals(digest(data))) throw invalid();
            Records records=json.readerFor(Records.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(data);
            if(!"fitnutri.food.v1".equals(records.format())||records.foods()==null||records.foods().size()!=m.expectedCount()) throw invalid();
            Set<String> keys=new HashSet<>(),surveys=new HashSet<>();
            for(var r:records.foods()) {
                if(r==null||!text(r.publicFoodKey(),255)||!text(r.surveyId(),255)||!keys.add(r.publicFoodKey())||!surveys.add(r.surveyId())||!(r.officialName()!=null&&!r.officialName().isBlank()&&r.officialName().length()<=255)||!text(r.displayName(),255)||(r.description()!=null&&r.description().length()>2048)) throw invalid();
                for(var n:Arrays.asList(r.energyKj100g(),r.proteinG100g(),r.fatG100g(),r.carbsG100g())) if(!nutrient(n,false)) throw invalid();
                for(var n:Arrays.asList(r.fiberG100g(),r.sugarG100g(),r.sodiumMg100g())) if(!nutrient(n,true)) throw invalid();
            }
            return new Prepared(m,List.copyOf(records.foods()),digest(manifestBytes));
        } catch(BusinessException e) {throw e;} catch(Exception e) {throw invalid();}
    }
    private byte[] bytes(Resource resource) throws Exception {
        if(resource==null) throw invalid();try(var in=resource.getInputStream()){byte[] data=in.readNBytes(MAX_BYTES+1);if(data.length>MAX_BYTES) throw invalid();return data;}
    }
    private static boolean nutrient(BigDecimal value,boolean nullable) {return value==null?nullable:value.signum()>=0&&value.compareTo(new BigDecimal("999999999.999"))<=0&&value.stripTrailingZeros().scale()<=3;}
    private static boolean text(String value,int max) {return value!=null&&!value.isBlank()&&value.length()<=max&&value.equals(value.trim());}
    private static boolean filename(String value) {return value!=null&&value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,200}")&&!value.contains("..");}
    private static boolean sha(String value) {return value!=null&&value.matches("[a-f0-9]{64}")&&!value.equals("0".repeat(64));}
    private static boolean url(String value) {try {URI uri=URI.create(value);return "https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(Exception e){return false;}}
    private static String digest(byte[] data) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
    private static BusinessException invalid() {return new BusinessException(HttpStatus.BAD_REQUEST,"DATASET_SOURCE_INVALID","Catalogue source files, manifest mapping or nutrient rows failed validation.");}
}
