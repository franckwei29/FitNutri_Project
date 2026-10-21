package com.fitnutri.backend.activity;

import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component @org.springframework.core.annotation.Order(100) @ConditionalOnProperty(name="app.catalogue.import-activity",havingValue="true")
public class ActivityReferenceDataLoader implements ApplicationRunner {
    private final ActivityReferenceImportService importer;private final ObjectMapper json;private final String sourceDirectory;private final String manifestPath;
    public ActivityReferenceDataLoader(ActivityReferenceImportService importer,ObjectMapper json,@Value("${app.catalogue.source-directory:}")String sourceDirectory,@Value("${app.catalogue.activity-manifest:data/manifests/compendium-2024-r1.json}")String manifestPath){this.importer=importer;this.json=json;this.sourceDirectory=sourceDirectory;this.manifestPath=manifestPath;}
    @Override public void run(ApplicationArguments args) throws Exception {
        if(sourceDirectory.isBlank())throw new IllegalStateException("Set REFERENCE_SOURCE_DIR to pinned official files before enabling activity import.");
        if(!Set.of("data/manifests/compendium-2024-r1.json","data/manifests/compendium-2024-r2.json").contains(manifestPath))throw new IllegalStateException("Choose a published reviewed activity manifest.");
        var manifest=new ClassPathResource(manifestPath);ActivityManifestReader.Manifest metadata;
        try(var in=manifest.getInputStream()){metadata=json.readValue(in,ActivityManifestReader.Manifest.class);}
        if(!safe(metadata.recordsFile()))throw new IllegalStateException("Invalid activity records filename.");var directory=Path.of(sourceDirectory).toAbsolutePath().normalize();Map<String,Resource> sources=new HashMap<>();
        for(var source:metadata.sourceFiles()){if(!safe(source.filename()))throw new IllegalStateException("Invalid activity source filename.");sources.put(source.filename(),new FileSystemResource(directory.resolve(source.filename())));}
        importer.importActivityManifest(manifest,new ClassPathResource("data/"+metadata.recordsFile()),sources);
    }
    private static boolean safe(String s){return s!=null&&s.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,200}")&&!s.contains("..");}
}
