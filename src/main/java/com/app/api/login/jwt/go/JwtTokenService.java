package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.RefreshTokenEntity;
import com.app.api.login.jwt.RefreshTokenRepository;
import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class JwtTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Lazy // 순환참조 방지
    private final AuthenticationManager authenticationManager;

    public JwtTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

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


        // ✅ JWT 생성 (role 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());

        log.info("✅ 로그인 성공 - '{}' 토큰 발급 완료", request.getUsername());

        return new JwtTokenResponse(user.getUsername(), accessToken, null, user.getType(), null);
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
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
        log.info("✅ 회원가입 완료 - '{}' 토큰 발급 완료", request.getUsername());

        return new JwtTokenResponse(user.getUsername(), accessToken, null, user.getType(), null);
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

        // ✅ JWT 생성 (role 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
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
        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType(), null);
    }

    /** ✅ 로그아웃 (리프레시 토큰 삭제) */
    @Transactional
    public void refreshLogout(String username) {
        log.info("🔒 사용자 '{}' 로그아웃 처리 - 리프레시 토큰 삭제", username);
        refreshTokenRepository.deleteByUsername(username);
    }

    /** ✅ Spring Security 로그인 */
    @Transactional
    public JwtTokenResponse securityLogin(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        // ✅ 1. Spring Security AuthenticationManager를 사용하여 인증 수행 (비밀번호 검증)
        log.debug("🔑 비밀번호 검증 시작");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        log.debug("🔑 인증 성공, 인증 정보: {}", authentication);

        // ✅ 2. 인증된 사용자 정보를 UserRepository에서 조회 (DB에서 최신 type 값 가져오기)
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("❌ 사용자 '{}'를 찾을 수 없습니다.", request.getUsername());
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        log.debug("🔍 UserEntity에서 가져온 사용자 정보: username={}, type={}", user.getUsername(), user.getType());

        // ✅ 3. type이 null이면 예외 발생
        if (user.getType() == null) {
            log.error("🚨 UserEntity의 type 값이 NULL 입니다! (username: {})", user.getUsername());
            throw new IllegalStateException("사용자 유형(type)이 설정되지 않았습니다.");
        }

        // ✅ 4. AccessToken과 RefreshToken 생성 (type 포함)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.debug("🔑 AccessToken 생성 완료: {}", accessToken);
        log.debug("🔑 RefreshToken 생성 완료: {}", refreshToken);

        // ✅ 5. SecurityContext에 사용자 정보 저장 (UserDetails 생성)
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getType().name()) // "ROLE_" 접두사 추가
        );
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.debug("✅ SecurityContext에 인증 정보 저장됨: {}", authenticationToken);

        // ✅ 6. RefreshToken을 DB에 저장 또는 업데이트 (있으면 갱신, 없으면 새로 저장)
        refreshTokenRepository.findByUsername(user.getUsername())
                .ifPresentOrElse(
                        existingToken -> {
                            log.debug("🔑 기존 refreshToken 발견, 갱신 진행");
                            existingToken.updateRefreshToken(refreshToken, jwtTokenProvider.getRefreshTokenExpiry());
                            refreshTokenRepository.save(existingToken);
                            log.debug("🔑 refreshToken 갱신 완료");
                        },
                        () -> {
                            log.debug("🔑 새 refreshToken 저장 진행");
                            RefreshTokenEntity newToken = RefreshTokenEntity.builder()
                                    .username(user.getUsername())
                                    .refreshToken(refreshToken)
                                    .issuedAt(LocalDateTime.now())
                                    .expiresAt(jwtTokenProvider.getRefreshTokenExpiry())
                                    .build();
                            refreshTokenRepository.save(newToken);
                            log.debug("🔑 새 refreshToken 저장 완료");
                        }
                );
        // ✅ 7. Role에 따라 redirect URL 설정
        String redirectUrl;
        switch (user.getType().name()) {
            case "A":
                redirectUrl = "/admin/main";
                break;
            case "C":
                redirectUrl = "/user/main";
                break;
            default:
                redirectUrl = "/access-denied"; // 기본 접근 제한 페이지
        }

        log.info("✅ 사용자 '{}' 로그인 성공, 이동할 페이지: {}", user.getUsername(), redirectUrl);

        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType(), redirectUrl);
    }



/** ✅ Spring Security 로그아웃 */

    @Transactional
    public ResponseEntity<?> securityLogout(HttpServletRequest request) {
        log.info("🔓 Spring Security 로그아웃 API 호출");

        // 1️⃣ Access Token & Refresh Token 요청 헤더에서 추출
        String accessToken = jwtTokenProvider.extractToken(request, "Authorization");
        String refreshToken = jwtTokenProvider.extractToken(request, "Refresh-Token");

        if (accessToken == null) {
            log.warn("❌ 로그아웃 요청에 Access Token이 없음");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("로그아웃 실패: Access Token이 필요합니다.");
        }
        if (refreshToken == null) {
            log.warn("❌ 로그아웃 요청에 Refresh Token이 없음");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("로그아웃 실패: Refresh Token이 필요합니다.");
        }

        log.debug("🔑 Access Token 확인: {}", accessToken);
        log.debug("🔑 Refresh Token 확인: {}", refreshToken);

        // 2️⃣ Access Token & Refresh Token 검증
        boolean isAccessTokenValid = jwtTokenProvider.validateToken(accessToken);
        boolean isRefreshTokenValid = jwtTokenProvider.validateToken(refreshToken);

        if (!isAccessTokenValid) {
            log.warn("❌ 유효하지 않은 Access Token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그아웃 실패: 유효하지 않은 Access Token입니다.");
        }
        if (!isRefreshTokenValid) {
            log.warn("❌ 유효하지 않은 Refresh Token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그아웃 실패: 유효하지 않은 Refresh Token입니다.");
        }

        // 3️⃣ Access Token에서 사용자 정보 추출
        String username = jwtTokenProvider.getUsernameFromToken(accessToken);
        log.info("🔑 사용자 '{}' 로그아웃 처리 시작", username);

        if (username == null) {
            log.warn("❌ Access Token에서 사용자 정보를 찾을 수 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그아웃 실패: 올바른 Access Token이 아닙니다.");
        }

        // 4️⃣ Refresh Token 삭제 (DB에서 제거)
        Optional<RefreshTokenEntity> refreshTokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (refreshTokenOpt.isPresent()) {
            refreshTokenRepository.delete(refreshTokenOpt.get());
            log.info("✅ 사용자 '{}'의 Refresh Token 삭제 완료", username);
        } else {
            log.warn("🔑 Refresh Token이 존재하지 않음");
        }

        // 5️⃣ SecurityContext 초기화
        SecurityContextHolder.clearContext();
        log.debug("🔑 SecurityContext 클리어됨");

        log.info("✅ Spring Security 로그아웃 완료");
        return ResponseEntity.ok("로그아웃 성공");
    }

}
