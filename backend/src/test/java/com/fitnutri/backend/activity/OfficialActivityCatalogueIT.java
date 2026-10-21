package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.ApiExceptionHandler;
import com.fitnutri.backend.records.ExerciseCatalog;
import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @ActiveProfiles("integration-test")
class OfficialActivityCatalogueIT {
    private static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @Autowired ActivityReferenceImportService importer;@Autowired ExerciseCatalog catalogue;@Autowired ExerciseTypeRepository types;
    @Test void pinnedOfficialPdfAndAgePageImportRepresentative15WithoutChangingSourceMets() throws Exception {
        Path dir=Path.of(System.getenv().getOrDefault("TEST_REFERENCE_SOURCE_DIR",Path.of("..",".local","reference-sources").toString())).toAbsolutePath().normalize();Map<String,Resource> raw=new HashMap<>();for(String name:List.of("2024-adult-compendium.pdf","2024-adult-compendium-age-source.html"))raw.put(name,new FileSystemResource(dir.resolve(name)));
        var manifest=new ClassPathResource("data/manifests/compendium-2024-r1.json");var data=new ClassPathResource("data/compendium-2024-activities-r1.json");var report=importer.importActivityManifest(manifest,data,raw);assertEquals(15,report.importedCount());assertTrue(importer.importActivityManifest(manifest,data,raw).replayed());var selected=catalogue.listActiveTypes();assertEquals(15,selected.size());assertEquals(29,types.count());
        var expected=Map.of("walking",List.of("17160","3.50"),"brisk-walking",List.of("17200","4.80"),"jogging",List.of("12020","7.50"),"running",List.of("12030","8.50"),"dancing",List.of("03070","3.80"),"stair-climbing",List.of("17131","6.80"),"badminton",List.of("15030","5.50"));
        for(var row:selected){assertEquals(19,row.dataset().minAge());assertEquals(59,row.dataset().maxAge());assertTrue(row.dataset().verified());if(expected.containsKey(row.activityKey())){assertEquals(expected.get(row.activityKey()).get(0),row.sourceActivityCode());assertEquals(new BigDecimal(expected.get(row.activityKey()).get(1)),row.metValue());}}
        var running=selected.stream().filter(t->t.activityKey().equals("running")).findFirst().orElseThrow();assertTrue(running.displayName().contains("5.0–5.2 mph"));assertTrue(running.sourceDescription().contains("5.0 to 5.2 mph"));
        var mvc=MockMvcBuilders.standaloneSetup(new ActivityReferenceController(catalogue)).setControllerAdvice(new ApiExceptionHandler()).build();mvc.perform(get("/api/records/exercise/types")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(15)).andExpect(jsonPath("$[0].id").isString()).andExpect(jsonPath("$[0].dataset.verified").value(true)).andExpect(jsonPath("$[0].dataset.minAge").value(19));
    }
}
