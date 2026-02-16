package com.lujianfeng.spanner.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final long CLOCK_SKEW_SECONDS = 60L;
    private Key key;


    /**
     * 项目启动后初始化一次注入安全密钥
     */
    @PostConstruct
    public void init() {
        String SECRET_KEY = "wocaonima3472y3427y41jhbsjkashdjkhvsdkjbasdfasdf" +
                "3412$@#$@#$@^^^DFWEWF";
        key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(String username) {
        return generateAccessToken(username);
    }

    public String generateAccessToken(String username) {
        return generateTokenByType(username, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_EXPIRE_MS);
    }

    public String generateRefreshToken(String username) {
        return generateTokenByType(username, REFRESH_TOKEN_TYPE, REFRESH_TOKEN_EXPIRE_MS);
    }

    public String extractUsername(String token) {
        return extractUsernameByType(token, ACCESS_TOKEN_TYPE);
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractUsernameByType(token, REFRESH_TOKEN_TYPE);
    }

    public long getAccessTokenExpiresInSeconds() {
        return ACCESS_TOKEN_EXPIRE_MS / 1000L;
    }

    private String generateTokenByType(String username, String tokenType, long expireMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }

    private String extractUsernameByType(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedType.equals(tokenType)) {
                log.warn("token type mismatch. expected={}, actual={}", expectedType, tokenType);
                return null;
            }
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.info("token expired");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("token invalid: {}", e.getMessage());
            return null;
        }

    }

}
