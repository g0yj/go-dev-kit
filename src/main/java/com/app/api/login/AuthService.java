package com.app.api.login;

import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.jwt.dto.AuthenticationResponse;
import com.app.api.login.jwt.dto.LoginRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserDetailsService userDetailsService;  // 사용자 인증 서비스
    private final PasswordEncoder passwordEncoder;  // 비밀번호 인코딩 서비스
    private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰 제공자

    public AuthenticationResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 🔹 사용자 인증
        User user = (User) userDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 🔹 Access Token & Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // 🔹 토큰 반환
        return new AuthenticationResponse(accessToken, refreshToken);
    }

    public void logout(HttpServletResponse response) {
        // Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refresh_token", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 쿠키 즉시 만료 처리

        response.addCookie(refreshTokenCookie);  // 응답에 쿠키 삭제 추가

        log.debug("로그아웃: 사용자 쿠키에서 Refresh Token 삭제");
    }

}
