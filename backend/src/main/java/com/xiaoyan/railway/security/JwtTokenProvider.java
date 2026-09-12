package com.xiaoyan.railway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Generates and parses HS256 JWTs carrying {@code userId} (subject) and {@code phone}.
 * Uses the jjwt 0.12.x API: {@code Jwts.parser().verifyWith(key).build().parseSignedClaims(...)}.
 */
@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expireMillis;

    public JwtTokenProvider(@Value("${railway.jwt.secret}") String secret,
                            @Value("${railway.jwt.expire-days:7}") int expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = TimeUnit.DAYS.toMillis(expireDays);
    }

    public String generate(Long userId, String phone) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public TokenPayload parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new TokenPayload(Long.valueOf(claims.getSubject()), claims.get("phone", String.class));
    }

    public record TokenPayload(Long userId, String phone) { }
}
