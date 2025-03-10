package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.RefreshTokenEntity;
import com.app.api.login.jwt.RefreshTokenRepository;
import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /** ✅ JWT 로그인 (토큰 발급) */
    public JwtTokenResponse login(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("⚠️ 로그인 실패 - '{}' 사용자 없음", request.getUsername());
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("⚠️ 로그인 실패 - '{}' 비밀번호 불일치", request.getUsername());
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // ✅ 사용자의 역할 조회 (ROLE_USER, ROLE_ADMIN 등)
        String role = (user.getType() != null) ? user.getType().name() : "ROLE_C";

        // ✅ JWT 생성 (role 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), role);

        log.info("✅ 로그인 성공 - '{}' 토큰 발급 완료", request.getUsername());

        return new JwtTokenResponse(user.getUsername(), accessToken, null, UserType.C);
    }

    /** ✅ 회원가입 */
    public JwtTokenResponse register(JwtTokenRequest request) {
        log.info("📝 회원가입 시도 - username: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("⚠️ 회원가입 실패 - '{}' 이미 존재하는 사용자", request.getUsername());
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity user = new UserEntity(request.getUsername(), encodedPassword, UserType.C , true);
        userRepository.save(user);

        // ✅ 사용자의 역할 조회 (ROLE_USER, ROLE_ADMIN 등)
        String role = (user.getType() != null) ? user.getType().name() : "ROLE_C";

        // ✅ JWT 생성 (role 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), role);
        log.info("✅ 회원가입 완료 - '{}' 토큰 발급 완료", request.getUsername());

        return new JwtTokenResponse(user.getUsername(), accessToken, null, user.getType());
    }

    /** ✅ 로그아웃 */
    public void logout(String username) {
        log.info("🔒 사용자 '{}' 로그아웃 처리", username);
        // 로그아웃 시 JWT 무효화할 방법이 필요 (예: 블랙리스트 DB 활용)
    }

    /** ✅ 로그인 */
    public JwtTokenResponse refreshLogin(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        // ✅ 사용자의 역할 조회 (ROLE_USER, ROLE_ADMIN 등)
        String role = (user.getType() != null) ? user.getType().name() : "ROLE_C";

        // ✅ JWT 생성 (role 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiry(); // ✅ 만료 시간 설정

        // ✅ 기존 리프레시 토큰 삭제 후 새로 저장
        refreshTokenRepository.findByUsername(user.getUsername()).ifPresent(refreshTokenRepository::delete);

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .username(user.getUsername())
                .refreshToken(refreshToken)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt) // ✅ JwtTokenProvider에서 가져온 만료 시간 적용
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("✅ 로그인 성공 - '{}' 액세스 & 리프레시 토큰 발급 완료", request.getUsername());
        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType());
    }

    /** ✅ 로그아웃 (리프레시 토큰 삭제) */
    @Transactional
    public void refreshLogout(String username) {
        log.info("🔒 사용자 '{}' 로그아웃 처리 - 리프레시 토큰 삭제", username);
        refreshTokenRepository.deleteByUsername(username);
    }


    /** ✅ Spring Security 로그인 */
    public ResponseEntity<?> securityLogin(SessionRequest request) {
        log.info("🔐 Spring Security 로그인 시도 - username: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("✅ Spring Security 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok("✅ Spring Security 로그인 성공");
        } catch (Exception e) {
            log.warn("⚠️ Spring Security 로그인 실패 - username: {}", request.getUsername());
            return ResponseEntity.badRequest().body("로그인 실패");
        }
    }

    /** ✅ Spring Security 로그아웃 */
    public ResponseEntity<?> securityLogout() {
        log.info("🔓 Spring Security 로그아웃 요청");
        SecurityContextHolder.clearContext();
        log.info("✅ Spring Security 로그아웃 완료");
        return ResponseEntity.ok("✅ Spring Security 로그아웃 완료");
    }
}
