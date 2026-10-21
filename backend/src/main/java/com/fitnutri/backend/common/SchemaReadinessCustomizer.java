package com.fitnutri.backend.common;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;
/** Flyway (when explicitly enabled) precedes this read-only history gate, then Hibernate validation. */
@Component
@DependsOnDatabaseInitialization
public final class SchemaReadinessCustomizer implements HibernatePropertiesCustomizer {
    private final DataSource source;
    public SchemaReadinessCustomizer(DataSource source){this.source=source;}
    @Override public void customize(Map<String,Object> properties){new SchemaReadinessGate(source);}
}
