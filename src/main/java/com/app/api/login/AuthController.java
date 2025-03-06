package com.app.api.login;

import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/session/login")
    public ResponseEntity<SessionResponse> sessionLogin(@RequestBody SessionRequest request, HttpServletRequest httpRequest) {
        log.debug("Session 로그인 요청 - username: {}, type: {}", request.getUsername(), request.getType());

        try {
            SessionResponse response = authService.sessionLogin(httpRequest, request);
            log.info("Session 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Session 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }
    // ✅ 세션 로그아웃
    @PostMapping("/session/logout")
    public ResponseEntity<String> sessionLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            String username = (String) session.getAttribute("username");
            log.info("Session 로그아웃 요청 - username: {}", username);

            authService.sessionLogout(request); // ✅ AuthService에서 DB에서 세션 삭제까지 처리
            return ResponseEntity.ok("Session 로그아웃 성공!");
        } else {
            log.warn("로그아웃 요청했지만 세션이 없음");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 로그아웃된 상태입니다.");
        }
    }
}


/**
 * @RestController
 * @RequestMapping("/auth")
 * @RequiredArgsConstructor
 * @Slf4j
 * public class AuthController {
 *
 *     private final AuthService authService;
 *
 *     @PostMapping("/session/login")
 *     public ResponseEntity<SessionResponse> sessionLogin(@RequestBody SessionRequest request, HttpServletRequest httpRequest) {
 *         return ResponseEntity.ok(authService.sessionLogin(httpRequest, request));
 *     }
 *
 *     @PostMapping("/session/logout")
 *     public ResponseEntity<Void> sessionLogout(HttpServletRequest request) {
 *         authService.sessionLogout(request);
 *         return ResponseEntity.ok().build();
 *     }
 *
 *     @PostMapping("/jwt/login")
 *     public ResponseEntity<JwtResponse> jwtLogin(@RequestBody JwtRequest jwtRequest) {
 *         return ResponseEntity.ok(authService.jwtLogin(jwtRequest));
 *     }
 *
 *     @PostMapping("/jwt/logout")
 *     public ResponseEntity<Void> jwtLogout(@RequestHeader("Authorization") String token) {
 *         authService.jwtLogout(token);
 *         return ResponseEntity.ok().build();
 *     }
 *
 *     @PostMapping("/oauth2/login")
 *     public ResponseEntity<OAuth2Response> oauth2Login(@RequestBody OAuth2Request oauth2Request) {
 *         return ResponseEntity.ok(authService.oauth2Login(oauth2Request));
 *     }
 *
 *     @PostMapping("/oauth2/logout")
 *     public ResponseEntity<Void> oauth2Logout(@RequestHeader("Authorization") String token) {
 *         authService.oauth2Logout(token);
 *         return ResponseEntity.ok().build();
 *     }
 * }
 */