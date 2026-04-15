package com.boon.bank.security;

import com.boon.bank.config.RateLimitProperties;
import com.boon.bank.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        if (!props.isEnabled()) {
            chain.doFilter(req, resp);
            return;
        }

        try {
            String now = String.valueOf(System.currentTimeMillis());

            // tier 1: global
            long globalRemaining = callLua("rl:global", props.getGlobalWindow(), props.getGlobalLimit(), now);
            if (globalRemaining < 0) {
                reject(resp, props.getGlobalWindow(), 0);
                return;
            }

            // tier 2: per-IP
            String ip = extractIp(req);
            long ipRemaining = callLua("rl:ip:" + ip, props.getIpWindow(), props.getIpLimit(), now);
            if (ipRemaining < 0) {
                log.warn("Rate limit IP={}", ip);
                reject(resp, props.getIpWindow(), 0);
                return;
            }

            // tier 3: per-user (if authenticated)
            var auth = SecurityContextHolder.getContext().getAuthentication();
            long userRemaining = Long.MAX_VALUE;
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                int limit = isAdmin ? props.getAdminLimit() : props.getUserLimit();
                userRemaining = callLua("rl:user:" + username, props.getUserWindow(), limit, now);
                if (userRemaining < 0) {
                    log.warn("Rate limit user={}", username);
                    reject(resp, props.getUserWindow(), 0);
                    return;
                }
            }

            long remaining = Math.min(globalRemaining, Math.min(ipRemaining, userRemaining));
            resp.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        } catch (Exception e) {
            // fail-open: nếu Redis down thì cho qua
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
        }

        chain.doFilter(req, resp);
    }

    private long callLua(String key, int windowSec, int limit, String now) {
        return redisTemplate.execute(rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(windowSec),
                String.valueOf(limit),
                now);
    }

    private void reject(HttpServletResponse resp, int retryAfter, int remaining) throws IOException {
        resp.setStatus(429);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setHeader("Retry-After", String.valueOf(retryAfter));
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        objectMapper.writeValue(resp.getWriter(), Map.of(
                "success", false,
                "code", ErrorCode.RATE_LIMITED,
                "message", "Too many requests",
                "timestamp", Instant.now().toString()
        ));
    }

    private String extractIp(HttpServletRequest req) {
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        // skip rate limit cho health check
        return req.getRequestURI().equals("/actuator/health");
    }
}
