package com.fitnutri.backend.security;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.common.CurrentUserService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Installed only inside the security chain, not also registered as a servlet Filter bean. */
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final CurrentUserService users;
    private final JsonSecurityErrorWriter errors;
    public ActiveAccountFilter(CurrentUserService users,JsonSecurityErrorWriter errors) {this.users=users;this.errors=errors;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                var user=users.requireActive(auth);
                // Database role is authoritative even if a session carries stale authorities.
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(),auth.getCredentials(),List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()))));
            } catch(org.springframework.transaction.CannotCreateTransactionException|org.springframework.dao.DataAccessResourceFailureException|org.springframework.dao.TransientDataAccessResourceException e) {
                // No authority is granted while the account cannot be checked; a temporary outage is not a logout.
                errors.write(request,response,503,"DATABASE_UNAVAILABLE","The database is temporarily unavailable. Check the outcome of any save before retrying.");return;
            } catch(BusinessException e) {
                SecurityContextHolder.clearContext();var session=request.getSession(false);if(session!=null) session.invalidate();
                errors.write(request,response,401,"UNAUTHENTICATED","Sign in to continue.");return;
            }
        }
        chain.doFilter(request,response);
    }
}
