package com.app.api.login.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtTokenFilter 클래스는 요청이 올 때마다 JWT 토큰을 검사하여 인증을 진행하는 필터
 *  Spring Security의 OncePerRequestFilter를 상속하여 한 번만 필터링이 실행되도록 합니다.
 */
@Component
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

    private JwtTokenProvider jwtTokenProvider;

    // ✅ SecurityConfig에서 주입 (순환 참조 방지)
    public void setJwtTokenProvider(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = resolveToken(httpRequest);

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            String refreshToken = resolveRefreshToken(httpRequest);
            if (refreshToken != null) {
                try {
                    String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);
                    httpResponse.setHeader("Authorization", "Bearer " + newAccessToken);
                } catch (RuntimeException e) {
                    log.error("❌ Refresh Token 검증 실패: {}", e.getMessage());
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        }

        filterChain.doFilter(httpRequest, httpResponse);
    }

    // ✅ Authorization 헤더에서 Access Token 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ✅ HTTP 요청에서 Refresh Token 쿠키 추출
    private String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("refresh_token")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

