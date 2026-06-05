package de.mmbbs.kassensystem.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Testnutzer und Zugriffsregeln fuer den Spring-Adminbereich.
 *
 * <p>Admins duerfen Produkte pflegen und Warenzugaenge buchen. Lageristen
 * duerfen die Verwaltung lesen und Warenzugaenge erfassen, aber keine Produkte
 * anlegen, aendern oder loeschen.</p>
 */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/javafx-login").permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "LAGERIST")
                        .requestMatchers(HttpMethod.GET, "/api/session").hasAnyRole("ADMIN", "LAGERIST")
                        .requestMatchers(HttpMethod.GET, "/api/produkte/**").hasAnyRole("ADMIN", "LAGERIST")
                        .requestMatchers(HttpMethod.POST, "/api/produkte/*/warenzugang").hasAnyRole("ADMIN", "LAGERIST")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}1234")
                .roles("ADMIN")
                .build();
        UserDetails kassierer = User.withUsername("kassierer")
                .password("{noop}1234")
                .roles("KASSIERER")
                .build();
        UserDetails lagerist = User.withUsername("lagerist")
                .password("{noop}1234")
                .roles("LAGERIST")
                .build();
        return new InMemoryUserDetailsManager(admin, kassierer, lagerist);
    }
}
