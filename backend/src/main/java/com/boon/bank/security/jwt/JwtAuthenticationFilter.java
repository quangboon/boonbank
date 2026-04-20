package com.boon.bank.security.jwt;

import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;
    private final AppUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length());
        try {
            Claims claims = tokenProvider.parse(token);

            if (isRevoked(claims, token)) {
                // Leave SecurityContext empty; downstream Spring Security filters
                // enforce .anyRequest().authenticated() and return 401/403. Do NOT
                // sendError here — that short-circuits the chain and breaks response.
                log.debug("Token rejected: blacklisted");
            } else {
                UserDetails loaded = userDetailsService.loadUserByUsername(claims.getSubject());
                if (!(loaded instanceof AppUserDetails userDetails)) {
                    log.warn("UserDetailsService returned unexpected type [{}]",
                            loaded.getClass().getName());
                    chain.doFilter(request, response);
                    return;
                }
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    log.debug("JWT subject account is disabled or locked");
                } else {
                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException e) {
            // Message may reflect attacker-controlled JWT content; log only the type.
            log.debug("Invalid JWT [{}]", e.getClass().getSimpleName());
        } catch (UsernameNotFoundException e) {
            log.debug("JWT subject not in user store");
        } catch (DataAccessException e) {
            // Redis (blacklist) or Postgres (user lookup) unreachable: fail closed — do NOT
            // proceed as anonymous through a security filter that cannot enforce its rules.
            log.error("Auth backing store unavailable: {}", e.getClass().getSimpleName());
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRevoked(Claims claims, String rawToken) {
        String jti = claims.getId();
        boolean strict = jwtProperties.getRotation().isBlacklistCheckEnabled();

        if (jti != null) {
            return blacklistService.isBlacklisted(jti);
        }
        // jti missing — grandfathered pre-P07 token.
        if (strict) {
            return true;
        }
        return blacklistService.isBlacklisted(rawToken);
    }
}
