package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.BusinessException;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;
/** Only the explicitly reviewed, pinned product artifact may activate; Python source validation is a paired gate. */
@Component
public class FoodSearchMetadataReader {
 private static final String APPROVED_SHA256="e9b1e50ec95d450acab4d1ad195292abd4b1228c16d76408e511f5506d8b3b47";
 private final ObjectMapper json;
 public FoodSearchMetadataReader(ObjectMapper json){this.json=json;}
 public record Dataset(String name,String release,String revision,String recordsSha256) {}
 public record Alias(String locale,String value) {}
 public record Entry(String publicFoodKey,String shortNameEn,String shortNameZh,String conceptKey,List<Alias> aliases,Map<String,String> facets,int commonRank,String reviewStatus,Map<String,String> evidence) {
  public Entry {aliases=List.copyOf(aliases);facets=Map.copyOf(facets);evidence=Map.copyOf(evidence);}
 }
 public record Document(String format,String metadataRevision,Dataset dataset,List<Entry> entries){public Document{entries=List.copyOf(entries);}}
 public Document prepare(Resource resource) {
  try(var input=resource.getInputStream()) {
   byte[] bytes=input.readNBytes(1024*1024+1);
   if(bytes.length>1024*1024||!APPROVED_SHA256.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw invalid();
   Document doc=json.readerFor(Document.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(bytes);
   if(!"fitnutri.food-search-metadata.v1".equals(doc.format())||doc.metadataRevision()==null||doc.metadataRevision().isBlank()||doc.entries().isEmpty()||doc.entries().stream().anyMatch(e->!"REVIEWED".equals(e.reviewStatus())))throw invalid();
   return doc;
  }catch(BusinessException e){throw e;}catch(Exception e){throw invalid();}
 }
 public static String normalise(String value){return Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").strip();}
 private static BusinessException invalid(){return new BusinessException(HttpStatus.BAD_REQUEST,"UX_METADATA_INVALID","Reviewed food search metadata failed source validation. Use a new reviewed artifact and revision.");}
}
