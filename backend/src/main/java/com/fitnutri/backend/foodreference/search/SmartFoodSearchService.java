package com.fitnutri.backend.foodreference.search;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

/** Bounded, server-paginated search of verified official references and reviewed UX metadata. */
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class SmartFoodSearchService {
 private final NamedParameterJdbcTemplate jdbc;private final FoodReferenceRepository foods;private final ObjectMapper json;
 public SmartFoodSearchService(JdbcTemplate jdbc,FoodReferenceRepository foods,ObjectMapper json){this.jdbc=new NamedParameterJdbcTemplate(jdbc);this.foods=foods;this.json=json;}
 public record Candidate(FoodReferenceResponse reference,String shortName,String matchKind,List<String> distinguishers){}
 public record Facet(String key,String question,List<String> values){}
 public record Response(List<Candidate> results,long totalMatches,int page,int size,boolean hasMore,Facet nextQuestion,String metadataRevision){}
 private static final List<String> FACET_ORDER=List.of("state","part","cookingMethod","addedFat","dairyFat","type");
 private static final Map<String,String> LABELS=Map.ofEntries(Map.entry("RAW","Raw"),Map.entry("COOKED","Cooked"),Map.entry("BREAST","Breast"),Map.entry("THIGH","Thigh"),Map.entry("FRIED","Fried"),Map.entry("BOILED","Boiled"),Map.entry("YES","Added fat"),Map.entry("NO","No added fat"),Map.entry("WHITE","White"),Map.entry("BROWN","Brown"),Map.entry("SKIM","Skim"),Map.entry("REDUCED","Reduced fat"),Map.entry("REGULAR","Regular fat"));
 public Response search(String value,Map<String,String> facets,String mode,int page,int size,Long owner){
  var query=SmartFoodQuery.parse(value,facets);
  if(!Set.of("SUGGESTED","ALL","APPROXIMATE").contains(mode==null?"":mode))throw BusinessException.invalid("mode","Choose a supported search mode.");
  if(page<0||size<1||size>100)throw BusinessException.invalid("page","Choose a nonnegative page and a size from 1 to 100.");
  if(owner==null||owner<1)throw BusinessException.invalid("owner","A current account is required.");
  try {
   if(foods.countSelectable()==0)throw unavailable();
   var params=new MapSqlParameterSource().addValue("owner",owner).addValue("query",query.normalised()).addValue("limit",size).addValue("offset",(long)page*size);
   String aliases="SELECT a.normalised_text FROM food_search_alias a WHERE a.review_status='REVIEWED' AND a.metadata_revision=p.metadata_revision AND (a.profile_id=p.id OR (a.profile_id IS NULL AND a.concept_key=p.concept_key))";
   String cte="""
    WITH candidates AS (
     SELECT f.id,f.display_name,coalesce(p.short_name_en,f.display_name) AS short_name,p.metadata_revision,
     coalesce(p.facets,'{}'::jsonb) AS facets,coalesce(p.common_rank,0) AS common_rank,
     p.id AS profile_id,
     lower(regexp_replace(f.display_name,'[^[:alnum:]]+',' ','g')) AS official_text,
     lower(coalesce(p.short_name_en,'')) || ' ' || coalesce((SELECT string_agg(normalised_text,' ') FROM (%s) a),'') AS alias_text,
     (lower(regexp_replace(f.display_name,'[^[:alnum:]]+',' ','g'))=:query OR EXISTS(SELECT 1 FROM (%s) a WHERE a.normalised_text=:query)) AS exact,
     (SELECT max(r.created_at) FROM food_record r WHERE r.user_id=:owner AND r.food_reference_id=f.id AND r.dish_id IS NULL AND r.deleted_at IS NULL) AS recent
     FROM food_reference f JOIN food_dataset d ON d.id=f.dataset_id
     LEFT JOIN food_search_profile p ON p.reference_id=f.id AND p.is_active AND p.review_status='REVIEWED'
     WHERE f.is_active AND d.verified AND f.energy_kj_100g IS NOT NULL AND f.protein_g_100g IS NOT NULL AND f.fat_g_100g IS NOT NULL AND f.carbs_g_100g IS NOT NULL
    ), matched AS (SELECT c.* FROM candidates c WHERE %s)
    """;
   String matching;
   if(query.chinese()) {
    params.addValue("chinese",query.normalised());
    // Longest reviewed Chinese alias wins; a generic substring must not erase a specific concept.
    matching="profile_id IS NOT NULL AND EXISTS(SELECT 1 FROM food_search_alias a JOIN food_search_profile ap ON ap.id=a.profile_id OR (a.profile_id IS NULL AND ap.concept_key=a.concept_key AND ap.metadata_revision=a.metadata_revision) WHERE ap.id=c.profile_id AND a.review_status='REVIEWED' AND a.metadata_revision=ap.metadata_revision AND a.locale='zh' AND position(a.normalised_text in :chinese)>0 AND length(a.normalised_text)=(SELECT max(length(z.normalised_text)) FROM food_search_alias z JOIN food_search_profile zp ON zp.id=z.profile_id OR (z.profile_id IS NULL AND zp.concept_key=z.concept_key AND zp.metadata_revision=z.metadata_revision) JOIN food_reference zf ON zf.id=zp.reference_id JOIN food_dataset zd ON zd.id=zf.dataset_id WHERE z.review_status='REVIEWED' AND z.locale='zh' AND zp.is_active AND zp.review_status='REVIEWED' AND z.metadata_revision=zp.metadata_revision AND zf.is_active AND zd.verified AND position(z.normalised_text in :chinese)>0))";
    if(!query.baseWords().isEmpty())matching+=" AND "+tokens(query.baseWords(),"official_text || ' ' || alias_text",query,params,"mixed");
   } else {
    String base=tokens(query.baseWords(),"official_text || ' ' || alias_text",query,params,"base");
    String original=tokens(query.originalWords(),"official_text",query,params,"original");
    matching="((profile_id IS NOT NULL AND "+base+") OR (profile_id IS NULL AND "+original+"))";
   }
   for(var entry:query.facets().entrySet()){
    params.addValue("facet"+entry.getKey(),entry.getValue());
    // Unknown metadata is not evidence: fallback must satisfy the complete official query.
    matching+=" AND ((facets->>'"+entry.getKey()+"')=:facet"+entry.getKey()+" OR ((facets->>'"+entry.getKey()+"') IS NULL AND "+(query.chinese()?"false":tokens(query.originalWords(),"official_text",query,params,"fallback"+entry.getKey()))+"))";
   }
   String sql=cte.formatted(aliases,aliases,matching);
   long total=jdbc.queryForObject(sql+" SELECT count(*) FROM matched",params,Long.class);
   Facet question=null;
   if(!mode.equals("APPROXIMATE"))for(String key:FACET_ORDER)if(!query.facets().containsKey(key)){
    var values=jdbc.queryForList(sql+" SELECT DISTINCT facets->>'"+key+"' AS value FROM matched WHERE jsonb_exists(facets,'"+key+"') ORDER BY value",params,String.class);
    if(values.size()>1){question=new Facet(key,"Choose "+switch(key){case "state"->"raw or cooked";case "part"->"the cut";case "cookingMethod"->"the cooking method";case "addedFat"->"whether fat was added";case "dairyFat"->"the milk fat level";default->"the food type";}+" (or choose an approximate match).",List.copyOf(values));break;}
   }
   var rows=jdbc.queryForList(sql+" SELECT id,short_name,facets::text,exact FROM matched ORDER BY exact DESC,recent DESC NULLS LAST,common_rank DESC,lower(display_name),id LIMIT :limit OFFSET :offset",params);
   Map<Long,FoodReference> references=new HashMap<>();var ids=rows.stream().map(r->((Number)r.get("id")).longValue()).toList();
   if(!ids.isEmpty())for(var f:foods.findAllWithDatasetById(ids))references.put(f.getId(),f);
   List<Candidate> result=new ArrayList<>();
   for(var row:rows){var f=references.get(((Number)row.get("id")).longValue());if(f==null)throw unavailable();var attributes=json.readTree(row.get("facets").toString());List<String> differences=new ArrayList<>();for(String key:FACET_ORDER)if(attributes.has(key))differences.add(LABELS.getOrDefault(attributes.get(key).asText(),attributes.get(key).asText()));result.add(new Candidate(FoodReferenceResponse.from(f),row.get("short_name").toString(),mode.equals("APPROXIMATE")?"APPROXIMATE":Boolean.TRUE.equals(row.get("exact"))?"EXACT":"RELATED",List.copyOf(differences)));}
   var revisions=jdbc.queryForList("SELECT DISTINCT p.metadata_revision FROM food_search_profile p JOIN food_reference f ON f.id=p.reference_id JOIN food_dataset d ON d.id=f.dataset_id WHERE p.is_active AND p.review_status='REVIEWED' AND f.is_active AND d.verified ORDER BY p.metadata_revision",params,String.class);
   return new Response(List.copyOf(result),total,page,size,(long)page*size+result.size()<total,question,revisions.size()==1?revisions.getFirst():null);
  }catch(DataAccessException ex){var error=unavailable();error.initCause(ex);throw error;}
 }
 private static String tokens(List<String> words,String field,SmartFoodQuery query,MapSqlParameterSource params,String namespace){
  if(words.isEmpty())return "false";List<String> parts=new ArrayList<>();
  for(int i=0;i<words.size();i++){String token=words.get(i),key=namespace+i;boolean prefix=query.prefix()&&i==words.size()-1&&query.normalised().endsWith(token)&&token.length()>=2;params.addValue(key,"(^|[^[:alnum:]])"+token+(prefix?"":"($|[^[:alnum:]])"));parts.add("("+field+") ~ :"+key);}
  return "("+String.join(" AND ",parts)+")";
 }
 private static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","The verified food catalogue is unavailable. Try again later.");}
}
