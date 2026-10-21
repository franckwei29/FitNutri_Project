package com.fitnutri.backend.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.admin-bootstrap.enabled",havingValue="true")
public class AdminBootstrapRunner implements CommandLineRunner {
    private final AuthService auth;
    private final String email,password,name;
    public AdminBootstrapRunner(AuthService auth,@Value("${app.admin-bootstrap.email:}") String email,
            @Value("${app.admin-bootstrap.password:}") String password,@Value("${app.admin-bootstrap.display-name:}") String name) {
        this.auth=auth;this.email=email;this.password=password;this.name=name;
    }
    @Override public void run(String... args) {auth.bootstrapAdmin(new RegisterRequest(email,password,name));}
}
