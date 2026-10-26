package com.ferreteria.ventas.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Seguridad a nivel de METODO: se usa desde {@code @PreAuthorize("@roleAccess.canRead(authentication)")} en
 * {@code AbstractBaseController}. Lee los roles de {@link AccessProperties}, de modo que nada queda escrito en el codigo.
 */
@Component("roleAccess")
public class RoleAccess {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AccessProperties properties;

    public RoleAccess(AccessProperties properties) {
        this.properties = properties;
    }

    public boolean canRead(Authentication authentication) {
        return hasAny(authentication, properties.read());
    }

    public boolean canWrite(Authentication authentication) {
        return hasAny(authentication, properties.write());
    }

    public boolean canDelete(Authentication authentication) {
        return hasAny(authentication, properties.delete());
    }

    static boolean hasAny(Authentication authentication, List<String> allowedRoles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return allowedRoles.stream()
                .map(r -> r.startsWith(ROLE_PREFIX) ? r : ROLE_PREFIX + r)
                .anyMatch(granted::contains);
    }
}
