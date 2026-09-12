package com.xiaoyan.railway.user;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock SMS verification-code service backed by Redis:
 * code key {@code sms:code:{phone}} (TTL 5 min, single use) and
 * send rate-limit key {@code sms:sendlmt:{phone}} (TTL 60 s).
 */
@Service
public class SmsCodeService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public SmsCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Returns true if this phone is not currently rate-limited. */
    public boolean canSend(String phone) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent("sms:sendlmt:" + phone, "1", SEND_TTL));
    }

    /** Generates a 6-digit code, stores it, and returns it (dev builds return it to the client). */
    public String generateAndStore(String phone) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set("sms:code:" + phone, code, CODE_TTL);
        return code;
    }

    /** Verifies and consumes the code (single use). */
    public boolean verify(String phone, String code) {
        String key = "sms:code:" + phone;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(code)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }
}
