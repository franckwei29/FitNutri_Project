package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.ReferenceManifestReader.SourceFile;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;

@Component
public class ActivityManifestReader {
    private final ObjectMapper json;
    public ActivityManifestReader(ObjectMapper json) {this.json=json;}
    public record Manifest(String format,String datasetName,String release,String revision,String sourceUrl,Integer minAge,Integer maxAge,String recordsFile,String recordsSha256,int expectedCount,List<SourceFile> sourceFiles,Map<String,Object> mapping,String sourceNote) {}
    public record ActivityRow(String activityKey,String displayName,String sourceActivityCode,String sourceDescription,BigDecimal metValue) {}
    public record Records(String format,List<ActivityRow> activities) {}
    public record Prepared(Manifest manifest,List<ActivityRow> activities,String manifestSha256) {}
    public Prepared prepare(Resource manifest,Resource records,Map<String,Resource> sources) {
        try {
            byte[] mb=bytes(manifest);Manifest m=json.readerFor(Manifest.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(mb);
            if(!"fitnutri.activity-manifest.v1".equals(m.format())||!text(m.datasetName(),128)||!text(m.release(),128)||!text(m.revision(),128)||!url(m.sourceUrl())||!filename(m.recordsFile())||!sha(m.recordsSha256())||m.expectedCount()<1||m.expectedCount()>10000||m.minAge()==null||m.maxAge()==null||m.minAge()<0||m.maxAge()<m.minAge()||m.maxAge()>130||m.mapping()==null||m.mapping().isEmpty()||!text(m.sourceNote(),8000)) throw invalid();
            if(m.sourceFiles()==null||m.sourceFiles().isEmpty()||m.sourceFiles().size()>10||sources==null) throw invalid();
            var names=new HashSet<String>();boolean primary=false;
            for(var source:m.sourceFiles()) {if(source==null||!filename(source.filename())||!names.add(source.filename())||!url(source.url())||!sha(source.sha256())||!source.sha256().equals(digest(bytes(sources.get(source.filename()))))) throw invalid();primary|=source.url().equals(m.sourceUrl());}
            if(!primary||!sources.keySet().equals(names)) throw invalid();byte[] rb=bytes(records);if(!m.recordsSha256().equals(digest(rb))) throw invalid();
            Records data=json.readerFor(Records.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(rb);
            if(!"fitnutri.activity.v1".equals(data.format())||data.activities()==null||data.activities().size()!=m.expectedCount()) throw invalid();
            var keys=new HashSet<String>();var codes=new HashSet<String>();
            for(var a:data.activities()) if(a==null||!text(a.activityKey(),255)||!a.activityKey().matches("[a-z0-9]+(?:-[a-z0-9]+)*")||!keys.add(a.activityKey())||!text(a.displayName(),255)||a.sourceActivityCode()==null||!a.sourceActivityCode().matches("[0-9]{5}")||!codes.add(a.sourceActivityCode())||!text(a.sourceDescription(),2048)||a.metValue()==null||a.metValue().signum()<=0||a.metValue().compareTo(new BigDecimal("9999.99"))>0||a.metValue().stripTrailingZeros().scale()>2) throw invalid();
            return new Prepared(m,List.copyOf(data.activities()),digest(mb));
        } catch(BusinessException e){throw e;}catch(Exception e){throw invalid();}
    }
    private static byte[] bytes(Resource r) throws Exception {if(r==null) throw invalid();try(var in=r.getInputStream()){byte[] b=in.readNBytes(20*1024*1024+1);if(b.length>20*1024*1024)throw invalid();return b;}}
    private static String digest(byte[] b) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    private static boolean text(String s,int max) {return s!=null&&!s.isBlank()&&s.length()<=max&&s.equals(s.trim());}
    private static boolean filename(String s) {return s!=null&&s.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,200}")&&!s.contains("..");}
    private static boolean sha(String s){return s!=null&&s.matches("[a-f0-9]{64}")&&!s.equals("0".repeat(64));}
    private static boolean url(String s){try{var u=URI.create(s);return "https".equals(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
    private static BusinessException invalid(){return new BusinessException(HttpStatus.BAD_REQUEST,"DATASET_SOURCE_INVALID","Activity source files, manifest, age applicability or activity rows failed validation.");}
}
