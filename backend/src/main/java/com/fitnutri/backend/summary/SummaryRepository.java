package com.fitnutri.backend.summary;

import com.fitnutri.backend.summary.SummaryModels.MetricAggregate;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Owner/date/nondeleted GROUP BY in PostgreSQL. SUM(NULL) remains NULL; no float or zero-fill authority. */
@Repository
public class SummaryRepository {
    private final JdbcTemplate jdbc;
    public SummaryRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record FoodDay(LocalDate date,long count,MetricAggregate intake,MetricAggregate protein,MetricAggregate fat,MetricAggregate carbs,long entryCount){public FoodDay(LocalDate date,long count,MetricAggregate intake,MetricAggregate protein,MetricAggregate fat,MetricAggregate carbs){this(date,count,intake,protein,fat,carbs,count);}}
    public record ExerciseDay(LocalDate date,long count,MetricAggregate expenditure,BigDecimal activityMinutes,long entryCount){public ExerciseDay(LocalDate date,long count,MetricAggregate expenditure,BigDecimal activityMinutes){this(date,count,expenditure,activityMinutes,count);}}
    public List<FoodDay> foodDays(Long owner,LocalDate from,LocalDate to){
        return jdbc.query("SELECT recorded_date,count(*) records,count(DISTINCT CASE WHEN dish_id IS NULL THEN 'single:'||id::text ELSE 'dish:'||dish_id::text END) entry_count,"+metricSql("energy_kj","intake")+","+metricSql("protein_grams","protein")+","+metricSql("fat_grams","fat")+","+metricSql("carbs_grams","carbs")+" FROM food_record WHERE user_id=? AND deleted_at IS NULL AND recorded_date BETWEEN ? AND ? GROUP BY recorded_date ORDER BY recorded_date",(r,n)->new FoodDay(r.getObject("recorded_date",LocalDate.class),r.getLong("records"),metric(r,"intake"),metric(r,"protein"),metric(r,"fat"),metric(r,"carbs"),r.getLong("entry_count")),owner,from,to);
    }
    public List<ExerciseDay> exerciseDays(Long owner,LocalDate from,LocalDate to){
        return jdbc.query("SELECT recorded_date,count(*) records,count(DISTINCT CASE WHEN workout_id IS NULL THEN 'quick:'||id::text ELSE 'workout:'||workout_id::text END) entry_count,"+metricSql("estimated_kj","expenditure")+",sum(duration_minutes) FILTER(WHERE duration_minutes>0) activity_minutes FROM exercise_record WHERE user_id=? AND deleted_at IS NULL AND recorded_date BETWEEN ? AND ? GROUP BY recorded_date ORDER BY recorded_date",(r,n)->new ExerciseDay(r.getObject("recorded_date",LocalDate.class),r.getLong("records"),metric(r,"expenditure"),r.getBigDecimal("activity_minutes"),r.getLong("entry_count")),owner,from,to);
    }
    private static String metricSql(String column,String alias){return "sum("+column+") "+alias+"_sum,count("+column+") "+alias+"_known,count(*) FILTER(WHERE data_quality='LEGACY_UNVERIFIED' AND "+column+" IS NOT NULL) "+alias+"_legacy";}
    private static MetricAggregate metric(ResultSet r,String alias) throws SQLException{return MetricAggregate.of(r.getLong("records"),r.getLong(alias+"_known"),r.getBigDecimal(alias+"_sum"),r.getLong(alias+"_legacy"));}
}
