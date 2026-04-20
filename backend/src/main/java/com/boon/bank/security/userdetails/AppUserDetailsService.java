package com.boon.bank.security.userdetails;

import com.boon.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // The lazy-init hazard described in P02 is closed at the query layer:
    // UserRepository#findByUsername carries
    //   @EntityGraph(attributePaths = {"roles", "customer", "customer.customerType"})
    // which fetches every association AppUserDetails exposes so callers can traverse the
    // graph after this transaction closes without hitting LazyInitializationException.
    // Remove this comment together with the @EntityGraph if both are ever dropped.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
