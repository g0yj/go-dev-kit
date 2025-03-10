package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.login.session.dto.SessionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/jwt")
@RequiredArgsConstructor
public class JwtTokenController {
    private final JwtTokenService jwtTokenService;

    /** ✅ JWT 로그인 (토큰 발급) */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtTokenRequest request) {
        log.info("🔑 로그인 API 호출 - username: {}", request.getUsername());

        try {
            JwtTokenResponse response = jwtTokenService.login(request);
            log.info("🚀 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("⚠️ 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(new JwtTokenResponse(null, null, null, UserType.C));
        }
    }

    /** ✅ 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<JwtTokenResponse> register(@RequestBody JwtTokenRequest request) {
        log.info("📝 회원가입 API 호출 - username: {}", request.getUsername());

        JwtTokenResponse response = jwtTokenService.register(request);
        log.info("✅ 회원가입 완료 - username: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

    /** ✅ 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody String username) {
        log.info("🔒 로그아웃 API 호출 - username: {}", username);

        jwtTokenService.logout(username);

        log.info("✅ 로그아웃 완료 - username: {}", username);
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }
    /** ✅ JWT 로그인 (토큰 발급) */
    @PostMapping("/refresh/login")
    public ResponseEntity<?> refreshLogin(@RequestBody JwtTokenRequest request) {
        log.info("🔑 로그인 API 호출 - username: {}", request.getUsername());

        try {
            JwtTokenResponse response = jwtTokenService.refreshLogin(request);
            log.info("🚀 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("⚠️ 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(new JwtTokenResponse(null, null, null, UserType.C));
        }
    }


    /** ✅ 로그아웃 */
    @PostMapping("/refresh/logout")
    public ResponseEntity<String> refreshLogout(@RequestBody String username) {
        log.info("🔒 로그아웃 API 호출 - username: {}", username);

        jwtTokenService.logout(username);

        log.info("✅ 로그아웃 완료 - username: {}", username);
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }

    /** ✅ Spring Security 로그인 */
    @PostMapping("/security/login")
    public ResponseEntity<?> securityLogin(@RequestBody SessionRequest request) {
        log.info("🔐 Spring Security 로그인 API 호출 - username: {}", request.getUsername());
        return jwtTokenService.securityLogin(request);
    }

    /** ✅ Spring Security 로그아웃 */
    @PostMapping("/security/logout")
    public ResponseEntity<?> securityLogout() {
        log.info("🔓 Spring Security 로그아웃 API 호출");
        return jwtTokenService.securityLogout();
    }

}
