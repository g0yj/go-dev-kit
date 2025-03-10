package com.app.api.login.jwt.go;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ✅ JWT 필터 (Spring Security와 함께 사용)
 * - 요청 헤더에서 JWT 추출
 * - JWT 유효성 검사 후 사용자 정보 추출
 * - SecurityContextHolder에 인증 정보 저장
 */

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider ) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            log.info("🔑 JWT 토큰이 요청 헤더에서 추출되었습니다: {}", token);

            if (jwtTokenProvider.validateToken(token)) {
                log.info("✅ JWT 토큰이 유효합니다.");

                // ✅ 사용자 정보 추출
                String username = jwtTokenProvider.getUsernameFromToken(token);
                List<GrantedAuthority> authorities = jwtTokenProvider.getGrantedAuthoritiesFromToken(token);

                log.info("🔑 사용자 '{}'의 권한을 추출했습니다: {}", username, authorities);

                // ✅ UserDetails 생성
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(username, "", authorities);

                // ✅ SecurityContext에 저장할 Authentication 객체 생성 (UserDetails 포함)
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.info("✅ 인증 정보가 SecurityContext에 저장되었습니다: {}", authenticationToken);
            } else {
                log.warn("❌ JWT 토큰이 유효하지 않습니다.");
            }
        } else {
            log.warn("❌ 요청 헤더에서 JWT 토큰을 찾을 수 없습니다.");
        }

        filterChain.doFilter(request, response);
    }


    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("✅ Bearer 토큰 추출됨: {}", bearerToken);
            return bearerToken.substring(7);  // "Bearer "를 제외한 토큰 반환
        }
        log.warn("❌ Bearer 토큰이 없음");
        return null;
    }
}

