package com.app.api.login.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }

    // ✅ AccessToken 생성
    public String generateAccessToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ RefreshToken 생성
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ RefreshToken 검증 및 사용자명 반환
    public String validateRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.error("❌ 검증 실패: 토큰이 비어 있음");
                throw new IllegalArgumentException("❌ 유효하지 않은 Refresh Token (비어 있음)");
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.info("✅ Refresh Token 유효성 검사 성공 - username: {}", claims.getSubject());
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.error("❌ 만료된 Refresh Token: {}", e.getMessage());
            throw new IllegalArgumentException("❌ 만료된 Refresh Token");
        } catch (MalformedJwtException e) {
            log.error("❌ 손상된 Refresh Token: {}", e.getMessage());
            throw new IllegalArgumentException("❌ 유효하지 않은 Refresh Token (손상됨)");
        } catch (SignatureException e) {
            log.error("❌ 서명 불일치 Refresh Token: {}", e.getMessage());
            throw new IllegalArgumentException("❌ 유효하지 않은 Refresh Token (서명 불일치)");
        } catch (Exception e) {
            log.error("❌ 기타 오류로 인한 Refresh Token 검증 실패: {}", e.getMessage());
            throw new IllegalArgumentException("❌ 유효하지 않은 Refresh Token");
        }
    }

    // ✅ 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // ✅ 만료됨
        } catch (Exception e) {
            throw new IllegalArgumentException("❌ 유효하지 않은 토큰");
        }
    }

    // ✅ RefreshToken 갱신 (새로운 RefreshToken 생성 후 반환)
    public String refreshRefreshToken(String oldRefreshToken) {
        String username = validateRefreshToken(oldRefreshToken); // ✅ 검증 후 사용자명 추출

        // 기존 토큰 만료되었는지 확인 후, 새 토큰 발급
        if (isTokenExpired(oldRefreshToken)) {
            throw new IllegalArgumentException("❌ 기존 Refresh Token이 만료됨, 재로그인이 필요합니다.");
        }

        return generateRefreshToken(username);
    }

    // ✅ RefreshToken 만료 시간 반환
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

}
