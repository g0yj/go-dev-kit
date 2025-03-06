package com.app.api.login.jwt;

import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtSecurityService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ObjectProvider<JwtTokenProvider> jwtTokenProviderProvider; // ✅ ObjectProvider를 활용한 동적 주입

    private JwtTokenProvider getJwtTokenProvider() {
        return jwtTokenProviderProvider.getIfAvailable();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("🔎 JwtSecurityService - 사용자 조회: {}", username);
        return userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getType().name()) // ✅ ROLE 설정
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("❌ 사용자를 찾을 수 없습니다."));
    }

    // ✅ JWT 토큰을 검증하고 SecurityContext에 인증 정보 저장
    public void authenticateUser(String token) {
        JwtTokenProvider jwtTokenProvider = getJwtTokenProvider();
        if (jwtTokenProvider != null && jwtTokenProvider.validateAccessToken(token)) { // ✅ validateAccessToken() 사용
            String username = jwtTokenProvider.getUsernameFromToken(token);
            List<GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromToken(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("✅ JWT 인증 성공 - username: {}", username);
            }
        } else {
            log.warn("❌ JWT 인증 실패 - 토큰이 유효하지 않음");
        }
    }
}
