package com.app.api.login.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 *  JWT의 생성, 파싱, 인증 등의 주요 기능을 담당하는 클래스
 */
@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    // ✅ application.yml에서 SecretKey & 만료 시간 가져오기
    public JwtTokenProvider(
            @Value("${spring.jwt.secret}") String secretKeyBase64,
            @Value("${spring.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${spring.jwt.refresh-token-expiration}") long refreshTokenExpiration) {

        // 🔹 Base64 디코딩된 SecretKey 설정
        byte[] decodedKey = Decoders.BASE64.decode(secretKeyBase64);
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("✅ JWT SecretKey 로드 완료 (길이: {}바이트)", decodedKey.length);
    }

    // ✅ Access Token 생성
    public String createAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", user.getAuthorities().toArray()[0].toString()) // 사용자 역할
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // ✅ Refresh Token 생성
    public String createRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", user.getAuthorities().toArray()[0].toString()) // 사용자 역할
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // ✅ JWT 검증 및 Claims 추출
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("❌ JWT 만료: {}", e.getMessage());
            return e.getClaims(); // 만료된 경우에도 Claims 반환
        } catch (JwtException e) {
            log.error("❌ JWT 검증 실패: {}", e.getMessage());
            throw new RuntimeException("JWT 검증 실패", e);
        }
    }


    // 🔹 Access Token 유효성 검사
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date()); // 만료되지 않은 경우 true
        } catch (JwtException e) {
            log.error("❌ JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 🔹 Access Token에서 인증 정보 생성
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String username = claims.getSubject();
        String type = claims.get("type", String.class);

        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority(type));
        User principal = new User(username, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 🔹 Refresh Token을 이용해 Access Token 갱신
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);  // ✅ 예외 발생 가능
            log.info("✅ Refresh Token Claims: {}", claims);

            if (claims.getExpiration().before(new Date())) {
                log.warn("❌ Refresh Token이 만료되었습니다.");
                throw new RuntimeException("Refresh Token이 만료되었습니다.");
            }

            String username = claims.getSubject();
            String role = claims.get("type", String.class);

            log.info("✅ Refresh Token 검증 성공! 사용자: {}, 권한: {}", username, role);

            User user = new User(username, "", List.of(new SimpleGrantedAuthority(role)));
            return createAccessToken(user);
        } catch (ExpiredJwtException e) {
            log.error("❌ Refresh Token 만료: {}", e.getMessage());
            throw new RuntimeException("Refresh Token 만료", e);
        } catch (JwtException e) {
            log.error("❌ Refresh Token 검증 실패: {}", e.getMessage());
            throw new RuntimeException("Refresh Token 검증 실패", e);
        }
    }

    // 🔹 HTTP 요청에서 Access Token을 가져오는 메서드
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ✅ HTTP 요청에서 Refresh Token을 쿠키에서 가져오는 메서드
    public String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    log.info("✅ Refresh Token 쿠키 확인: {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        log.warn("❌ Refresh Token 쿠키가 존재하지 않습니다.");
        return null;
    }


}




