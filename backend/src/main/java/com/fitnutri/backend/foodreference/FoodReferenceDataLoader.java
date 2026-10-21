package com.fitnutri.backend.foodreference;

import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Explicit manifest import only. No count-based skip, unknown-source seed or swallowed startup failure. */
@org.springframework.core.annotation.Order(100)
@Component @ConditionalOnProperty(name="app.catalogue.import-food",havingValue="true")
public class FoodReferenceDataLoader implements ApplicationRunner {
    private final ReferenceImportService importer;
    private final ObjectMapper json;
    private final String sourceDirectory;
    public FoodReferenceDataLoader(ReferenceImportService importer,ObjectMapper json,@Value("${app.catalogue.source-directory:}") String sourceDirectory) {this.importer=importer;this.json=json;this.sourceDirectory=sourceDirectory;}
    @Override public void run(ApplicationArguments args) throws Exception {
        if(sourceDirectory.isBlank()) throw new IllegalStateException("Set REFERENCE_SOURCE_DIR to the pinned official files before enabling food import.");
        var manifest=new ClassPathResource("data/manifests/ausnut-2023-r1.json");ReferenceManifestReader.Manifest metadata;
        try(var in=manifest.getInputStream()){metadata=json.readValue(in,ReferenceManifestReader.Manifest.class);}
        if(!safe(metadata.recordsFile())) throw new IllegalStateException("Invalid catalogue records filename.");
        Path directory=Path.of(sourceDirectory).toAbsolutePath().normalize();Map<String,Resource> sources=new HashMap<>();
        for(var source:metadata.sourceFiles()) {if(!safe(source.filename())) throw new IllegalStateException("Invalid catalogue source filename.");sources.put(source.filename(),new FileSystemResource(directory.resolve(source.filename())));}
        importer.importManifest(manifest,new ClassPathResource("data/"+metadata.recordsFile()),sources);
    }
    private static boolean safe(String filename) {return filename!=null&&filename.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,200}")&&!filename.contains("..");}
}
