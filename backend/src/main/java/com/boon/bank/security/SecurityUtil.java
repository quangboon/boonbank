package com.boon.bank.security;

import com.boon.bank.entity.AppUser;
import com.boon.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {
    private final UserRepository userRepo;
    private static final ThreadLocal<AppUser> cachedUser = new ThreadLocal<>();

    public AppUser currentUser() {
        var cached = cachedUser.get();
        if (cached != null) return cached;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        var user = userRepo.findByUsername(auth.getName()).orElse(null);
        if (user != null) cachedUser.set(user);
        return user;
    }

    public Long currentCustomerId() {
        var user = currentUser();
        return (user != null && user.getCustomer() != null) ? user.getCustomer().getId() : null;
    }

    public boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public static void clearCache() {
        cachedUser.remove();
    }
}
