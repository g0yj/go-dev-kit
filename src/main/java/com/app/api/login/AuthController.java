package com.app.api.login;

import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.jwt.dto.AuthenticationResponse;
import com.app.api.login.jwt.dto.LoginRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;


    // ✅ 로그인 엔드포인트 (Access Token & Refresh Token 발급)
    @PostMapping("/jwt")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        // 로그인 로직을 AuthService로 위임
        AuthenticationResponse authenticationResponse = authService.login(request);

        // 🔹 Refresh Token을 HTTPOnly 쿠키에 저장
        Cookie refreshTokenCookie = new Cookie("refresh_token", authenticationResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 30);  // 30일

        response.addCookie(refreshTokenCookie);  // 응답에 쿠키 추가

        return ResponseEntity.ok(authenticationResponse);
    }

    // ✅ Refresh Token을 사용하여 새로운 Access Token 발급
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 🔹 요청에서 Refresh Token 가져오기 (쿠키에서 추출)
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(new AuthenticationResponse(null, "Invalid or expired Refresh Token"));
        }

        // 🔹 Refresh Token에서 사용자 정보 추출
        String username = jwtTokenProvider.parseClaims(refreshToken).getSubject();
        User user = (User) userDetailsService.loadUserByUsername(username);

        // 🔹 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user);

        return ResponseEntity.ok(new AuthenticationResponse(newAccessToken, refreshToken));
    }

    // ✅ Refresh Token을 수동으로 쿠키에 설정하는 API (테스트용)
    @GetMapping("/set-cookie")
    public ResponseEntity<String> setCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refresh_token", "sample-refresh-token");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 30);

        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok("Refresh Token Cookie Set");
    }

    @PostMapping("/jwt/logout")
    public ResponseEntity<String> logout(HttpServletResponse httpServletResponse){
        // 로그아웃 서비스 호출
        authService.logout(httpServletResponse);

        return ResponseEntity.ok("로그아웃 완료");
    }
}
