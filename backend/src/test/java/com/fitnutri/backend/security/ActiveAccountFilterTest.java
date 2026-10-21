package com.fitnutri.backend.security;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ActiveAccountFilterTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void databaseOutageNeverBypassesAccountCheckOrDestroysSession()throws Exception{var users=mock(CurrentUserService.class);var filter=new ActiveAccountFilter(users,new JsonSecurityErrorWriter(new tools.jackson.databind.ObjectMapper()));var auth=UsernamePasswordAuthenticationToken.authenticated("synthetic@example.test",null,List.of(new SimpleGrantedAuthority("ROLE_USER")));SecurityContextHolder.getContext().setAuthentication(auth);var session=new MockHttpSession();session.setAttribute("syntheticRetention","keep");var request=new MockHttpServletRequest("GET","/api/v2/exercise/options");request.setSession(session);var response=new MockHttpServletResponse();var called=new AtomicBoolean();when(users.requireActive(auth)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private SQL sentinel"));filter.doFilter(request,response,(a,b)->called.set(true));assertEquals(503,response.getStatus());assertFalse(called.get());assertEquals("keep",session.getAttribute("syntheticRetention"));assertFalse(response.getContentAsString().contains("private"));assertTrue(response.getContentAsString().contains("DATABASE_UNAVAILABLE"));assertTrue(response.getContentAsString().contains("\"retryable\":true"));var user=new User();doReturn(user).when(users).requireActive(auth);var next=new MockHttpServletRequest("GET","/api/v2/exercise/options");next.setSession(session);filter.doFilter(next,new MockHttpServletResponse(),(a,b)->called.set(true));assertTrue(called.get());verify(users,times(2)).requireActive(auth);}
}
