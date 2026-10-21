package com.fitnutri.backend.common;
import com.fitnutri.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
class UpstreamCorsTest {
 @Test void configuredOriginsExposeReplayWithoutAllowingOtherOrigins(){var config=new SecurityConfig().corsConfigurationSource("http://localhost:5173,http://localhost:15197").getCorsConfiguration(new MockHttpServletRequest("POST","/api/v2/food/batches"));assertNotNull(config);assertEquals("http://localhost:15197",config.checkOrigin("http://localhost:15197"));assertNull(config.checkOrigin("https://untrusted.example"));assertTrue(config.getExposedHeaders().contains("Idempotent-Replay"));assertTrue(config.getAllowCredentials());assertThrows(IllegalStateException.class,()->new SecurityConfig().corsConfigurationSource("*"));}
}
