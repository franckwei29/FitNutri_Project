package com.fitnutri.backend.capabilities;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class CapabilitiesRepository {
    private final JdbcTemplate jdbc;
    public CapabilitiesRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Snapshot(long verifiedCount,long usableCount,String selectedIds,String datasetVersions){}
    public Snapshot food(){return read("food_reference","food_dataset","r.energy_kj_100g IS NOT NULL AND r.protein_g_100g IS NOT NULL AND r.fat_g_100g IS NOT NULL AND r.carbs_g_100g IS NOT NULL");}
    public Snapshot exercise(){return read("exercise_type","activity_dataset","r.source_activity_code IS NOT NULL AND btrim(r.source_activity_code)<>'' AND r.source_description IS NOT NULL AND btrim(r.source_description)<>'' AND d.min_age IS NOT NULL AND d.max_age IS NOT NULL");}
    private Snapshot read(String table,String dataset,String valid){String where="d.verified AND "+valid;return jdbc.queryForObject("SELECT count(*) FILTER(WHERE d.verified) verified_count,count(*) FILTER(WHERE "+where+") usable_count,string_agg(r.id::text,',' ORDER BY r.id) FILTER(WHERE "+where+") selected_ids,string_agg(DISTINCT d.id::text||':'||d.content_sha256,',' ORDER BY d.id::text||':'||d.content_sha256) FILTER(WHERE "+where+") dataset_versions FROM "+table+" r JOIN "+dataset+" d ON d.id=r.dataset_id WHERE r.is_active",(r,n)->new Snapshot(r.getLong("verified_count"),r.getLong("usable_count"),r.getString("selected_ids"),r.getString("dataset_versions")));}
}
