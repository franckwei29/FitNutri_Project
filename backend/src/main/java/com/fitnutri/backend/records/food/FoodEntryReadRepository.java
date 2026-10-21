package com.fitnutri.backend.records.food;
import com.fitnutri.backend.records.MealType;
import java.time.LocalDate;
import java.sql.Types;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
@Repository
public class FoodEntryReadRepository {
 private final NamedParameterJdbcTemplate jdbc;
 public FoodEntryReadRepository(JdbcTemplate jdbc){this.jdbc=new NamedParameterJdbcTemplate(jdbc);}
 public record Keys(List<FoodSnapshotReader.Key> values,long total){}
 public Keys listRoots(Long owner,LocalDate from,LocalDate to,MealType meal,int page,int size){
  var args=new MapSqlParameterSource().addValue("owner",owner).addValue("from",from).addValue("to",to).addValue("meal",meal==null?null:meal.name(),Types.VARCHAR).addValue("limit",size).addValue("offset",(long)page*size);
  String roots="""
   WITH roots AS (
    SELECT id,recorded_date,created_at,'single' AS kind FROM food_record WHERE user_id=:owner AND dish_id IS NULL AND deleted_at IS NULL AND recorded_date BETWEEN :from AND :to AND (:meal IS NULL OR meal_type=:meal)
    UNION ALL
    SELECT id,recorded_date,created_at,'dish' AS kind FROM food_dish_record WHERE user_id=:owner AND deleted_at IS NULL AND recorded_date BETWEEN :from AND :to AND (:meal IS NULL OR meal_type=:meal)
   )
   """;
  long total=jdbc.queryForObject(roots+"SELECT count(*) FROM roots",args,Long.class);var keys=jdbc.query(roots+"SELECT id,kind FROM roots ORDER BY recorded_date DESC,created_at DESC,kind,id DESC LIMIT :limit OFFSET :offset",args,(r,n)->new FoodSnapshotReader.Key(r.getString("kind"),r.getLong("id")));return new Keys(List.copyOf(keys),total);
 }
}
