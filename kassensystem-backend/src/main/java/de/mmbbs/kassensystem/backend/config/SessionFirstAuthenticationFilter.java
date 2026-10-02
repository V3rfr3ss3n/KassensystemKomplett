package de.mmbbs.kassensystem.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Eine Browser-Sitzung hat Vorrang vor im Browser gespeicherten Basic-Zugangsdaten.
 * Der JavaFX-API-Client hat keine Browser-Sitzung und authentifiziert sich weiter per Basic.
 */
final class SessionFirstAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest ohneBasicHeader = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return "Authorization".equalsIgnoreCase(name) ? null : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return "Authorization".equalsIgnoreCase(name)
                        ? Collections.emptyEnumeration() : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
                        .filter(name -> !"Authorization".equalsIgnoreCase(name)).toList());
            }
        };
        chain.doFilter(ohneBasicHeader, response);
    }
}
