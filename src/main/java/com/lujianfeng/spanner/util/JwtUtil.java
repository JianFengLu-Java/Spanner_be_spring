package com.lujianfeng.spanner.util;

import io.jsonwebtoken.ExpiredJwtException;
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


    /**
     * 创建Jwt Token
     *
     * @param username 用户名参数
     */
    public String generateToken(String username) {
        long EXPIRATION_TIME = 86400L;
        return Jwts.builder()  //链式调用
                .subject(username)  //设置用户信息
                .issuedAt(new Date()) //创建时间
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) //设置过期时间
                .signWith(key) //签名
                .compact();
    }

    /**
     * 解析用户UserN🤔me
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            log.info("token expired");
            return null;
        }

    }

}
