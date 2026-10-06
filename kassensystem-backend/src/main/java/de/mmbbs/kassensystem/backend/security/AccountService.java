package de.mmbbs.kassensystem.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Order(1)
public class AccountService implements UserDetailsService, ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final List<String> PERMISSIONS = List.of(
            "products.read", "products.manage", "stock.book", "sales.create", "receipts.read", "users.manage");
    public static final List<String> ROLES = List.of("ADMIN", "KASSIERER", "LAGERIST");
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", Set.copyOf(PERMISSIONS),
            "KASSIERER", Set.of("products.read", "sales.create", "receipts.read"),
            "LAGERIST", Set.of("products.read", "stock.book"));

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final boolean demo;
    private final String initialPassword;

    public AccountService(JdbcTemplate jdbc, PasswordEncoder encoder,
                          @Value("${kassensystem.auth.demo:false}") boolean demo,
                          @Value("${kassensystem.auth.initial-admin-password:}") String initialPassword) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.demo = demo;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM benutzer", Integer.class) != 0) return;
        if (demo) {
            seed("admin", "Admin", "ADMIN");
            seed("kassierer", "Kassierer", "KASSIERER");
            seed("lagerist", "Lagerist", "LAGERIST");
            return;
        }
        String startPasswort = initialPassword == null || initialPassword.isBlank()
                ? generiereStartpasswort() : initialPassword;
        validatePassword(startPasswort);
        insert("admin", "Admin", startPasswort, true, List.of("ADMIN"));
        if (initialPassword == null || initialPassword.isBlank()) {
            LOG.warn("Ersteinrichtung: Ein einmaliges Admin-Startpasswort wurde erzeugt. " +
                    "Jetzt als 'admin' anmelden und das Passwort ändern: {}", startPasswort);
        }
    }

    private String generiereStartpasswort() {
        byte[] zufall = new byte[24];
        SECURE_RANDOM.nextBytes(zufall);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(zufall);
    }

    private void seed(String login, String displayName, String role) {
        insert(login, displayName, "1234", false, List.of(role));
    }

    private void insert(String login, String displayName, String password, boolean mustChange, List<String> roles) {
        jdbc.update("INSERT INTO benutzer(login, anzeigename, passwort_hash, passwortwechsel_noetig) VALUES(?,?,?,?)",
                login, displayName, encoder.encode(password), mustChange ? 1 : 0);
        long id = jdbc.queryForObject("SELECT id FROM benutzer WHERE login=?", Long.class, login);
        for (String role : roles) jdbc.update("INSERT INTO benutzer_rollen(benutzer_id, rolle) VALUES(?,?)", id, role);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = findByLogin(username);
        if (account == null) throw new UsernameNotFoundException("Unbekannter Benutzer");
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        for (String role : account.roles()) authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        Map<String, Boolean> permissions = effectivePermissions(account);
        for (String permission : permissions.keySet()) {
            if (permissions.get(permission))
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission));
        }
        return new AccountPrincipal(account.id(), account.login(), account.displayName(), account.hash(),
                account.active(), account.version(), account.mustChange(), authorities);
    }

    public Account findByLogin(String login) {
        if (login == null) return null;
        var rows = jdbc.query("SELECT * FROM benutzer WHERE login=?", (rs, n) -> account(rs.getLong("id"),
                rs.getString("login"), rs.getString("anzeigename"), rs.getString("passwort_hash"),
                rs.getInt("aktiv") != 0, rs.getInt("passwortwechsel_noetig") != 0, rs.getLong("version")), normalize(login));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public Account findById(long id) {
        var rows = jdbc.query("SELECT * FROM benutzer WHERE id=?", (rs, n) -> account(rs.getLong("id"),
                rs.getString("login"), rs.getString("anzeigename"), rs.getString("passwort_hash"),
                rs.getInt("aktiv") != 0, rs.getInt("passwortwechsel_noetig") != 0, rs.getLong("version")), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Account account(long id, String login, String display, String hash, boolean active, boolean mustChange, long version) {
        List<String> roles = jdbc.queryForList("SELECT rolle FROM benutzer_rollen WHERE benutzer_id=? ORDER BY rolle", String.class, id);
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        jdbc.query("SELECT recht, erlaubt FROM benutzer_rechte WHERE benutzer_id=?",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        overrides.put(rs.getString(1), rs.getInt(2) != 0), id);
        return new Account(id, login, display, hash, active, mustChange, version, roles, overrides);
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT id FROM benutzer ORDER BY login").stream()
                .map(row -> publicView(findById(((Number) row.get("id")).longValue()))).toList();
    }

    public Map<String, Boolean> effectivePermissions(Account account) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String permission : PERMISSIONS) {
            boolean byRole = account.roles().stream().anyMatch(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
            result.put(permission, account.overrides().getOrDefault(permission, byRole));
        }
        return result;
    }

    public Map<String, Set<String>> roleDefaults() { return ROLE_PERMISSIONS; }

    public Map<String, Object> publicView(Account account) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", account.id());
        view.put("username", account.login());
        view.put("displayName", account.displayName());
        view.put("active", account.active());
        view.put("mustChangePassword", account.mustChange());
        view.put("roles", account.roles());
        view.put("overrides", account.overrides());
        view.put("permissions", effectivePermissions(account));
        return view;
    }

    @Transactional
    public synchronized Map<String, Object> create(String actor, String login, String displayName, String password,
                                                     Boolean active, List<String> roles, Map<String, Boolean> overrides) {
        String normalized = validateLogin(login);
        validatePassword(password);
        validateRolesAndOverrides(roles, overrides);
        if (findByLogin(normalized) != null) throw new IllegalArgumentException("Login ist bereits vergeben.");
        insert(normalized, validateDisplay(displayName), password, true, roles);
        Account created = findByLogin(normalized);
        if (Boolean.FALSE.equals(active)) jdbc.update("UPDATE benutzer SET aktiv=0 WHERE id=?", created.id());
        saveOverrides(created.id(), overrides);
        audit(actor, normalized, "angelegt");
        return publicView(findById(created.id()));
    }

    @Transactional
    public synchronized Map<String, Object> update(String actor, long id, String displayName, Boolean active,
                                                     List<String> roles, Map<String, Boolean> overrides) {
        Account current = require(id);
        String nextDisplay = displayName == null ? current.displayName() : validateDisplay(displayName);
        boolean nextActive = active == null ? current.active() : active;
        List<String> nextRoles = roles == null ? current.roles() : roles;
        Map<String, Boolean> nextOverrides = overrides == null ? current.overrides() : overrides;
        validateRolesAndOverrides(nextRoles, nextOverrides);
        if (current.active() && effectivePermissions(current).get("users.manage") &&
                (!nextActive || !effectivePermissions(new Account(id, current.login(), nextDisplay, current.hash(),
                        nextActive, current.mustChange(), current.version(), nextRoles, nextOverrides)).get("users.manage"))) {
            long otherAdmins = listAccounts().stream().filter(a -> a.id() != id && a.active()
                    && effectivePermissions(a).get("users.manage")).count();
            if (otherAdmins == 0) throw new IllegalArgumentException("Der letzte Benutzer mit Benutzerverwaltung darf nicht gesperrt werden.");
        }
        jdbc.update("UPDATE benutzer SET anzeigename=?, aktiv=?, version=version+1 WHERE id=?", nextDisplay, nextActive ? 1 : 0, id);
        jdbc.update("DELETE FROM benutzer_rollen WHERE benutzer_id=?", id);
        for (String role : nextRoles) jdbc.update("INSERT INTO benutzer_rollen(benutzer_id, rolle) VALUES(?,?)", id, role);
        saveOverrides(id, nextOverrides);
        audit(actor, current.login(), "geändert");
        return publicView(require(id));
    }

    @Transactional
    public void resetPassword(String actor, long id, String password) {
        validatePassword(password);
        Account target = require(id);
        jdbc.update("UPDATE benutzer SET passwort_hash=?, passwortwechsel_noetig=1, version=version+1 WHERE id=?",
                encoder.encode(password), id);
        audit(actor, target.login(), "Passwort zurückgesetzt");
    }

    @Transactional
    public void changePassword(String login, String oldPassword, String newPassword) {
        Account account = findByLogin(login);
        if (account == null || !account.active() || !encoder.matches(oldPassword, account.hash()))
            throw new IllegalArgumentException("Das bisherige Passwort ist falsch.");
        validatePassword(newPassword);
        if (encoder.matches(newPassword, account.hash()))
            throw new IllegalArgumentException("Das neue Passwort muss sich unterscheiden.");
        jdbc.update("UPDATE benutzer SET passwort_hash=?, passwortwechsel_noetig=0, version=version+1 WHERE id=?",
                encoder.encode(newPassword), account.id());
        audit(login, login, "Passwort geändert");
    }

    private List<Account> listAccounts() {
        return jdbc.queryForList("SELECT id FROM benutzer").stream()
                .map(row -> findById(((Number) row.get("id")).longValue())).toList();
    }

    private Account require(long id) {
        Account account = findById(id);
        if (account == null) throw new IllegalArgumentException("Benutzer nicht gefunden.");
        return account;
    }

    private void saveOverrides(long id, Map<String, Boolean> overrides) {
        jdbc.update("DELETE FROM benutzer_rechte WHERE benutzer_id=?", id);
        for (var entry : overrides.entrySet())
            jdbc.update("INSERT INTO benutzer_rechte(benutzer_id, recht, erlaubt) VALUES(?,?,?)",
                    id, entry.getKey(), entry.getValue() ? 1 : 0);
    }

    private void audit(String actor, String target, String action) {
        jdbc.update("INSERT INTO benutzer_audit(admin_login, ziel_login, aktion) VALUES(?,?,?)", actor, target, action);
    }

    private String validateLogin(String login) {
        String normalized = normalize(login);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]{2,39}"))
            throw new IllegalArgumentException("Login: 3 bis 40 Zeichen, Kleinbuchstaben, Zahlen, Punkt, Minus oder Unterstrich.");
        return normalized;
    }

    private String validateDisplay(String display) {
        if (display == null || display.isBlank() || display.trim().length() > 80)
            throw new IllegalArgumentException("Anzeigename muss 1 bis 80 Zeichen enthalten.");
        return display.trim();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128)
            throw new IllegalArgumentException("Passwort muss 10 bis 128 Zeichen enthalten. Für die erste Einrichtung KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD setzen.");
    }

    private void validateRolesAndOverrides(List<String> roles, Map<String, Boolean> overrides) {
        if (roles == null || roles.isEmpty() || !ROLES.containsAll(roles) || new LinkedHashSet<>(roles).size() != roles.size())
            throw new IllegalArgumentException("Mindestens eine gültige Rolle wählen.");
        if (overrides == null || !PERMISSIONS.containsAll(overrides.keySet()) || overrides.containsValue(null))
            throw new IllegalArgumentException("Ungültige Einzelrechte.");
    }

    private String normalize(String login) { return login == null ? "" : login.trim().toLowerCase(Locale.ROOT); }

    public record Account(long id, String login, String displayName, String hash, boolean active,
                          boolean mustChange, long version, List<String> roles, Map<String, Boolean> overrides) {}
}
