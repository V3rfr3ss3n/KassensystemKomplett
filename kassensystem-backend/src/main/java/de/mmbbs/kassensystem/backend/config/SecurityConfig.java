package de.mmbbs.kassensystem.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * <p>Der Adminbereich und die Produkt-API sind nur fuer Nutzer mit Rolle
 * ADMIN erreichbar. Die festen Nutzer sind fuer die Projektphase bewusst klein
 * gehalten und koennen spaeter durch Datenbanknutzer ersetzt werden.</p>
 */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**", "/api/**").hasRole("ADMIN")
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
        return new InMemoryUserDetailsManager(admin, kassierer);
    }
}
