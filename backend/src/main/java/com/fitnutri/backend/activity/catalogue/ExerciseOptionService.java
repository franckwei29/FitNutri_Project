package com.fitnutri.backend.activity.catalogue;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class ExerciseOptionService {
 private final JdbcTemplate jdbc;private final NamedParameterJdbcTemplate named;private final ObjectMapper json;
 public ExerciseOptionService(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.named=new NamedParameterJdbcTemplate(jdbc);this.json=json;}
 public record Response(String id,String optionKey,String revision,String displayName,String category,String periodKind,String mappingKind,List<String> allowedDetails,String referenceId,String mappingReason){public Response{allowedDetails=List.copyOf(allowedDetails);}}
 public ExerciseCataloguePage<Response> search(String category,String query,int page,int size){String value=ExerciseCatalogueQuery.validate(query,page,size);ExerciseCatalogueQuery.field("category",category,ExerciseCatalogueQuery.CATEGORIES);String base=" FROM exercise_option o WHERE "+ExerciseCatalogueQuery.selectableOption("o");
  try{if(jdbc.queryForObject("SELECT count(*)"+base,Long.class)==0)throw ExerciseCatalogueQuery.unavailable();var args=new MapSqlParameterSource().addValue("limit",size).addValue("offset",(long)page*size);String filter=base+" AND "+ExerciseCatalogueQuery.matches(value,"o.display_name","o.aliases",args);if(category!=null){filter+=" AND o.category=:category";args.addValue("category",category);}long total=named.queryForObject("SELECT count(*)"+filter,args,Long.class);var rows=named.query("SELECT o.id,o.option_key,o.revision,o.display_name,o.category,o.period_kind,o.mapping_kind,o.allowed_details::text,o.reference_id::text,o.mapping_evidence->>'reason' AS mapping_reason"+filter+" ORDER BY lower(o.display_name),o.id LIMIT :limit OFFSET :offset",args,(r,n)->new Response(r.getString("id"),r.getString("option_key"),r.getString("revision"),r.getString("display_name"),r.getString("category"),r.getString("period_kind"),r.getString("mapping_kind"),json.readValue(r.getString("allowed_details"),new TypeReference<List<String>>(){}),r.getString("reference_id"),r.getString("mapping_reason")));return new ExerciseCataloguePage<>(rows,total,page,size,(long)page*size+rows.size()<total);
  }catch(DataAccessException e){var error=ExerciseCatalogueQuery.unavailable();error.initCause(e);throw error;}
 }
}
