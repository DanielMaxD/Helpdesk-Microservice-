package com.helpdesk.ticketservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Small helper shared by controllers to read the authenticated user's id and
 * role out of the Authentication populated by JwtAuthenticationFilter.
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    public static UUID getUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public static String getRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElseThrow(() -> new AccessDeniedException("No role present on token"));
    }
}
