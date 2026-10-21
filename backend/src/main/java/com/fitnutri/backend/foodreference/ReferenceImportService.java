package com.fitnutri.backend.foodreference;

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
public class ReferenceImportService {
    private final ReferenceManifestReader reader;
    private final FoodDatasetRepository datasets;
    private final FoodReferenceRepository foods;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Clock clock;
    public ReferenceImportService(ReferenceManifestReader reader,FoodDatasetRepository datasets,FoodReferenceRepository foods,JdbcTemplate jdbc,PlatformTransactionManager transactions,Clock clock) {
        this.reader=reader;this.datasets=datasets;this.foods=foods;this.jdbc=jdbc;this.transactions=new TransactionTemplate(transactions);this.clock=clock;
    }
    public record ImportReport(String datasetId,int importedCount,boolean replayed) {}
    public ImportReport importManifest(Resource manifest,Resource records,Map<String,Resource> sources) {
        var prepared=reader.prepare(manifest,records,sources);
        return transactions.execute(status->importPrepared(prepared));
    }
    private ImportReport importPrepared(ReferenceManifestReader.Prepared prepared) {
        // Global catalogue writes serialize; row locks use the same ascending reference order as record writers.
        jdbc.execute("SELECT pg_advisory_xact_lock(6892032741)");
        var m=prepared.manifest();String note="Manifest SHA256: "+prepared.manifestSha256()+"; "+m.sourceNote();
        var existing=datasets.findByNameAndReleaseAndRevision(m.datasetName(),m.release(),m.revision());
        if(existing.isPresent()) {
            var d=existing.get();
            if(!d.isVerified()||!Objects.equals(d.getContentSha256(),m.recordsSha256())||!Objects.equals(d.getSourceUrl(),m.sourceUrl())||!Objects.equals(d.getNutrientPolicy(),m.nutrientPolicy())||!Objects.equals(d.getSourceNote(),note)) throw new BusinessException(HttpStatus.CONFLICT,"DATASET_VERSION_CONFLICT","This catalogue version has different immutable content or source evidence. Use a new reviewed revision.");
            return new ImportReport(d.getId().toString(),0,true);
        }
        var dataset=new FoodDataset();dataset.setName(m.datasetName());dataset.setRelease(m.release());dataset.setRevision(m.revision());dataset.setSourceUrl(m.sourceUrl());dataset.setContentSha256(m.recordsSha256());dataset.setNutrientPolicy(m.nutrientPolicy());dataset.setSourceNote(note);dataset.setVerified(true);dataset.setImportedAt(clock.instant());datasets.saveAndFlush(dataset);
        jdbc.queryForList("SELECT id FROM food_reference WHERE is_active ORDER BY id FOR UPDATE",Long.class);
        foods.deactivateActiveVerified();
        List<FoodReference> rows=new ArrayList<>(prepared.foods().size());
        for(var row:prepared.foods()) {
            var f=new FoodReference();f.setDataset(dataset);f.setPublicFoodKey(row.publicFoodKey());f.setSurveyId(row.surveyId());f.setOfficialName(row.officialName());f.setDisplayName(row.displayName());f.setFoodName(row.officialName());f.setDescription(row.description());f.setEnergyKj100g(row.energyKj100g());f.setProteinG100g(row.proteinG100g());f.setFatG100g(row.fatG100g());f.setCarbsG100g(row.carbsG100g());
            f.setFiberG(row.fiberG100g()==null?null:row.fiberG100g().doubleValue());f.setSugarG(row.sugarG100g()==null?null:row.sugarG100g().doubleValue());f.setSodiumMg(row.sodiumMg100g()==null?null:row.sodiumMg100g().doubleValue());f.setActive(true);rows.add(f);
        }
        foods.saveAllAndFlush(rows);return new ImportReport(dataset.getId().toString(),rows.size(),false);
    }
}
