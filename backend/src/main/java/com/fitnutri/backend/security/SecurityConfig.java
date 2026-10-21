package com.fitnutri.backend.security;

import com.fitnutri.backend.common.CurrentUserService;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {return c.getAuthenticationManager();}
    @Bean public SecurityContextRepository securityContextRepository() {
        var repository=new HttpSessionSecurityContextRepository();repository.setDisableUrlRewriting(true);return repository;
    }
    @Bean public CsrfTokenRepository csrfTokenRepository() {return new HttpSessionCsrfTokenRepository();}
    @Bean public CsrfTokenRequestHandler csrfTokenRequestHandler() {return new XorCsrfTokenRequestAttributeHandler();}
    @Bean public SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository tokens,CsrfTokenRequestHandler handler) {
        var csrf=new CsrfAuthenticationStrategy(tokens);csrf.setRequestHandler(handler);
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),csrf));
    }
    @Bean public LogoutHandler apiLogoutHandler(SecurityContextRepository contexts,CsrfTokenRepository tokens) {
        var security=new SecurityContextLogoutHandler();security.setSecurityContextRepository(contexts);
        return new CompositeLogoutHandler(new CsrfLogoutHandler(tokens),security,new CookieClearingLogoutHandler("JSESSIONID"));
    }
    @Bean public SecurityFilterChain filterChain(HttpSecurity http,SecurityContextRepository contexts,
            CsrfTokenRepository tokens,CsrfTokenRequestHandler handler,CurrentUserService users,JsonSecurityErrorWriter errors,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.csrf(c->c.csrfTokenRepository(tokens).csrfTokenRequestHandler(handler))
            .cors(c->c.configurationSource(cors))
            .securityContext(c->c.securityContextRepository(contexts).requireExplicitSave(true))
            .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(c->c
                .requestMatchers("/api/auth/register","/api/auth/login","/api/auth/logout","/api/auth/csrf","/actuator/health","/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v2/**","/api/profile/**","/api/records/**","/api/summary/**","/api/food/**","/api/food-reference/**","/api/submissions/**","/api/speech/**","/api/qa/**").hasRole("USER")
                .anyRequest().authenticated())
            .exceptionHandling(c->c.authenticationEntryPoint((req,res,e)->errors.write(req,res,401,"UNAUTHENTICATED","Sign in to continue."))
                .accessDeniedHandler((req,res,e)->errors.write(req,res,403,e instanceof CsrfException?"CSRF_INVALID":"FORBIDDEN",e instanceof CsrfException?"Refresh the security token and try again.":"This operation is not permitted.")))
            .addFilterAfter(new ActiveAccountFilter(users,errors),AnonymousAuthenticationFilter.class)
            .httpBasic(c->c.disable()).formLogin(c->c.disable()).logout(c->c.disable());
        return http.build();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${app.allowed-origins:http://localhost:5173}") String origins) {
        var allowed=Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();
        if(allowed.isEmpty() || allowed.contains("*")) throw new IllegalStateException("Explicit allowed origins are required.");
        var config=new CorsConfiguration();config.setAllowedOrigins(allowed);config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));config.setAllowedHeaders(List.of("*"));config.setAllowCredentials(true);config.setExposedHeaders(List.of("Idempotent-Replay"));
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
    }
}
