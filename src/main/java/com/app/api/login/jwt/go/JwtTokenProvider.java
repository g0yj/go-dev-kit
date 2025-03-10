package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ JWT 토큰 생성 및 검증을 담당하는 클래스
 * - AccessToken 및 RefreshToken 생성
 * - JWT 유효성 검사 및 사용자 정보 추출
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.access-token-expiration}")
    private Duration accessTokenExpiration; // ✅ Duration 타입으로 변경

    @Value("${spring.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration; // ✅ Duration 타입으로 변경

    /**
     * ✅ JWT 서명 키 생성
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }

    /** ✅ Access Token 생성 */
    /** ✅ Access Token 생성 */
    public String generateAccessToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration.toMillis())) // ✅ Duration 사용
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ Refresh Token 생성 */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration.toMillis())) // ✅ Duration 사용
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ Refresh Token 만료 시간 반환 */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plus(refreshTokenExpiration); // ✅ Duration을 LocalDateTime으로 변환
    }

    /** ✅ JWT 유효성 검사 */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            log.info("✅ JWT 유효성 검증 성공");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⏳ JWT 만료됨: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("❌ JWT 검증 실패: {}", e.getMessage());
        }
        return false;
    }



    /** ✅ JWT에서 사용자명 추출 */
    public String getUsernameFromToken(String token) {
        return Jwts.parser().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /** ✅ JWT에서 역할(UserType) 추출 */
    public UserType getTypeFromToken(String token) {
        String typeStr = Jwts.parser().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().get("type", String.class);
        return UserType.valueOf(typeStr);
    }

}