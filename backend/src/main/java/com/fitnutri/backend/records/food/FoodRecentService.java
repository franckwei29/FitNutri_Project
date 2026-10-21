package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class FoodRecentService {
 private final JdbcTemplate jdbc;private final FoodSnapshotReader snapshots;
 public FoodRecentService(JdbcTemplate jdbc,FoodSnapshotReader snapshots){this.jdbc=jdbc;this.snapshots=snapshots;}
 /** limit is per section (default five); the combined response is capped at one hundred. */
 public FoodSnapshotReader.Page recentSinglesAndDishes(Long owner,int limit){
  if(limit<1||limit>100)throw BusinessException.invalid("limit","Choose 1 to 100 recent items.");int sectionLimit=Math.min(limit,50);
  String cte="""
   WITH singles AS (SELECT id,created_at,row_number() OVER(PARTITION BY food_reference_id ORDER BY created_at DESC,id DESC) AS reference_rank FROM food_record WHERE user_id=? AND deleted_at IS NULL AND dish_id IS NULL AND food_reference_id IS NOT NULL AND data_quality='CURRENT'),
   roots AS (SELECT id,created_at,'single' AS kind FROM singles WHERE reference_rank=1 UNION ALL SELECT id,created_at,'dish' AS kind FROM food_dish_record WHERE user_id=? AND deleted_at IS NULL),
   ranked AS (SELECT *,row_number() OVER(PARTITION BY kind ORDER BY created_at DESC,id DESC) AS section_rank FROM roots)
   """;
  long total=jdbc.queryForObject(cte+"SELECT count(*) FROM roots",Long.class,owner,owner);
  var rows=jdbc.queryForList(cte+"SELECT id,kind FROM ranked WHERE section_rank<=? ORDER BY created_at DESC,kind,id DESC",owner,owner,sectionLimit);
  var result=snapshots.batchOwnedRoots(owner,rows.stream().map(r->new FoodSnapshotReader.Key(r.get("kind").toString(),((Number)r.get("id")).longValue())).toList());
  return new FoodSnapshotReader.Page(result,total,0,sectionLimit*2,total>result.size());
 }
}
