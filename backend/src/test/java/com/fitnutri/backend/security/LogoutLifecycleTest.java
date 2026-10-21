package com.fitnutri.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class LogoutLifecycleTest {
    @Test void anonymousLogoutHandlerDoesNotCreateASessionAndClearsCookie() {
        var config=new SecurityConfig();var request=new MockHttpServletRequest();var response=new MockHttpServletResponse();
        assertNull(request.getSession(false));
        config.apiLogoutHandler(config.securityContextRepository(),config.csrfTokenRepository()).logout(request,response,null);
        assertNull(request.getSession(false));assertNotNull(response.getCookie("JSESSIONID"));assertEquals(0,response.getCookie("JSESSIONID").getMaxAge());
    }
}
