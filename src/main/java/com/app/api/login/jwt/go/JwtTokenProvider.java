package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.TokenBlacklistService;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenProvider(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * ✅ JWT 서명 키 생성
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }

    /** ✅ JWT 토큰 생성 */
    public String generateToken(String username, UserType type) {
        String token = Jwts.builder()
                .setSubject(username)
                .claim("type", type.name())  // UserType 저장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        log.info("✅ JWT 생성 완료 - 사용자: {}, 역할: {}", username, type);
        return token;
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