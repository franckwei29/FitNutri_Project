package com.fitnutri.backend.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapRunnerTest {
    private final AuthService service=mock(AuthService.class);
    private final ApplicationContextRunner contexts=new ApplicationContextRunner().withBean(AuthService.class,()->service).withUserConfiguration(AdminBootstrapRunner.class);
    @Test void bootstrapIsAbsentByDefault() {
        contexts.run(context->{assertEquals(0,context.getBeansOfType(AdminBootstrapRunner.class).size());verifyNoInteractions(service);});
    }
    @Test void explicitFlagAndConfigurationRouteToControlledServiceOnly() {
        contexts.withPropertyValues("app.admin-bootstrap.enabled=true","app.admin-bootstrap.email=  ADMIN@EXAMPLE.TEST  ","app.admin-bootstrap.password=Synthetic123","app.admin-bootstrap.display-name=Synthetic admin")
            .run(context->{context.getBean(AdminBootstrapRunner.class).run();verify(service).bootstrapAdmin(new RegisterRequest("admin@example.test","Synthetic123","Synthetic admin"));});
    }
}
