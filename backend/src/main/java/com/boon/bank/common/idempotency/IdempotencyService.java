package com.boon.bank.common.idempotency;

import com.boon.bank.exception.business.DuplicateIdempotencyKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "idem:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public void reserve(String key) {
        Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + key, "1", TTL);
        if (ok == null || !ok) {
            throw new DuplicateIdempotencyKeyException();
        }
    }

    public void release(String key) {
        redis.delete(PREFIX + key);
    }
}
