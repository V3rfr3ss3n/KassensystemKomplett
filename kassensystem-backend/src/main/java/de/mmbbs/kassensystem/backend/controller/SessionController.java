package de.mmbbs.kassensystem.backend.controller;

import org.springframework.security.core.Authentication;
import de.mmbbs.kassensystem.backend.security.AccountPrincipal;
import de.mmbbs.kassensystem.backend.security.AccountService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final AccountService accounts;

    public SessionController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    public Map<String, Object> aktuelleSession(Authentication authentication, CsrfToken csrfToken) {
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        Map<String, Object> view = new LinkedHashMap<>(accounts.publicView(accounts.findByLogin(principal.getUsername())));
        Map<String, Boolean> permissions = new LinkedHashMap<>((Map<String, Boolean>) view.get("permissions"));
        permissions.put("manageProducts", permissions.get("products.manage"));
        permissions.put("bookStock", permissions.get("stock.book"));
        view.put("permissions", permissions);
        view.put("csrfToken", csrfToken.getToken());
        return view;
    }
}
