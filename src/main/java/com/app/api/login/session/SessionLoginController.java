package com.app.api.login.session;


import com.app.api.login.UserType;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
class SessionLoginController {
/*
    private final SessionLoginService sessionLoginService;

    @PostMapping("/login")
    public ResponseEntity<?> apiLogin(@RequestBody SessionRequest request, HttpServletRequest httpRequest) {
        log.info("🔑 로그인 API 호출 - username: {}", request.getUsername());

        Optional<SessionResponse> response = sessionLoginService.login(request, httpRequest);

        return response.map(res -> {
            log.info("🚀 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(res);
        }).orElseGet(() -> {
            log.warn("⚠️ 로그인 실패 - username: {}", request.getUsername());
            return  ResponseEntity.badRequest().body(new SessionResponse("로그인 실패", UserType.C));
        });
    }

    @PostMapping("/register")
    public ResponseEntity<?> apiRegister(@RequestBody SessionRequest request) {
        log.info("📝 회원가입 API 호출 - username: {}", request.getUsername());

        SessionResponse response = sessionLoginService.registerUser(request);
        log.info("✅ 회원가입 완료 - username: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> apiLogout(@RequestBody SessionRequest request) {
        log.info("🔒 로그아웃 API 호출 - username: {}", request.getUsername());
        sessionLoginService.logout(request.getUsername());
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }

    @PostMapping("/security/login")
    public ResponseEntity<?> securityLogin(@RequestBody SessionRequest request) {
        return sessionLoginService.securityLogin(request);
    }

    @PostMapping("/security/logout")
    public ResponseEntity<?> securityLogout() {
        return sessionLoginService.securityLogout();
    }
*/

}
