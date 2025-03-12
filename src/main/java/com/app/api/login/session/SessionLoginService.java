package com.app.api.login.session;

import com.app.api.login.UserType;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j // ✅ 로깅 추가
@Service
@RequiredArgsConstructor
public class SessionLoginService {
/*
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository userSessionRepository;
    private final AuthenticationManager authenticationManager;

    // 🔹 회원가입 처리
    public SessionResponse registerUser(SessionRequest request) {
        log.info("📝 회원가입 요청 - username: {}", request.getUsername());

        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity user = new UserEntity(request.getUsername(), encryptedPassword, request.getType(), true);
        userRepository.save(user);

        log.info("✅ 회원가입 완료 - username: {}", user.getUsername());
        return new SessionResponse(user.getUsername(), user.getType());
    }

    // 🔹 로그인 처리 (중복 로그인 방지 적용)
    public Optional<SessionResponse> login(SessionRequest request, HttpServletRequest httpRequest) {
        log.info("🔑 로그인 요청 - username: {}", request.getUsername());

        // 1️⃣ 기존 로그인 세션 확인
        Optional<UserSessionEntity> existingSession = userSessionRepository.findByUsername(request.getUsername());
        if (existingSession.isPresent()) {
            log.warn("⚠️ 중복 로그인 시도 - username: {}", request.getUsername());
            return Optional.empty(); // ❌ 중복 로그인 방지
        }

        // 2️⃣ 사용자 인증
        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                String sessionId = httpRequest.getSession().getId(); // ✅ 세션 ID 저장

                // 3️⃣ 새 로그인 세션 저장
                UserSessionEntity newSession = new UserSessionEntity();
                newSession.setUsername(user.getUsername());
                newSession.setSessionId(sessionId);
                newSession.setCreatedAt(LocalDateTime.now());
                userSessionRepository.save(newSession);

                log.info("🚀 로그인 성공 - username: {}", user.getUsername());
                return Optional.of(new SessionResponse(user.getUsername(), user.getType()));
            } else {
                log.warn("⚠️ 로그인 실패 - 잘못된 비밀번호: {}", request.getUsername());
            }
        } else {
            log.warn("❌ 로그인 실패 - 존재하지 않는 사용자: {}", request.getUsername());
        }

        return Optional.empty();
    }

    // 🔹 로그아웃 처리 (세션 테이블에서 삭제)
    public void logout(String username) {
        log.info("🔒 로그아웃 요청 - username: {}", username);
        userSessionRepository.deleteByUsername(username); // ✅ 기존 세션 삭제
        log.info("✅ 로그아웃 완료 - username: {}", username);
    }

    // 🔹 Spring Security 기반 로그인 처리
    public ResponseEntity<?> securityLogin(SessionRequest request) {
        log.info("🔑 Spring Security 로그인 요청 - username: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("🚀 Spring Security 로그인 성공 - username: {}", request.getUsername());
        return ResponseEntity.ok(new SessionResponse(request.getUsername(), request.getType()));
    }

    // 🔹 Spring Security 기반 로그아웃 처리
    public ResponseEntity<?> securityLogout() {
        log.info("🔒 Spring Security 로그아웃 요청");
        SecurityContextHolder.clearContext();
        log.info("✅ Spring Security 로그아웃 완료");
        return ResponseEntity.ok("✅ 로그아웃 완료");
    }
*/

}
