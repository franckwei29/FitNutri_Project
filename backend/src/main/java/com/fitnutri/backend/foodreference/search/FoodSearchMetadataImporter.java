package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
/** Offline, explicit atomic import. No nutrient updates or network calls in the transaction. */
@Service
public class FoodSearchMetadataImporter {
 private final FoodSearchMetadataReader reader;private final FoodReferenceRepository foods;private final JdbcTemplate jdbc;private final ObjectMapper json;private final PlatformTransactionManager manager;
 public FoodSearchMetadataImporter(FoodSearchMetadataReader reader,FoodReferenceRepository foods,JdbcTemplate jdbc,ObjectMapper json,PlatformTransactionManager manager){this.reader=reader;this.foods=foods;this.jdbc=jdbc;this.json=json;this.manager=manager;}
 public record ImportReport(int importedProfiles,int importedAliases,boolean replayed,boolean dryRun) {}
 public ImportReport importRevision(Resource resource,boolean dryRun){
  var document=reader.prepare(resource);var tx=new TransactionTemplate(manager);tx.setReadOnly(dryRun);
  return tx.execute(status->{
   if(!dryRun)jdbc.execute("SELECT pg_advisory_xact_lock(6892032741)");
   var references=new LinkedHashMap<String,FoodReference>();
   for(var entry:document.entries()) {
    var f=foods.findActiveByPublicFoodKey(entry.publicFoodKey()).orElseThrow(FoodSearchMetadataImporter::unavailable);var d=f.getDataset();
    if(!d.isVerified()||!d.getName().equals(document.dataset().name())||!d.getRelease().equals(document.dataset().release())||!d.getRevision().equals(document.dataset().revision())||!d.getContentSha256().equals(document.dataset().recordsSha256())||!Objects.equals(f.getOfficialName(),entry.evidence().get("text")))throw unavailable();
    references.put(entry.publicFoodKey(),f);
   }
   int aliasCount=document.entries().stream().mapToInt(e->e.aliases().size()).sum();
   if(dryRun)return new ImportReport(document.entries().size(),aliasCount,false,true);
   foods.lockAllById(references.values().stream().map(FoodReference::getId).sorted().toList());
   int existing=jdbc.queryForObject("SELECT count(*) FROM food_search_profile WHERE metadata_revision=?",Integer.class,document.metadataRevision());
   if(existing>0){
    if(existing!=document.entries().size())throw conflict();
    for(var entry:document.entries()) {
     var profiles=jdbc.queryForList("SELECT id,short_name_en,short_name_zh,concept_key,facets::text,evidence::text,common_rank,review_status FROM food_search_profile WHERE reference_id=? AND metadata_revision=?",references.get(entry.publicFoodKey()).getId(),document.metadataRevision());
     if(profiles.size()!=1)throw conflict();var p=profiles.getFirst();
     if(!Objects.equals(p.get("short_name_en"),entry.shortNameEn())||!Objects.equals(p.get("short_name_zh"),entry.shortNameZh())||!Objects.equals(p.get("concept_key"),entry.conceptKey())||!Objects.equals(p.get("review_status"),"REVIEWED")||((Number)p.get("common_rank")).intValue()!=entry.commonRank()||!json.readTree(p.get("facets").toString()).equals(json.valueToTree(entry.facets()))||!json.readTree(p.get("evidence").toString()).equals(json.valueToTree(entry.evidence())))throw conflict();
     var aliases=jdbc.queryForList("SELECT locale,normalised_text FROM food_search_alias WHERE profile_id=? AND metadata_revision=? AND review_status='REVIEWED'",p.get("id"),document.metadataRevision());
     var expected=entry.aliases().stream().map(a->a.locale()+":"+FoodSearchMetadataReader.normalise(a.value())).collect(java.util.stream.Collectors.toSet());
     var actual=aliases.stream().map(a->a.get("locale")+":"+a.get("normalised_text")).collect(java.util.stream.Collectors.toSet());
     if(aliases.size()!=expected.size()||!actual.equals(expected))throw conflict();
    }
    return new ImportReport(0,0,true,false);
   }
   var ids=references.values().stream().map(FoodReference::getId).toList();
   // Serialize against catalogue imports; replace this dataset's selected metadata revision atomically.
   jdbc.update("UPDATE food_search_profile SET is_active=false WHERE is_active AND reference_id IN (SELECT id FROM food_reference WHERE dataset_id=?)",references.values().iterator().next().getDataset().getId());
   for(var entry:document.entries()) {
    Long id=jdbc.queryForObject("INSERT INTO food_search_profile(reference_id,metadata_revision,is_active,review_status,short_name_en,short_name_zh,concept_key,facets,common_rank,evidence) VALUES(?,?,true,'REVIEWED',?,?,?,?::jsonb,?,?::jsonb) RETURNING id",Long.class,references.get(entry.publicFoodKey()).getId(),document.metadataRevision(),entry.shortNameEn(),entry.shortNameZh(),entry.conceptKey(),json.writeValueAsString(entry.facets()),entry.commonRank(),json.writeValueAsString(entry.evidence()));
    for(var alias:entry.aliases())jdbc.update("INSERT INTO food_search_alias(profile_id,locale,normalised_text,metadata_revision,review_status) VALUES(?,?,?,?,'REVIEWED')",id,alias.locale(),FoodSearchMetadataReader.normalise(alias.value()),document.metadataRevision());
   }
   return new ImportReport(ids.size(),aliasCount,false,false);
  });
 }
 private static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"UX_METADATA_REFERENCE_UNAVAILABLE","Food search metadata requires its exact active verified reference catalogue.");}
 private static BusinessException conflict(){return new BusinessException(HttpStatus.CONFLICT,"UX_METADATA_VERSION_CONFLICT","This search metadata revision has different content. Use a new reviewed revision.");}
}
