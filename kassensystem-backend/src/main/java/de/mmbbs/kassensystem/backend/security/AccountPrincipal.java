package de.mmbbs.kassensystem.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public final class AccountPrincipal extends User {
    private final long id;
    private final String displayName;
    private final long version;
    private final boolean passwordChangeRequired;

    public AccountPrincipal(long id, String login, String displayName, String hash, boolean enabled,
                            long version, boolean passwordChangeRequired,
                            Collection<? extends GrantedAuthority> authorities) {
        super(login, hash, enabled, true, true, true, authorities);
        this.id = id;
        this.displayName = displayName;
        this.version = version;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public long id() { return id; }
    public String displayName() { return displayName; }
    public long version() { return version; }
    public boolean passwordChangeRequired() { return passwordChangeRequired; }
}
