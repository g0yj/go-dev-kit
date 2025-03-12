package com.app.api.login.jwt.go;


import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/jwt")
@RequiredArgsConstructor
public class JwtTokenController {
    private final JwtTokenService jwtTokenService;

    /** ✅ 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<JwtTokenResponse> register(@RequestBody JwtTokenRequest request) {
        log.info("📝 회원가입 API 호출 - username: {}", request.getUsername());

        JwtTokenResponse response = jwtTokenService.register(request);
        log.info("✅ 회원가입 완료 - username: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }
    /** ✅ Access토큰 로그인 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtTokenRequest request) {
        log.info("🔑 로그인 API 호출 - username: {}", request.getUsername());

        try {
            JwtTokenResponse response = jwtTokenService.login(request);
            log.info("🚀 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("⚠️ 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(new JwtTokenResponse(null, null, null, null, null));
        }
    }

    /** ✅ Access 토큰 로그아웃 (Authorization 헤더에서 토큰 추출) */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("🔒 로그아웃 API 호출 - Authorization: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("⚠️ 로그아웃 실패 - 유효하지 않은 토큰");
        }

        String accessToken = authHeader.substring(7);
        jwtTokenService.logout(accessToken);

        log.info("✅ 로그아웃 완료");
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }

    /** ✅ Access + Refresh 로그인
     *  - 사용자가 username + password 입력하여 로그인 요청
     * */
    @PostMapping("/refresh/login")
    public ResponseEntity<?> refreshLogin(@RequestBody JwtTokenRequest request) {
        log.info("🔑 로그인 API 호출 - username: {}", request.getUsername());

        try {
            JwtTokenResponse response = jwtTokenService.refreshLogin(request);
            log.info("🚀 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("⚠️ 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(new JwtTokenResponse(null, null, null, null, null));
        }
    }

    /** ✅ Access + Refresh 로그아웃 (Refresh-Token 헤더 활용) */
    @PostMapping("/refresh/logout")
    public ResponseEntity<String> refreshLogout(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("🔒 Refresh 로그아웃 API 호출 - Refresh-Token: {}", refreshToken);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body("⚠️ 로그아웃 실패 - Refresh Token이 없습니다.");
        }

        jwtTokenService.refreshLogout(refreshToken);
        log.info("✅ 로그아웃 완료");
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }

    /** ✅ Access Token 갱신 (Refresh Token 기반)
     *  - 사용자가 비밀번호를 입력할 필요 없음
     *  - 로그인을 유지할 수 있음 (Access Token이 만료되어도 Refresh Token이 있으면 자동 갱신 가능)
     * */
    @PostMapping("/refresh/map/login")
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("🔄 Access Token 재발급 요청 - Refresh-Token: {}", refreshToken);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "⚠️ Refresh Token이 없습니다."));
        }

        Map<String, String> newTokens = jwtTokenService.refreshAccessToken(refreshToken);
        log.info("✅ 새로운 Access Token 발급 완료");

        return ResponseEntity.ok(newTokens);
    }

    /**
     * ✅ Refresh Token 로그아웃 (Refresh-Token 삭제)
     */
    @PostMapping("/refresh/map/logout")
    public ResponseEntity<String> logoutWithRefreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("🔒 Refresh 로그아웃 API 호출 - Refresh-Token: {}", refreshToken);

        try {
            jwtTokenService.logoutWithRefreshToken(refreshToken);
            return ResponseEntity.ok("✅ 로그아웃 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    /** ✅ Spring Security 로그인 */
    @PostMapping("/security/login")
    public ResponseEntity<JwtTokenResponse> securityLogin(@RequestBody JwtTokenRequest request) {
        log.info("🔐 Spring Security JWT 로그인 API 호출 - username: {}", request.getUsername());
        // ✅ 서비스에서 로그인 처리 및 Role 기반 URL 반환
        JwtTokenResponse response = jwtTokenService.securityLogin(request);
        return ResponseEntity.ok(response);
    }

    /** ✅ Spring Security 로그아웃 */
    @PostMapping("/security/logout")
    public ResponseEntity<?> securityLogout(HttpServletRequest request) {
        log.info("🔓 Spring Security 로그아웃 API 호출");
        return jwtTokenService.securityLogout(request);
    }
}
