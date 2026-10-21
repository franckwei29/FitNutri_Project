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
public class MovementCatalogueService {
 private final JdbcTemplate jdbc;private final NamedParameterJdbcTemplate named;private final ObjectMapper json;
 public MovementCatalogueService(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.named=new NamedParameterJdbcTemplate(jdbc);this.json=json;}
 public record Response(String id,String displayName,String bodyRegion,String equipmentType,String loadBasis,String defaultPeriodOptionId,List<String> allowedFields){public Response{allowedFields=List.copyOf(allowedFields);}}
 public ExerciseCataloguePage<Response> search(String bodyRegion,String equipment,String query,int page,int size){String value=ExerciseCatalogueQuery.validate(query,page,size);ExerciseCatalogueQuery.field("bodyRegion",bodyRegion,ExerciseCatalogueQuery.BODY_REGIONS);ExerciseCatalogueQuery.field("equipment",equipment,ExerciseCatalogueQuery.EQUIPMENT);String base=" FROM movement_catalogue m JOIN exercise_option o ON o.id=m.default_period_option_id WHERE m.active AND m.review_status='REVIEWED' AND o.period_kind='STRENGTH' AND "+ExerciseCatalogueQuery.selectableOption("o");
  try{if(jdbc.queryForObject("SELECT count(*)"+base,Long.class)==0)throw ExerciseCatalogueQuery.unavailable();var args=new MapSqlParameterSource().addValue("limit",size).addValue("offset",(long)page*size);String filter=base+" AND "+ExerciseCatalogueQuery.matches(value,"m.display_name","m.aliases",args);if(bodyRegion!=null){filter+=" AND m.body_region=:body";args.addValue("body",bodyRegion);}if(equipment!=null){filter+=" AND m.equipment_type=:equipment";args.addValue("equipment",equipment);}long total=named.queryForObject("SELECT count(*)"+filter,args,Long.class);var rows=named.query("SELECT m.id,m.display_name,m.body_region,m.equipment_type,m.load_basis,m.default_period_option_id,m.allowed_fields::text"+filter+" ORDER BY lower(m.display_name),m.id LIMIT :limit OFFSET :offset",args,(r,n)->new Response(r.getString("id"),r.getString("display_name"),r.getString("body_region"),r.getString("equipment_type"),r.getString("load_basis"),r.getString("default_period_option_id"),json.readValue(r.getString("allowed_fields"),new TypeReference<List<String>>(){})));return new ExerciseCataloguePage<>(rows,total,page,size,(long)page*size+rows.size()<total);
  }catch(DataAccessException e){var error=ExerciseCatalogueQuery.unavailable();error.initCause(e);throw error;}
 }
}
