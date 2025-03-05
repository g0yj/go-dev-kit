package com.app.api.login.jwt;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *  JWT의 생성, 파싱, 인증 등의 주요 기능을 담당하는 클래스
 */
@Component
@Slf4j
public class JwtTokenProvider {
    private final String secretKey  = "6v7aNfE9KtPqXb3yGz5CdMqT8hYwVgLpQrZoUsJmKnMo="; // 비밀 키


    // JWT 토큰 생성
    public String createToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", user.getAuthorities().toArray()[0].toString())  // type (권한 정보) - 시큐리티는 role을 일반적으로 사용하는데, 기존에 사용하던 게 있다면 커스터마이징 필요함!
                .setIssuedAt(new Date())  // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))  // 만료 시간: 1시간
                .signWith(SignatureAlgorithm.HS512, secretKey)  // 비밀 키로 서명
                .compact();
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            JwtParser jwtParser = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build(); // 서명 키 설정
            jwtParser.parseClaimsJws(token);  // 토큰 파싱
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            return false;  // 예외 발생 시 유효하지 않은 토큰으로 처리
        }
    }

    // JWT 토큰에서 Authentication 객체 추출
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String username = claims.getSubject();
        String type = claims.get("type", String.class);

        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_" + type));
        User principal = new User(username, "", authorities);  // 사용자 정보로 User 객체 생성
        return new UsernamePasswordAuthenticationToken(principal, token, authorities); // 인증 객체 생성
    }

    // JWT 토큰에서 Claims (payload) 파싱
    public Claims parseClaims(String token) {
        try {
            JwtParser jwtParser = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build();

            Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
            Claims claims = claimsJws.getBody();

            // ✅ JWT 내부 정보 로깅
            log.info("🔹 JWT 디코딩 결과: {}", claims);
            return claims;
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("토큰이 만료되었습니다.", e);
        } catch (JwtException e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.", e);
        }
    }

}
