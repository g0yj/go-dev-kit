package com.app.api.login.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "6v7aNfE9KtPqXb3yGz5CdMqT8hYwVgLpQrZoUsJmKnMo=";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1시간

    private final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY)); // ✅ SecretKey 타입 사용

    // ✅ 최신 방식으로 JWT 생성
    public String generateToken(String username, String type) {
        return Jwts.builder()
                .subject(username)  // `setSubject()` → `subject()`
                .claim("type", type)
                .issuedAt(new Date())  // `setIssuedAt()` → `issuedAt()`
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // `setExpiration()` → `expiration()`
                .signWith(key, Jwts.SIG.HS256)  // ✅ 서명 알고리즘 명시 (이전 코드 문제 해결)
                .compact();
    }

    // ✅ 최신 방식으로 JWT 검증 및 클레임 추출
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key) // ✅ `verifyWith()` 사용 (SecretKey 타입 그대로 유지)
                    .build()
                    .parseSignedClaims(token) // `parseClaimsJws()` → `parseSignedClaims()`
                    .getPayload();
        } catch (SignatureException e) {
            return null; // 서명이 잘못된 경우
        } catch (Exception e) {
            return null; // 기타 오류 발생 시
        }
    }
}


