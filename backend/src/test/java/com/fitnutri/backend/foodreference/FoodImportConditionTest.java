package com.fitnutri.backend.foodreference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FoodImportConditionTest {
    @Test void defaultStartupNeverImportsAnUnverifiedOrImplicitCatalogue() {new ApplicationContextRunner().withUserConfiguration(FoodReferenceDataLoader.class).run(c->assertFalse(c.containsBean("foodReferenceDataLoader")));}
    @Test void explicitlyEnabledImportWithoutSourceDirectoryFailsBeforeAnyWrite() {
        var importer=mock(ReferenceImportService.class);var loader=new FoodReferenceDataLoader(importer,JsonMapper.builder().build(),"");assertThrows(IllegalStateException.class,()->loader.run(new DefaultApplicationArguments()));verifyNoInteractions(importer);
    }
}
