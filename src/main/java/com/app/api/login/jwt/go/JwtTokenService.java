package com.app.api.login.jwt.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.RefreshTokenEntity;
import com.app.api.login.jwt.RefreshTokenRepository;
import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.jpa.entity.UserEntity;
import com.app.api.jpa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Map;
import java.util.Optional;

/**
 * 토큰 발급 및 인증 로직 담당
 */
@Service
@Slf4j
public class JwtTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Lazy // 순환참조 방지
    private final AuthenticationManager authenticationManager;

    private final JwtSecurityService jwtSecurityService;

    public JwtTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager, JwtSecurityService jwtSecurityService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.jwtSecurityService = jwtSecurityService;
    }

    /** ✅  Access Token만 사용
     *  - 자동 로그아웃 (추가 로그인 필요) , 보안성이 높음
     *  - 토큰 만료 기간이 짧을 경우 사용자가 자주 로그인 해야함 -> 트래픽 증가
     */

    public JwtTokenResponse login(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("⚠️ 로그인 실패 - 비밀번호 불일치");
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // ✅ AccessToken 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());

        log.info("✅ 로그인 성공 - '{}' Access Token 발급 완료", request.getUsername());
        return new JwtTokenResponse(user.getUsername(), accessToken, null, user.getType(), null);
    }


    /** ✅ 로그아웃  (프론트에서 Access Token 삭제)*/
    public void logout(String username) {
        log.info("🔒 사용자 '{}' 로그아웃 처리", username);
        // 로그아웃 시 JWT 무효화할 방법이 필요 (예: 블랙리스트 DB 활용)
    }

    /**
     * ✅ Refresh Token을 사용한 로그인
     *  - 사용자가 직접 로그인 (비밀번호 입력)
     *  - 사용자가 로그인할 때 기존 Refresh Token을 삭제하고, 새 Refresh Token을 발급해 갱신(불필요한 Refresh Token이 DB에 남아있지 않음)
     *  - 프론트
     *      - Access Token은 localStorage 또는 sessionStorage에 저장
     *      - Refresh Token은 HTTP-Only Secure Cookie로 저장하는 것이 가장 안전한 방식
     *      -Access Token이 만료되었을 때 → Refresh Token을 이용해 새로운 Access Token을 요청. (Refresh Token은 HttpOnly Cookie에 저장했으므로 프론트에서는 직접 접근할 수 없고, 자동으로 쿠키를 포함하여 요청)
     *      -Refresh Token을 전달하지 않으면 서버에서 Refresh Token 삭제해야됨( 배치 등 .. )
     */
    public JwtTokenResponse refreshLogin(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        refreshTokenRepository.findByUsername(user.getUsername()).ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(new RefreshTokenEntity(null, user.getUsername(), refreshToken, LocalDateTime.now(), jwtTokenProvider.getRefreshTokenExpiry()));

        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType(), null);
    }

    /** ✅ Refresh Token을 사용한 로그아웃 (Refresh Token 삭제) */
    public void refreshLogout(String refreshToken) {
        refreshTokenRepository.findByRefreshToken(refreshToken).ifPresent(refreshTokenRepository::delete);
    }

    /** ✅ Refresh Token을 사용하여 새로운 Access Token 발급
     *  - 이미 로그인된 사용자의 토큰을 재발급하는 방식 (자동 로그인 유지)
     * */
    @Transactional
    public Map<String, String> refreshAccessToken(String refreshToken) {
        log.info("🔄 Refresh Token을 사용하여 Access Token 재발급 요청");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ✅ 기존 Refresh Token 삭제
        refreshTokenRepository.deleteByUsername(username);

        // ✅ 새 Refresh Token 발급 및 저장
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpiry();
        refreshTokenRepository.save(new RefreshTokenEntity(username, newRefreshToken, LocalDateTime.now(), expiresAt));

        log.info("✅ 새로운 Access Token 및 Refresh Token 발급 완료");

        return Map.of("accessToken", jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType()),
                "refreshToken", newRefreshToken);
    }

    /**
     * ✅ Refresh Token을 사용한 로그아웃 (DB에서 삭제)
     */
    @Transactional
    public void logoutWithRefreshToken(String refreshToken) {
        log.info("🔒 Refresh Token 로그아웃 요청 - Refresh-Token: {}", refreshToken);

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("⚠️ 로그아웃 실패 - Refresh Token이 없습니다.");
            throw new IllegalArgumentException("로그아웃 실패 - Refresh Token이 없습니다.");
        }

        // ✅ Refresh Token을 DB에서 삭제
        refreshTokenRepository.findByRefreshToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);

        log.info("✅ Refresh Token 삭제 완료, 로그아웃 성공");
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

    //=================== Spring Security ==========================================================

    /**  ✅ JWT 로그인 (토큰 발급 및 SecurityContext 저장) */
    @Transactional
    public JwtTokenResponse securityLogin(JwtTokenRequest request) {
        log.info("🔑 사용자 '{}' 로그인 시도", request.getUsername());

        // ✅ 사용자 조회 (없으면 예외 발생)
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ✅ AccessToken & RefreshToken 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.info("✅ AccessToken 생성 완료: {}", accessToken);
        log.info("✅ RefreshToken 생성 완료: {}", refreshToken);

        // ✅ SecurityContext에 인증 정보 저장 (Spring Security 로그인 처리)
        jwtSecurityService.authenticateUser(accessToken);

        // ✅ 기존 Refresh Token 삭제 후 새로 저장 (업데이트 방식 적용)
        refreshTokenRepository.findByUsername(user.getUsername())
                .ifPresentOrElse(
                        existingToken -> {
                            existingToken.updateRefreshToken(refreshToken, jwtTokenProvider.getRefreshTokenExpiry());
                            refreshTokenRepository.save(existingToken);
                            log.debug("🔄 기존 Refresh Token 업데이트 완료");
                        },
                        () -> {
                            refreshTokenRepository.save(new RefreshTokenEntity(null, user.getUsername(), refreshToken, LocalDateTime.now(), jwtTokenProvider.getRefreshTokenExpiry()));
                            log.debug("✅ 새로운 Refresh Token 저장 완료");
                        }
                );

        // ✅ Role에 따라 redirect URL 설정
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
/*
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
*/


    /** ✅ Spring Security 로그아웃 */
    @Transactional
    /** ✅ 로그아웃 */
    public ResponseEntity<?> securityLogout(HttpServletRequest request) {
        return jwtSecurityService.securityLogout(request);
    }
/*
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
*/

    /** ✅ Refresh Token을 사용하여 새로운 Access Token 발급 */
    public Map<String, String> securityRefresh(String refreshToken) {
        log.info("🔄 Refresh Token을 사용하여 Access Token 재발급 요청");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ✅ 기존 Refresh Token 삭제 후 새로 발급
        refreshTokenRepository.deleteByUsername(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        refreshTokenRepository.save(new RefreshTokenEntity(username, newRefreshToken, LocalDateTime.now(), jwtTokenProvider.getRefreshTokenExpiry()));

        // ✅ SecurityContext에 인증 정보 저장 (새로운 AccessToken으로 업데이트)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType());
        jwtSecurityService.authenticateUser(newAccessToken);

        log.info("✅ 새로운 Access Token 및 Refresh Token 발급 완료");

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);
    }

}
