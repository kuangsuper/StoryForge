package com.toonflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_TOKEN_PREFIX = "toonflow:token:";

    public JwtTokenProvider(@Value("${toonflow.jwt.secret}") String secret,
                            @Value("${toonflow.jwt.expiration}") long expiration,
                            StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(Long userId, String name, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("name", name)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();

        // 存 Redis，支持主动失效
        redisTemplate.opsForValue().set(
                REDIS_TOKEN_PREFIX + userId, token, expiration, TimeUnit.MILLISECONDS);

        return token;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());

            // 检查 Redis 中是否存在（支持主动失效）
            String cached = redisTemplate.opsForValue().get(REDIS_TOKEN_PREFIX + userId);
            return token.equals(cached);
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateToken(Long userId) {
        redisTemplate.delete(REDIS_TOKEN_PREFIX + userId);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
