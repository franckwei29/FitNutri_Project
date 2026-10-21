package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.ApiExceptionHandler;
import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual official files must be present and match pinned SHA; no network or substitute fixture fallback. */
@SpringBootTest @ActiveProfiles("integration-test")
class OfficialFoodCatalogueIT {
    private static final TestDatabase DB=TestDatabase.create();static {Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @Autowired private ReferenceImportService importer;
    @Autowired private FoodReferenceService service;
    @Autowired private FoodReferenceRepository foods;
    @Test void officialSourceFilesImport3741FoodsAndMatchIndependentReviewedSamples() throws Exception {
        Path directory=Path.of(System.getenv().getOrDefault("TEST_REFERENCE_SOURCE_DIR",Path.of("..",".local","reference-sources").toString())).toAbsolutePath().normalize();Map<String,Resource> raw=new HashMap<>();
        for(String name:List.of("ausnut-2023-food-details.xlsx","ausnut-2023-food-nutrient-profiles.xlsx","ausnut-2023-list-of-nutrients.xlsx")){assertTrue(Files.isRegularFile(directory.resolve(name)),"Prepare pinned official source files first: "+name);raw.put(name,new FileSystemResource(directory.resolve(name)));}
        var manifest=new ClassPathResource("data/manifests/ausnut-2023-r1.json");var records=new ClassPathResource("data/ausnut-2023-foods-r1.json");var report=importer.importManifest(manifest,records,raw);assertEquals(3741,report.importedCount());assertEquals(3741,foods.countSelectable());assertEquals(3741,foods.count());assertTrue(importer.importManifest(manifest,records,raw).replayed());assertEquals(3741,foods.count());
        var rice=service.getActiveByPublicKey("F007661");assertEquals(new BigDecimal("659.000"),rice.per100g().energyKj());assertEquals(new BigDecimal("3.100"),rice.per100g().proteinGrams());assertEquals(new BigDecimal("0.200"),rice.per100g().fatGrams());assertEquals(new BigDecimal("34.700"),rice.per100g().carbsGrams());assertTrue(rice.dataset().verified());assertEquals("AUSNUT",rice.dataset().name());assertEquals("2023",rice.dataset().release());
        var chicken=service.getActiveByPublicKey("F002594");assertEquals(new BigDecimal("412.000"),chicken.per100g().energyKj());assertEquals(new BigDecimal("22.500"),chicken.per100g().proteinGrams());assertEquals(new BigDecimal("0.000"),chicken.per100g().carbsGrams());
        var banana=service.getActiveByPublicKey("F000262");assertEquals(new BigDecimal("399.000"),banana.per100g().energyKj());
        var mvc=MockMvcBuilders.standaloneSetup(new FoodReferenceController(service,new FoodCalculationService(),org.mockito.Mockito.mock(com.fitnutri.backend.records.OwnedPreviewService.class),org.mockito.Mockito.mock(com.fitnutri.backend.common.CurrentUserService.class))).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(get("/api/food-reference/search").param("query","rice cooked").param("size","2")).andExpect(status().isOk()).andExpect(jsonPath("$.results.length()").value(2)).andExpect(jsonPath("$.results[0].referenceId").isString()).andExpect(jsonPath("$.results[0].dataset.verified").value(true)).andExpect(jsonPath("$.results[0].per100g.energyKj").isNumber()).andExpect(jsonPath("$.hasMore").value(true));
        mvc.perform(get("/api/food-reference/by-id/"+rice.referenceId()+"/nutrition").param("grams","150")).andExpect(status().isOk()).andExpect(jsonPath("$.nutrients.energyKj").value(988.5)).andExpect(jsonPath("$.nutrients.proteinGrams").value(4.65));
        mvc.perform(get("/api/food-reference/by-id/"+rice.referenceId())).andExpect(status().isOk()).andExpect(jsonPath("$.publicFoodKey").value("F007661")).andExpect(jsonPath("$.per100g.energyKj").value(659));
    }
}
