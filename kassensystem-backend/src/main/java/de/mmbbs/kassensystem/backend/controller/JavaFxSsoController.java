package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.config.JavaFxSsoTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
public class JavaFxSsoController {
    private final JavaFxSsoTicketService ticketService;
    private final UserDetailsService userDetailsService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public JavaFxSsoController(JavaFxSsoTicketService ticketService, UserDetailsService userDetailsService) {
        this.ticketService = ticketService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/auth/javafx-login")
    public String loginMitJavaFxTicket(@RequestParam("ticket") String ticket,
                                       @RequestParam(name = "theme", defaultValue = "light") String theme,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        JavaFxSsoTicketService.Ticket validiertesTicket = ticketService.validiere(ticket)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "SSO-Ticket ist ungültig."));

        if (!"ADMIN".equals(validiertesTicket.rolle()) && !"LAGERIST".equals(validiertesTicket.rolle())) {
            throw new ResponseStatusException(FORBIDDEN, "Rolle darf die Verwaltung nicht nutzen.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(validiertesTicket.benutzername());
        if (userDetails.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_" + validiertesTicket.rolle()))) {
            throw new ResponseStatusException(FORBIDDEN, "Rolle passt nicht zum Benutzer.");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        String zielTheme = "dark".equalsIgnoreCase(theme) ? "dark" : "light";
        return "redirect:/admin/index.html?theme=" + zielTheme;
    }
}
