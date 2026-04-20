package com.boon.bank.security.logger;

import com.boon.bank.security.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {


    private static final String REQ_ID = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var reqId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQ_ID, reqId);
        response.setHeader("X-Request-Id", reqId);

        var start = System.currentTimeMillis();
        var method = request.getMethod();
        var uri = request.getRequestURI();
        var ip = extractIp(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
//            SecurityUtil.clearCache();
            var duration = System.currentTimeMillis() - start;
            var status = response.getStatus();
            var user = getUsername();

            if (status >= 500) {
                log.error("[{}] {} {} user={} ip={} -> {} ({}ms)", reqId, method, uri, user, ip, status, duration);
            } else if (status >= 400) {
                log.warn("[{}] {} {} user={} ip={} -> {} ({}ms)", reqId, method, uri, user, ip, status, duration);
            } else {
                log.info("[{}] {} {} user={} ip={} -> {} ({}ms)", reqId, method, uri, user, ip, status, duration);
            }
            MDC.remove(REQ_ID);
        }
    }

    private String getUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "-";
    }

    private String extractIp(HttpServletRequest req) {
        var xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        var uri = req.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs");
    }
}
