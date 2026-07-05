package com.microsoft.aediumbackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private static final String SECRET_KEY = "53fe621cc57c091a5c2a20c1c0f851fea5c5e603ce506b334e0d709a90bdeff4";
    private static final long EXPIRATION_TIME = 7 * 24 * 3600 * 1000;
    public static String generateToken(Map<String, Object> dataMap) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
        return Jwts.builder()
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .addClaims(dataMap)
                .signWith(key) // 签名
                .compact();
    }
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
