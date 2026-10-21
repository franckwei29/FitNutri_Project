package com.fitnutri.backend.activity.catalogue;
import com.fitnutri.backend.activity.ExerciseType;
import com.fitnutri.backend.common.BusinessException;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;

/** Source-reviewed product artifacts; a change requires a new reviewed version/digest, never runtime inference. */
@Component
public class ExerciseOptionCatalogueReader {
 private static final String OPTIONS_SHA="a4f5307a149b0374daf34965b7d0a1cff716abf8ea9ccfaf74f4a73ced957643",MOVEMENTS_SHA="38f76897ea980d38009168629d28ce6a9ed31ebcacdd359371aa241cc3879bfb";
 private final ObjectMapper json;
 public ExerciseOptionCatalogueReader(ObjectMapper json){this.json=json;}
 public record Dataset(String name,String release,String revision,String recordsSha256){}
 public record Option(String optionKey,String displayName,String category,String periodKind,List<String> aliases,List<String> allowedDetails,String mappingKind,String activityKey,String sourceActivityCode,String sourceDescription,String mappingReason,String reviewStatus){public Option{aliases=List.copyOf(aliases);allowedDetails=List.copyOf(allowedDetails);}}
 public record Movement(String movementKey,String displayName,List<String> aliases,String bodyRegion,String equipmentType,String loadBasis,List<String> allowedFields,String defaultOptionKey,String reviewStatus){public Movement{aliases=List.copyOf(aliases);allowedFields=List.copyOf(allowedFields);}}
 public record Options(String format,String revision,Dataset dataset,List<Option> entries){public Options{entries=List.copyOf(entries);}}
 public record Movements(String format,String revision,String defaultOptionRevision,List<Movement> entries){public Movements{entries=List.copyOf(entries);}}
 public record Bundle(Options options,Movements movements){}
 public Bundle prepare(Resource options,Resource movements){
  try{byte[] a=bytes(options,OPTIONS_SHA),b=bytes(movements,MOVEMENTS_SHA);Options o=json.readerFor(Options.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(a);Movements m=json.readerFor(Movements.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(b);
   if(!o.format().equals("fitnutri.exercise-options.v1")||!m.format().equals("fitnutri.movement-catalogue.v1")||!m.defaultOptionRevision().equals(o.revision())||o.entries().size()!=38||m.entries().size()!=36||o.entries().stream().anyMatch(e->!e.reviewStatus().equals("REVIEWED"))||m.entries().stream().anyMatch(e->!e.reviewStatus().equals("REVIEWED")))throw invalid();return new Bundle(o,m);
  }catch(BusinessException e){throw e;}catch(Exception e){throw invalid();}
 }
 public void validateOptionMapping(Dataset dataset,Option option,ExerciseType reference){
  if(option.mappingKind().equals("UNAVAILABLE")){if(reference!=null||option.activityKey()!=null||option.sourceActivityCode()!=null||option.sourceDescription()!=null)throw invalid();return;}
  if(reference==null||reference.getId()==null||!reference.isActive()||reference.getDataset()==null)throw unavailable();var d=reference.getDataset();
  if(!d.isVerified()||!Objects.equals(d.getName(),dataset.name())||!Objects.equals(d.getRelease(),dataset.release())||!Objects.equals(d.getRevision(),dataset.revision())||!Objects.equals(d.getContentSha256(),dataset.recordsSha256())||!Objects.equals(d.getMinAge(),19)||!Objects.equals(d.getMaxAge(),59)||!Objects.equals(reference.getActivityKey(),option.activityKey())||!Objects.equals(reference.getSourceActivityCode(),option.sourceActivityCode())||!Objects.equals(reference.getSourceDescription(),option.sourceDescription())||reference.getMetValue()==null||reference.getMetValue().signum()<=0)throw unavailable();
 }
 public Map<String,String> evidence(Dataset dataset,Option option){var result=new LinkedHashMap<String,String>();result.put("reason",option.mappingReason());result.put("sourceDatasetRevision",dataset.revision());result.put("sourceRecordsSha256",dataset.recordsSha256());if(option.activityKey()!=null){result.put("sourceActivityCode",option.sourceActivityCode());result.put("sourceDescription",option.sourceDescription());}return Collections.unmodifiableMap(result);}
 private static byte[] bytes(Resource resource,String sha)throws Exception{try(var in=resource.getInputStream()){byte[] bytes=in.readNBytes(1024*1024+1);if(bytes.length>1024*1024||!sha.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw invalid();return bytes;}}
 private static BusinessException invalid(){return new BusinessException(HttpStatus.BAD_REQUEST,"UX_EXERCISE_CATALOGUE_INVALID","Reviewed exercise catalogue artifacts failed validation. Use a new reviewed revision.");}
 private static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"UX_ACTIVITY_SOURCE_UNAVAILABLE","Exercise options require their exact active verified activity source revision. Import the reviewed source first.");}
}
