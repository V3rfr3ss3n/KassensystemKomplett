package de.mmbbs.kassensystem.backend.config;

import de.mmbbs.kassensystem.backend.security.AccountService;
import de.mmbbs.kassensystem.backend.security.AccountVersionFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AccountService accounts) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        return http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers(this::isDesktopApiRequest))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/javafx-login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bilder/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority("PERM_products.manage", "PERM_stock.book", "PERM_users.manage")
                        .requestMatchers(HttpMethod.GET, "/api/session").authenticated()
                        .requestMatchers("/api/account/password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/browser-ticket")
                            .hasAnyAuthority("PERM_products.manage", "PERM_stock.book", "PERM_users.manage")
                        .requestMatchers("/api/admin/**").hasAuthority("PERM_users.manage")
                        .requestMatchers(HttpMethod.GET, "/api/produkte/**").hasAuthority("PERM_products.read")
                        .requestMatchers(HttpMethod.POST, "/api/produkte/*/warenzugang").hasAuthority("PERM_stock.book")
                        .requestMatchers("/api/produkte/**", "/api/bilder/**").hasAuthority("PERM_products.manage")
                        .requestMatchers("/api/bons/**").hasAuthority("PERM_receipts.read")
                        .requestMatchers("/api/kasse/**").hasAuthority("PERM_sales.create")
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(new SessionFirstAuthenticationFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new AccountVersionFilter(accounts), BasicAuthenticationFilter.class)
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .build();
    }

    private boolean isDesktopApiRequest(HttpServletRequest request) {
        return request.getRequestURI().contains("/api/")
                && request.getSession(false) == null
                && "JavaFX".equals(request.getHeader("X-Kassensystem-Client"))
                && request.getHeader("Authorization") != null;
    }
}
