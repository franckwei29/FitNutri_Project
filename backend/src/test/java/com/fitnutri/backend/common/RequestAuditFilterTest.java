package com.fitnutri.backend.common;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.HandlerMapping;
import static org.junit.jupiter.api.Assertions.*;
class RequestAuditFilterTest {
    final Logger logger=(Logger)LoggerFactory.getLogger(RequestAuditFilter.class);final ListAppender<ILoggingEvent> appender=new ListAppender<>();Level previous;
    @BeforeEach void capture(){previous=logger.getLevel();logger.setLevel(Level.INFO);appender.start();logger.addAppender(appender);}
    @AfterEach void cleanup(){logger.detachAppender(appender);logger.setLevel(previous);appender.stop();}
    @Test void logsOnlyServerIdMatchedOperationStatusAndElapsedNotRequestData() throws Exception {
        var request=new MockHttpServletRequest("POST","/api/records/food/987654321");request.setQueryString("health=synthetic-private-health");request.setContent("synthetic-private-password".getBytes());request.addHeader("Authorization","synthetic-private-token");request.addHeader("X-CSRF-TOKEN","synthetic-private-csrf");request.addHeader("Cookie","synthetic-private-cookie");request.addHeader("X-Request-Id","untrusted-client-id");var response=new MockHttpServletResponse();
        new RequestAuditFilter().doFilter(request,response,(req,res)->{req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/api/records/food/{id}");((jakarta.servlet.http.HttpServletResponse)res).setStatus(201);});
        String id=response.getHeader("X-Request-Id");assertNotNull(id);UUID.fromString(id);assertNotEquals("untrusted-client-id",id);assertEquals(1,appender.list.size());String line=appender.list.getFirst().getFormattedMessage();assertTrue(line.contains("requestId="+id));assertTrue(line.contains("operation=POST /api/records/food/{id}"));assertTrue(line.contains("status=201"));assertTrue(line.matches(".*durationMs=[0-9]+$"));assertFalse(line.contains("synthetic-private"));assertFalse(line.contains("987654321"));
    }
    @Test void responseErrorAndAuditShareTheSameServerCorrelationId() throws Exception {var request=new MockHttpServletRequest("GET","/api/profile");var response=new MockHttpServletResponse();final ApiError[] body={null};new RequestAuditFilter().doFilter(request,response,(req,res)->{response.setStatus(401);body[0]=ApiError.create(401,"UNAUTHENTICATED","Sign in.",request,List.of(),false);});assertEquals(body[0].requestId(),response.getHeader("X-Request-Id"));assertTrue(appender.list.getFirst().getFormattedMessage().contains("status=401"));assertTrue(appender.list.getFirst().getFormattedMessage().contains("requestId="+body[0].requestId()));}
    @Test void unhandledFailureLogs500WithoutExceptionOrBodyAndStillPropagates() {var request=new MockHttpServletRequest("POST","/api/private-secret-path");var response=new MockHttpServletResponse();assertThrows(ServletException.class,()->new RequestAuditFilter().doFilter(request,response,(req,res)->{throw new ServletException("synthetic-password-sql-provider-detail");}));String line=appender.list.getFirst().getFormattedMessage();assertTrue(line.contains("status=500"));assertTrue(line.contains("operation=POST API"));assertFalse(line.contains("secret"));assertFalse(line.contains("synthetic"));}
    @Test void untrustedPatternCannotInjectLogFieldsAndNonApiRequestsAreNotAudited() throws Exception {var request=new MockHttpServletRequest("GET","/api/unknown");request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"unsafe\npassword=private");new RequestAuditFilter().doFilter(request,new MockHttpServletResponse(),(req,res)->{});assertTrue(appender.list.getFirst().getFormattedMessage().contains("operation=GET API"));assertFalse(appender.list.getFirst().getFormattedMessage().contains("password"));new RequestAuditFilter().doFilter(new MockHttpServletRequest("GET","/favicon.svg"),new MockHttpServletResponse(),(req,res)->{});assertEquals(1,appender.list.size());}
}
