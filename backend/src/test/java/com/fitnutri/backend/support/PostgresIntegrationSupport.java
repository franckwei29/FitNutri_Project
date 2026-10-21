package com.fitnutri.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** No H2 or application-default fallback. The test JVM owns its entire UUID database. */
@SpringBootTest
@ActiveProfiles("integration-test")
public abstract class PostgresIntegrationSupport {
    private static final TestDatabase DATABASE=TestDatabase.create();
    static { Runtime.getRuntime().addShutdownHook(new Thread(DATABASE::close,"fitnutri-owned-test-db-cleanup")); }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",DATABASE::url);
        properties.add("spring.datasource.username",DATABASE::username);
        properties.add("spring.datasource.password",DATABASE::password);
    }
}
