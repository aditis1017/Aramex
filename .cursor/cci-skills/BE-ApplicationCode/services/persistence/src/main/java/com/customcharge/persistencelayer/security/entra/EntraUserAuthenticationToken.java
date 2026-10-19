package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Post-validation authentication token: principal is the persisted {@link User}; Entra claim snapshot
 * is available via {@link #getEntraIdentity()}.
 */
public class EntraUserAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;
    private final JwtUserIdentity entraIdentity;

    public EntraUserAuthenticationToken(
            User user,
            JwtUserIdentity entraIdentity,
            Collection<? extends GrantedAuthority> authorities) {
        super(new ArrayList<>(authorities));
        this.user = user;
        this.entraIdentity = entraIdentity;
        setAuthenticated(true);
    }

    public User getUser() {
        return user;
    }

    public JwtUserIdentity getEntraIdentity() {
        return entraIdentity;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
