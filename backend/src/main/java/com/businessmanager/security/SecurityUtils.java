package com.businessmanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (authority.getAuthority().equals("ROLE_" + role)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Accès aux données financières (devis, factures, statistiques). */
    public static boolean canSeeFinance(Authentication authentication) {
        return hasAnyRole(authentication, "ADMIN", "ACCOUNTING");
    }

    /** Accès aux projets et tâches. */
    public static boolean canSeeProjects(Authentication authentication) {
        return hasAnyRole(authentication, "ADMIN", "EMPLOYEE");
    }
}
