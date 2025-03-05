package com.app.api.test.controller;

import com.app.api.login.AuthService;
import com.app.api.login.jwt.dto.LoginRequest;
import com.app.api.login.jwt.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/protected")
    public String protectedApi(){
        return "✅ 인증된 사용자만 접근할 수 있습니다!";
    }
    @GetMapping("/public")
    public String publicEndpoint() {
        return "✅ 누구나 접근 가능";
    }

    @GetMapping("/user")
    public String userEndpoint(Authentication authentication) {
        return "🔹 사용자 전용 API (접근 가능: " + authentication.getAuthorities() + ")";
    }

    @GetMapping("/admin")
    public String adminEndpoint(Authentication authentication) {
        return "🚨 관리자 전용 API (접근 가능: " + authentication.getAuthorities() + ")";
    }
}