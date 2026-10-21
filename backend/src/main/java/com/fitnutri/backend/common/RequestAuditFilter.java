package com.fitnutri.backend.common;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
/** One outer servlet filter covers security rejections too; no request/response body capture. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE+10)
public class RequestAuditFilter extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(RequestAuditFilter.class);
    private static final Set<String> METHODS=Set.of("GET","POST","PUT","PATCH","DELETE","HEAD","OPTIONS");
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith("/api/");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String id=RequestCorrelation.id(request);response.setHeader("X-Request-Id",id);long started=System.nanoTime();boolean complete=false;
        try{chain.doFilter(request,response);complete=true;}
        finally{Object matched=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);String route=matched instanceof String value&&value.matches("/[A-Za-z0-9_{}./*:-]{0,200}")?value:"API";String method=METHODS.contains(request.getMethod())?request.getMethod():"OTHER";LOG.info("api_request requestId={} operation={} {} status={} durationMs={}",id,method,route,complete?response.getStatus():500,Math.max(0,(System.nanoTime()-started)/1_000_000));}
    }
}
