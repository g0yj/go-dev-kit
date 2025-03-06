package com.app.api.login.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider; // JwtTokenProvider 사용

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1️⃣ JWT 토큰 추출
        String jwt = getJwtFromRequest(request);

        // 2️⃣ 토큰이 존재하는 경우 인증 처리
        if (jwt != null && jwtTokenProvider.validateAccessToken(jwt)) { // ✅ validateAccessToken() 사용
            String username = jwtTokenProvider.getUsernameFromToken(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 3️⃣ 토큰 유효성 검사 후 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, jwtTokenProvider.getAuthoritiesFromToken(jwt));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4️⃣ 인증 정보 SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 5️⃣ 필터 체인 진행 (다음 필터로 요청 전달)
        filterChain.doFilter(request, response);
    }

    // ✅ JWT 토큰을 HTTP 요청 헤더에서 추출하는 메서드
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후 토큰만 추출
        }
        return null;
    }
}
