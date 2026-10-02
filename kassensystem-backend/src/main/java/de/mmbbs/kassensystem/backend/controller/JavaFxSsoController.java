package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.config.JavaFxSsoTicketService;
import de.mmbbs.kassensystem.backend.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
public class JavaFxSsoController {
    private final JavaFxSsoTicketService tickets;
    private final UserDetailsService users;
    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public JavaFxSsoController(JavaFxSsoTicketService tickets, UserDetailsService users) {
        this.tickets = tickets;
        this.users = users;
    }

    @PostMapping("/api/auth/browser-ticket")
    @ResponseBody
    public Map<String, String> ticket(@AuthenticationPrincipal AccountPrincipal principal) {
        if (principal == null || principal.passwordChangeRequired()) throw new ResponseStatusException(FORBIDDEN);
        return Map.of("ticket", tickets.issue(principal));
    }

    @GetMapping("/auth/javafx-login")
    public String login(@RequestParam("ticket") String ticket,
                        @RequestParam(name = "theme", defaultValue = "light") String theme,
                        HttpServletRequest request, HttpServletResponse response) {
        var consumed = tickets.consume(ticket)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Anmeldeticket ist ungültig."));
        AccountPrincipal user = (AccountPrincipal) users.loadUserByUsername(consumed.username());
        if (!user.isEnabled() || user.passwordChangeRequired() || user.version() != consumed.version()
                || user.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("PERM_products.manage")
                || a.getAuthority().equals("PERM_stock.book") || a.getAuthority().equals("PERM_users.manage")))
            throw new ResponseStatusException(FORBIDDEN, "Zugang zur Verwaltung nicht erlaubt.");

        if (request.getSession(false) != null) request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
        return "redirect:/admin/index.html?theme=" + ("dark".equalsIgnoreCase(theme) ? "dark" : "light");
    }
}
