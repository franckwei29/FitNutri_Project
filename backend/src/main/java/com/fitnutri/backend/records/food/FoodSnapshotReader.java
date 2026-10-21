package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.NutrientValues;
import com.fitnutri.backend.records.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Owned stored inputs and snapshots only; batch loading prevents per-card child queries. */
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class FoodSnapshotReader {
 private final NamedParameterJdbcTemplate jdbc;private final ObjectMapper json;
 public FoodSnapshotReader(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=new NamedParameterJdbcTemplate(jdbc);this.json=json;}
 public record Key(String kind,long id){}
 public record Leaf(UUID clientEntryId,String referenceId,BigDecimal inputGrams,BigDecimal consumedGrams,NutrientValues nutrients){}
 public record Preview(String kind,UUID clientEntryId,List<Leaf> leaves,NutrientValues totals){}
 public record ComponentDetail(UUID clientEntryId,String referenceId,String name){}
 public record Root(String id,long version,LocalDate recordDate,String source,Instant createdAt,Instant updatedAt,DataQuality dataQuality,String displayName,Map<String,Object> input,Preview preview,int componentCount,List<ComponentDetail> componentDetails){}
 public record Page(List<Root> results,long totalMatches,int page,int size,boolean hasMore){}
 public Root detailForReuse(Long owner,String kind,long id){return batchOwnedRoots(owner,List.of(new Key(kind,id))).getFirst();}
 public List<Root> batchOwnedRoots(Long owner,List<Key> keys){
  if(keys==null||keys.size()>100||new HashSet<>(keys).size()!=keys.size())throw invalid();if(keys.isEmpty())return List.of();
  var singleIds=new ArrayList<Long>();var dishIds=new ArrayList<Long>();
  for(var key:keys){if(key==null||key.id()<1||!Set.of("single","dish").contains(key.kind()==null?"":key.kind()))throw invalid();(key.kind().equals("single")?singleIds:dishIds).add(key.id());}
  Map<Long,Map<String,Object>> singles=new HashMap<>(),dishes=new HashMap<>();Map<Long,List<Map<String,Object>>> children=new HashMap<>();
  if(!singleIds.isEmpty()){var args=new MapSqlParameterSource().addValue("owner",owner).addValue("ids",singleIds);for(var r:jdbc.queryForList("SELECT * FROM food_record WHERE user_id=:owner AND id IN (:ids) AND dish_id IS NULL AND deleted_at IS NULL",args))singles.put(((Number)r.get("id")).longValue(),r);}
  if(!dishIds.isEmpty()){var args=new MapSqlParameterSource().addValue("owner",owner).addValue("ids",dishIds);for(var r:jdbc.queryForList("SELECT * FROM food_dish_record WHERE user_id=:owner AND id IN (:ids) AND deleted_at IS NULL",args))dishes.put(((Number)r.get("id")).longValue(),r);for(var child:jdbc.queryForList("SELECT * FROM food_record WHERE user_id=:owner AND dish_id IN (:ids) AND deleted_at IS NULL ORDER BY dish_id,dish_position,id",args))children.computeIfAbsent(((Number)child.get("dish_id")).longValue(),k->new ArrayList<>()).add(child);}
  var result=new ArrayList<Root>();for(var key:keys){var r=(key.kind().equals("single")?singles:dishes).get(key.id());if(r==null)throw missing();result.add(fromRows(key.kind(),key.id(),r,children.getOrDefault(key.id(),List.of())));}return List.copyOf(result);
 }
 private Root fromRows(String kind,long id,Map<String,Object> r,List<Map<String,Object>> children){
  UUID uuid=(UUID)r.get("client_entry_id");var input=new LinkedHashMap<String,Object>();input.put("kind",kind.toUpperCase(Locale.ROOT));input.put("clientEntryId",uuid);input.put("mealType",r.get("meal_type"));List<Leaf> leaves=new ArrayList<>();DataQuality quality=kind.equals("dish")?DataQuality.CURRENT:DataQuality.valueOf(r.get("data_quality").toString());
  if(kind.equals("single")){input.put("referenceId",identity(r.get("food_reference_id")));input.put("quantityGrams",r.get("quantity_grams"));if(quality==DataQuality.CURRENT)selection(input,r);leaves.add(leaf(r));}
  else{input.put("name",r.get("name"));input.put("inputMode",r.get("input_mode"));var portion=json.readValue(r.get("portion_input_json").toString(),new TypeReference<LinkedHashMap<String,Object>>(){});portion.put("kind",r.get("portion_input_kind"));input.put("portion",Collections.unmodifiableMap(portion));if(r.get("origin_dish_id")!=null)input.put("originDishId",identity(r.get("origin_dish_id")));List<Map<String,Object>> ingredients=new ArrayList<>();for(var child:children){var ingredient=new LinkedHashMap<String,Object>();ingredient.put("clientEntryId",child.get("client_entry_id"));ingredient.put("referenceId",identity(child.get("food_reference_id")));ingredient.put("inputGrams",child.get("ingredient_input_grams"));selection(ingredient,child);ingredients.add(Collections.unmodifiableMap(ingredient));leaves.add(leaf(child));}input.put("ingredients",List.copyOf(ingredients));}
  return new Root(Long.toString(id),((Number)r.get("version")).longValue(),((java.sql.Date)r.get("recorded_date")).toLocalDate(),RecordSource.valueOf(r.get("source").toString()).canonical().name(),((java.sql.Timestamp)r.get("created_at")).toInstant(),((java.sql.Timestamp)r.get("updated_at")).toInstant(),quality,r.get(kind.equals("single")?"food_name":"name").toString(),Collections.unmodifiableMap(input),new Preview(kind.toUpperCase(Locale.ROOT),uuid,List.copyOf(leaves),DishCalculationService.sumRoundedLeaves(leaves.stream().map(Leaf::nutrients).toList())),leaves.size(),(kind.equals("single")?List.of(r):children).stream().map(row->new ComponentDetail((UUID)row.get("client_entry_id"),identity(row.get("food_reference_id")),row.get("food_name").toString())).toList());
 }
 private static void selection(Map<String,Object> input,Map<String,Object> r){input.put("selectionKind",r.get("selection_kind"));if(r.get("search_metadata_revision")!=null)input.put("searchMetadataRevision",r.get("search_metadata_revision"));}
 private static String identity(Object value){return value==null?null:value.toString();}
 private static Leaf leaf(Map<String,Object> r){var consumed=(BigDecimal)r.get("quantity_grams");return new Leaf((UUID)r.get("client_entry_id"),identity(r.get("food_reference_id")),r.get("dish_id")==null?consumed:(BigDecimal)r.get("ingredient_input_grams"),consumed,new NutrientValues((BigDecimal)r.get("energy_kj"),(BigDecimal)r.get("protein_grams"),(BigDecimal)r.get("fat_grams"),(BigDecimal)r.get("carbs_grams")));}
 private static BusinessException invalid(){return BusinessException.invalid("id","Choose bounded distinct single food or dish roots.");}
 private static BusinessException missing(){return new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Food record not found.");}
}
