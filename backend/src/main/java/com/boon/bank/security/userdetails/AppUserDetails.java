package com.boon.bank.security.userdetails;

import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public record AppUserDetails(User user) implements UserDetails {

    public UUID getUserId() {
        return user.getId();
    }

    public Customer getCustomer() {
        return user.getCustomer();
    }

    public UUID getCustomerId() {
        return user.getCustomer() == null ? null : user.getCustomer().getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.isAccountLocked()) {
            return false;
        }
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
