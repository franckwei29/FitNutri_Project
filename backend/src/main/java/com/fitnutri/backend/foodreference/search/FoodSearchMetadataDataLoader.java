package com.fitnutri.backend.foodreference.search;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
@Component @Order(200) @ConditionalOnProperty(name="app.catalogue.import-food-search",havingValue="true")
public class FoodSearchMetadataDataLoader implements ApplicationRunner {
 private final FoodSearchMetadataImporter importer;
 public FoodSearchMetadataDataLoader(FoodSearchMetadataImporter importer){this.importer=importer;}
 @Override public void run(ApplicationArguments args){importer.importRevision(new ClassPathResource("data/ux/food-search-metadata-v1.json"),false);}
}
