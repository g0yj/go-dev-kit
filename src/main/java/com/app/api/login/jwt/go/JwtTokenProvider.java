package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    public String generateAccessToken(String username, UserType userType) {
        log.debug("🔑 AccessToken 생성 시작 - username: {}, userType: {}", username, userType);

        if (userType == null) {
            log.error("❌ AccessToken 생성 오류 - userType이 null입니다.");
            throw new IllegalArgumentException("userType이 null일 수 없습니다.");
        }

        String token = Jwts.builder()
                .setSubject(username)
                .claim("type", userType.name())  // ✅ UserType을 Enum에서 String으로 변환하여 저장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration.toMillis()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("✅ AccessToken 생성 완료 - token: {}", token);
        return token;
    }

    /** ✅ Refresh Token 생성 */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration.toMillis()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ Refresh Token 만료 시간 반환 */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plus(refreshTokenExpiration);
    }

    /** ✅ JWT 유효성 검사 */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** ✅ JWT에서 역할(UserType) 추출 */
    public UserType getTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("🔍 JWT Claims: {}", claims);

            String typeStr = claims.get("type", String.class);
            log.debug("🔍 JWT에서 읽은 type 값: {}", typeStr);

            if (typeStr == null || typeStr.isEmpty()) {
                log.error("❌ JWT에서 type 클레임이 없습니다!");
                throw new IllegalArgumentException("❌ JWT에서 type을 추출할 수 없습니다.");
            }

            return UserType.valueOf(typeStr);
        } catch (Exception e) {
            log.error("❌ JWT에서 type 추출 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("❌ JWT에서 type을 추출할 수 없습니다.");
        }
    }

    /**
     * ✅ 요청 헤더에서 특정 키 값을 기반으로 토큰을 추출하는 메서드
     * - Authorization 헤더 또는 Refresh-Token 헤더에서 토큰을 가져올 때 사용 가능
     *
     * @param request HTTP 요청 객체
     * @param headerKey 헤더 키 값 (예: "Authorization" 또는 "Refresh-Token")
     * @return 추출된 토큰 (없으면 null)
     */
    public String extractToken(HttpServletRequest request, String headerKey) {
        String headerValue = request.getHeader(headerKey);

        if (headerValue != null) {
            // "Authorization" 헤더에서 Bearer 토큰을 추출할 경우
            if (headerKey.equalsIgnoreCase("Authorization") && headerValue.startsWith("Bearer ")) {
                return headerValue.substring(7);  // "Bearer " 제거 후 반환
            }
            return headerValue; // 일반적인 Refresh-Token 같은 경우 그대로 반환
        }

        log.warn("❌ 요청 헤더 '{}'에서 토큰을 찾을 수 없음", headerKey);
        return null;
    }


    /**
     * ✅ JWT에서 Spring Security 권한 목록 추출
     */
    public List<GrantedAuthority> getGrantedAuthoritiesFromToken(String token) {
        log.debug("✅ JWT에서 Spring Security 권한 목록 추출 > token : {}", token);

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("🔍 JWT Claims 전체 정보: {}", claims);
        } catch (Exception e) {
            log.error("❌ JWT 파싱 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("❌ JWT를 파싱할 수 없습니다.");
        }

        // 🔍 type 클레임 확인
        String typeStr = claims.get("type", String.class);
        log.debug("🔍 JWT에서 읽은 type 값: {}", typeStr);

        // ❌ type이 null이거나 빈 문자열이면 예외 발생
        if (typeStr == null || typeStr.isEmpty()) {
            log.error("🚨 JWT에서 권한 정보가 없습니다. 토큰 검증 실패!");
            throw new IllegalArgumentException("❌ JWT에서 권한 정보가 없습니다.");
        }

        // 🔍 UserType을 Enum으로 변환
        UserType userType;
        try {
            userType = UserType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            log.error("❌ JWT의 'type' 값이 올바르지 않습니다: {}", typeStr);
            throw new IllegalArgumentException("❌ JWT의 'type' 값이 올바르지 않습니다.");
        }

        // 🔍 ROLE_ 접두사가 없는 경우 추가
        String role = "ROLE_" + userType.name();

        log.debug("✅ 최종 role 값: {}", role);

        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}