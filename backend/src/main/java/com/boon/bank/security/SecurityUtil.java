package com.boon.bank.security;

import com.boon.bank.security.userdetails.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? Optional.empty() : Optional.ofNullable(auth.getName());
    }

    public static Optional<AppUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        return principal instanceof AppUserDetails details ? Optional.of(details) : Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserDetails().map(AppUserDetails::getUserId);
    }

    public static Optional<UUID> getCurrentCustomerId() {
        return getCurrentUserDetails().map(AppUserDetails::getCustomerId);
    }

    public static Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? Collections.emptyList() : auth.getAuthorities();
    }

    public static boolean hasRole(String role) {
        return getCurrentAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_" + role));
    }

    public static boolean isStaff() {
        return hasRole("ADMIN") || hasRole("TELLER") || hasRole("OPS");
    }
}
