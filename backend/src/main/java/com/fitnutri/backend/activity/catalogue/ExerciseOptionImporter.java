package com.fitnutri.backend.activity.catalogue;
import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/** Explicit offline import; the existing activity importer lock and one transaction protect immutable mappings. */
@Service
public class ExerciseOptionImporter {
 private final ExerciseOptionCatalogueReader reader;private final ExerciseTypeRepository references;private final JdbcTemplate jdbc;private final ObjectMapper json;private final PlatformTransactionManager manager;
 public ExerciseOptionImporter(ExerciseOptionCatalogueReader reader,ExerciseTypeRepository references,JdbcTemplate jdbc,ObjectMapper json,PlatformTransactionManager manager){this.reader=reader;this.references=references;this.jdbc=jdbc;this.json=json;this.manager=manager;}
 public record Report(int options,int movements,boolean replayed,boolean dryRun){}
 public Report importImmutableCatalogueVersion(Resource options,Resource movements,boolean dryRun){var data=reader.prepare(options,movements);var tx=new TransactionTemplate(manager);tx.setReadOnly(dryRun);return tx.execute(status->{
  if(!dryRun)jdbc.execute("SELECT pg_advisory_xact_lock(6892032742)");var expected=data.options().dataset();Map<String,ExerciseType> loaded=new HashMap<>();for(var reference:references.findByActiveTrueAndDatasetVerifiedTrue()){var d=reference.getDataset();if(Objects.equals(d.getName(),expected.name())&&Objects.equals(d.getRelease(),expected.release())&&Objects.equals(d.getRevision(),expected.revision())&&Objects.equals(d.getContentSha256(),expected.recordsSha256()))loaded.put(reference.getActivityKey(),reference);}
  for(var option:data.options().entries())reader.validateOptionMapping(expected,option,option.activityKey()==null?null:loaded.get(option.activityKey()));if(dryRun)return new Report(data.options().entries().size(),data.movements().entries().size(),false,true);
  references.lockAllById(loaded.values().stream().map(ExerciseType::getId).distinct().sorted().toList());
  int existingOptions=jdbc.queryForObject("SELECT count(*) FROM exercise_option WHERE revision=?",Integer.class,data.options().revision()),existingMovements=jdbc.queryForObject("SELECT count(*) FROM movement_catalogue WHERE revision=?",Integer.class,data.movements().revision());
  if(existingOptions>0||existingMovements>0){if(existingOptions!=data.options().entries().size()||existingMovements!=data.movements().entries().size())throw conflict();verifyExisting(data,loaded);return new Report(0,0,true,false);}
  var optionIds=new HashMap<String,Long>();
  for(var option:data.options().entries()){
   jdbc.update("UPDATE exercise_option SET active=false WHERE option_key=? AND active",option.optionKey());Long reference=option.activityKey()==null?null:loaded.get(option.activityKey()).getId();
   Long id=jdbc.queryForObject("INSERT INTO exercise_option(option_key,revision,active,category,period_kind,display_name,aliases,allowed_details,reference_id,mapping_kind,mapping_evidence,review_status) VALUES(?,?,true,?,?,?,?::jsonb,?::jsonb,?,?,?::jsonb,'REVIEWED') RETURNING id",Long.class,option.optionKey(),data.options().revision(),option.category(),option.periodKind(),option.displayName(),json.writeValueAsString(option.aliases()),json.writeValueAsString(option.allowedDetails()),reference,option.mappingKind(),json.writeValueAsString(reader.evidence(expected,option)));optionIds.put(option.optionKey(),id);
  }
  for(var movement:data.movements().entries()){
   jdbc.update("UPDATE movement_catalogue SET active=false WHERE movement_key=? AND active",movement.movementKey());
   jdbc.update("INSERT INTO movement_catalogue(movement_key,revision,active,display_name,aliases,body_region,equipment_type,load_basis,allowed_fields,review_status,default_period_option_id) VALUES(?,?,true,?,?::jsonb,?,?,?,?::jsonb,'REVIEWED',?)",movement.movementKey(),data.movements().revision(),movement.displayName(),json.writeValueAsString(movement.aliases()),movement.bodyRegion(),movement.equipmentType(),movement.loadBasis(),json.writeValueAsString(movement.allowedFields()),optionIds.get(movement.defaultOptionKey()));
  }
  return new Report(data.options().entries().size(),data.movements().entries().size(),false,false);
 });}
 private void verifyExisting(ExerciseOptionCatalogueReader.Bundle data,Map<String,ExerciseType> references){
  var optionIds=new HashMap<String,Long>();for(var option:data.options().entries()){
   var rows=jdbc.queryForList("SELECT id,category,period_kind,display_name,aliases::text,allowed_details::text,reference_id,mapping_kind,mapping_evidence::text,review_status FROM exercise_option WHERE option_key=? AND revision=?",option.optionKey(),data.options().revision());if(rows.size()!=1)throw conflict();var row=rows.getFirst();Long id=option.activityKey()==null?null:references.get(option.activityKey()).getId();
   if(!Objects.equals(row.get("category"),option.category())||!Objects.equals(row.get("period_kind"),option.periodKind())||!Objects.equals(row.get("display_name"),option.displayName())||!Objects.equals(row.get("reference_id"),id)||!Objects.equals(row.get("mapping_kind"),option.mappingKind())||!Objects.equals(row.get("review_status"),"REVIEWED")||!equalJson(row,"aliases",option.aliases())||!equalJson(row,"allowed_details",option.allowedDetails())||!equalJson(row,"mapping_evidence",reader.evidence(data.options().dataset(),option)))throw conflict();optionIds.put(option.optionKey(),((Number)row.get("id")).longValue());
  }
  for(var movement:data.movements().entries()){
   var rows=jdbc.queryForList("SELECT display_name,aliases::text,body_region,equipment_type,load_basis,allowed_fields::text,review_status,default_period_option_id FROM movement_catalogue WHERE movement_key=? AND revision=?",movement.movementKey(),data.movements().revision());if(rows.size()!=1)throw conflict();var row=rows.getFirst();
   if(!Objects.equals(row.get("display_name"),movement.displayName())||!Objects.equals(row.get("body_region"),movement.bodyRegion())||!Objects.equals(row.get("equipment_type"),movement.equipmentType())||!Objects.equals(row.get("load_basis"),movement.loadBasis())||!Objects.equals(row.get("review_status"),"REVIEWED")||!Objects.equals(row.get("default_period_option_id"),optionIds.get(movement.defaultOptionKey()))||!equalJson(row,"aliases",movement.aliases())||!equalJson(row,"allowed_fields",movement.allowedFields()))throw conflict();
  }
 }
 private boolean equalJson(Map<String,Object> row,String field,Object expected){return json.readTree(row.get(field).toString()).equals(json.valueToTree(expected));}
 private static BusinessException conflict(){return new BusinessException(HttpStatus.CONFLICT,"UX_EXERCISE_VERSION_CONFLICT","This exercise catalogue revision has different immutable content. Use a new reviewed revision.");}
}
