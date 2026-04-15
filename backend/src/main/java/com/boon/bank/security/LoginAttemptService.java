package com.boon.bank.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final String KEY_PREFIX = "login:fail:";

    @Value("${app.login.max-attempts}")
    private int maxAttempts;

    @Value("${app.login.lock-duration-minutes}")
    private int lockMinutes;

    private final StringRedisTemplate redis;

    public boolean isLocked(String username) {
        try {
            var val = redis.opsForValue().get(KEY_PREFIX + username);
            return val != null && Integer.parseInt(val) >= maxAttempts;
        } catch (Exception e) {
            log.error("Redis unavailable, blocking login for safety: {}", e.getMessage());
            return true; // fail-closed for banking
        }
    }

    public void recordFailure(String username) {
        try {
            var lockDuration = Duration.ofMinutes(lockMinutes);
            var key = KEY_PREFIX + username;
            var count = redis.opsForValue().increment(key);
            redis.expire(key, lockDuration);
            log.warn("Login fail: user={} attempts={}", username, count);
            if (count != null && count >= maxAttempts) {
                log.warn("Account locked: user={} for {}min", username, lockMinutes);
            }
        } catch (Exception e) {
            log.warn("Redis error tracking login: {}", e.getMessage());
        }
    }

    public void resetAttempts(String username) {
        try {
            redis.delete(KEY_PREFIX + username);
        } catch (Exception e) {
            // ignore
        }
    }
}
