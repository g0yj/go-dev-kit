package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Spring Security 인증 및 로그아웃
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwtSecurityService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * ✅ JWT 인증 정보 SecurityContext에 저장
     */
    public void authenticateUser(String accessToken) {
        log.info("🔑 SecurityContext 설정 - Access Token 검증 시작");

        String username = jwtTokenProvider.getUsernameFromToken(accessToken);
        UserType userType = jwtTokenProvider.getTypeFromToken(accessToken);
        log.debug("📌 토큰에서 추출한 사용자 정보 - username: {}, role: {}", username, userType);

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + userType.name())
        );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, "", authorities
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("✅ SecurityContext에 '{}' 인증 정보 저장 완료", username);
    }

    /**
     * ✅ Spring Security 로그아웃
     */
    public ResponseEntity<?> securityLogout(HttpServletRequest request) {
        log.info("🔓 Spring Security 로그아웃 API 호출");

        // 1️⃣ Access Token & Refresh Token 추출
        String accessToken = jwtTokenProvider.extractToken(request, "Authorization");
        String refreshToken = jwtTokenProvider.extractToken(request, "Refresh-Token");

        if (accessToken == null || refreshToken == null) {
            log.warn("⚠️ 로그아웃 실패 - 토큰이 제공되지 않음 (AccessToken: {}, RefreshToken: {})", accessToken, refreshToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("로그아웃 실패: 토큰이 필요합니다.");
        }

        log.debug("🔑 Access Token: {}", accessToken);
        log.debug("🔑 Refresh Token: {}", refreshToken);

        // 2️⃣ 토큰 유효성 검사
        boolean isAccessTokenValid = jwtTokenProvider.validateToken(accessToken);
        boolean isRefreshTokenValid = jwtTokenProvider.validateToken(refreshToken);

        if (!isAccessTokenValid || !isRefreshTokenValid) {
            log.warn("❌ 유효하지 않은 토큰 감지 - (Access: {}, Refresh: {})", isAccessTokenValid, isRefreshTokenValid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그아웃 실패: 유효하지 않은 토큰입니다.");
        }

        // 3️⃣ Refresh Token 삭제 (DB에서 제거)
        refreshTokenRepository.findByRefreshToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            log.info("🗑️ 사용자 '{}'의 Refresh Token 삭제 완료", token.getUsername());
        });

        // 4️⃣ SecurityContext 초기화
        SecurityContextHolder.clearContext();
        log.info("🔄 SecurityContext 초기화 완료");

        log.info("✅ Spring Security 로그아웃 완료");
        return ResponseEntity.ok("로그아웃 성공");
    }
}
