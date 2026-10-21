package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class ExerciseRecentService{
 private final JdbcTemplate jdbc;private final ExerciseEntryReadService reads;
 public ExerciseRecentService(JdbcTemplate jdbc,ExerciseEntryReadService reads){this.jdbc=jdbc;this.reads=reads;}
 public ExerciseEntryReadService.Page recentEntries(Long owner,int limit){if(limit<1||limit>100)throw BusinessException.invalid("limit","Choose1 to100 recent items.");int section=Math.min(limit,50);String cte="WITH quick AS(SELECT id,created_at,row_number() OVER(PARTITION BY exercise_option_id ORDER BY created_at DESC,id DESC) AS option_rank FROM exercise_record WHERE user_id=? AND workout_id IS NULL AND deleted_at IS NULL AND exercise_option_id IS NOT NULL AND data_quality='CURRENT'), roots AS(SELECT id,created_at,'quick' AS kind FROM quick WHERE option_rank=1 UNION ALL SELECT id,created_at,'workout' AS kind FROM workout_record WHERE user_id=? AND deleted_at IS NULL), ranked AS(SELECT *,row_number() OVER(PARTITION BY kind ORDER BY created_at DESC,id DESC) AS section_rank FROM roots) ";long total=jdbc.queryForObject(cte+"SELECT count(*) FROM roots",Long.class,owner,owner);var keys=jdbc.query(cte+"SELECT kind,id FROM ranked WHERE section_rank<=? ORDER BY created_at DESC,kind,id DESC",(r,n)->new ExerciseEntryReadService.Key(r.getString("kind"),r.getLong("id")),owner,owner,section);var rows=reads.batchOwnedRoots(owner,keys);return new ExerciseEntryReadService.Page(rows,total,0,section*2,total>rows.size());}
}
