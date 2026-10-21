package com.fitnutri.backend.activity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ActivityImportConditionTest {
    @Test void defaultStartupDoesNotSeedActivities(){new ApplicationContextRunner().withUserConfiguration(ActivityReferenceDataLoader.class).run(c->assertFalse(c.containsBean("activityReferenceDataLoader")));}
    @Test void explicitImportNeedsSourceDirectoryBeforeAnyWrite(){var service=mock(ActivityReferenceImportService.class);var loader=new ActivityReferenceDataLoader(service,JsonMapper.builder().build(),"","data/manifests/compendium-2024-r1.json");assertThrows(IllegalStateException.class,()->loader.run(new DefaultApplicationArguments()));verifyNoInteractions(service);}
    @Test void unsupportedManifestCannotRedirectSourceImport(){var service=mock(ActivityReferenceImportService.class);var loader=new ActivityReferenceDataLoader(service,JsonMapper.builder().build(),".","../../unreviewed.json");assertThrows(IllegalStateException.class,()->loader.run(new DefaultApplicationArguments()));verifyNoInteractions(service);}
    @Test void defaultStartupDoesNotSeedUxProducts(){new ApplicationContextRunner().withUserConfiguration(com.fitnutri.backend.activity.catalogue.ExerciseOptionDataLoader.class).run(c->assertFalse(c.containsBean("exerciseOptionDataLoader")));}
}
