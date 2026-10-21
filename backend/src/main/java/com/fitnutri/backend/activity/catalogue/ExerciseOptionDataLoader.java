package com.fitnutri.backend.activity.catalogue;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
@Component @Order(200) @ConditionalOnProperty(name="app.catalogue.import-exercise-options",havingValue="true")
public class ExerciseOptionDataLoader implements ApplicationRunner {
 private final ExerciseOptionImporter importer;
 public ExerciseOptionDataLoader(ExerciseOptionImporter importer){this.importer=importer;}
 @Override public void run(ApplicationArguments args){importer.importImmutableCatalogueVersion(new ClassPathResource("data/ux/exercise-options-v1.json"),new ClassPathResource("data/ux/movement-catalogue-v1.json"),false);}
}
