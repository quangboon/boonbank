package com.boon.bank.common.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    public static final String HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String key = request.getHeader(HEADER);
        if (StringUtils.hasText(key)) {
            idempotencyService.reserve(key);
        }
        return true;
    }
}
