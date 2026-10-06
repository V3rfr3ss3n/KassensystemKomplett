package de.mmbbs.kassensystem.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Prüft gespeicherte Browser-Sitzungen nach Kontoänderungen erneut. */
public final class AccountVersionFilter extends OncePerRequestFilter {
    private final AccountService accounts;

    public AccountVersionFilter(AccountService accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/auth/javafx-login".equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            AccountService.Account account = accounts.findByLogin(principal.getUsername());
            if (account == null || !account.active() || account.version() != principal.version()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bitte erneut anmelden.");
                return;
            }
            if (principal.passwordChangeRequired() && path.startsWith("/api/")
                    && !path.equals("/api/session") && !path.equals("/api/account/password")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Passwortwechsel erforderlich.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
