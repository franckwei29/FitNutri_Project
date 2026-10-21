package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.BusinessException;
import java.time.Clock;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ActivityReferenceImportService {
    private final ActivityManifestReader reader;
    private final ActivityDatasetRepository datasets;
    private final ExerciseTypeRepository types;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Clock clock;
    public ActivityReferenceImportService(ActivityManifestReader reader,ActivityDatasetRepository datasets,ExerciseTypeRepository types,JdbcTemplate jdbc,PlatformTransactionManager manager,Clock clock) {this.reader=reader;this.datasets=datasets;this.types=types;this.jdbc=jdbc;transaction=new TransactionTemplate(manager);this.clock=clock;}
    public record ImportReport(String datasetId,int importedCount,boolean replayed) {}
    public ImportReport importActivityManifest(Resource manifest,Resource records,Map<String,Resource> sources) {var prepared=reader.prepare(manifest,records,sources);return transaction.execute(status->store(prepared));}
    private ImportReport store(ActivityManifestReader.Prepared p) {
        jdbc.execute("SELECT pg_advisory_xact_lock(6892032742)");var m=p.manifest();String note="Manifest SHA256: "+p.manifestSha256()+"; "+m.sourceNote();
        var existing=datasets.findByNameAndReleaseAndRevision(m.datasetName(),m.release(),m.revision());
        if(existing.isPresent()) {var d=existing.get();if(!d.isVerified()||!Objects.equals(d.getContentSha256(),m.recordsSha256())||!Objects.equals(d.getSourceUrl(),m.sourceUrl())||!Objects.equals(d.getSourceNote(),note)||!Objects.equals(d.getMinAge(),m.minAge())||!Objects.equals(d.getMaxAge(),m.maxAge())) throw new BusinessException(HttpStatus.CONFLICT,"DATASET_VERSION_CONFLICT","This activity version has different immutable content or source evidence. Use a new reviewed revision.");return new ImportReport(d.getId().toString(),0,true);}
        var d=new ActivityDataset();d.setName(m.datasetName());d.setRelease(m.release());d.setRevision(m.revision());d.setSourceUrl(m.sourceUrl());d.setContentSha256(m.recordsSha256());d.setMinAge(m.minAge());d.setMaxAge(m.maxAge());d.setSourceNote(note);d.setVerified(true);d.setImportedAt(clock.instant());datasets.saveAndFlush(d);
        jdbc.queryForList("SELECT id FROM exercise_type WHERE is_active ORDER BY id FOR UPDATE",Long.class);types.deactivateActiveVerified();
        var rows=new ArrayList<ExerciseType>();for(var a:p.activities()){var t=new ExerciseType();t.setDataset(d);t.setActivityKey(a.activityKey());t.setDisplayName(a.displayName());t.setSourceActivityCode(a.sourceActivityCode());t.setSourceDescription(a.sourceDescription());t.setMetValue(a.metValue());t.setActive(true);rows.add(t);}types.saveAllAndFlush(rows);return new ImportReport(d.getId().toString(),rows.size(),false);
    }
}
