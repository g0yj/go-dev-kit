package com.app.api.login;


import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.jwt.RefreshTokenEntity;
import com.app.api.login.jwt.RefreshTokenRepository;
import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.login.session.SessionSecurityService;
import com.app.api.login.session.UserSessionEntity;
import com.app.api.login.session.UserSessionRepository;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 *  인증 및 로그인 관리
 */
@Service
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final SessionSecurityService sessionSecurityService; // ✅ UserDetailsService 역할 수행
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository; // ✅ RefreshToken 관리 추가

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            SessionSecurityService sessionSecurityService,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository) { // ✅ 추가됨
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.sessionSecurityService = sessionSecurityService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository; // ✅ 주입 추가
    }

    // ============================== ✅ [1] 세션 로그인 (Spring Security 미사용) ==============================
    /**
     *1️⃣ Spring Security를 사용하지 않는 경우
     * (Spring Security 미적용)
     *
     * 🔹 특징
     * 세션을 직접 관리해야 함 (HttpSession 객체 활용)
     * 사용자 인증을 직접 구현해야 함 (user_sessions 테이블 관리 필요)
     * 권한 및 접근 제어도 수동으로 처리해야 함
     * 🔹 로그인 흐름
     * 클라이언트가 /session/login 요청 (ID/PW 전송)
     * AuthService에서 직접 사용자 인증 (UserRepository)
     * 세션 생성 후, session.setAttribute()로 정보 저장
     * 응답 반환 (로그인 성공 시 SessionResponse 반환)
     * 사용자는 이후 요청에서 JSESSIONID 쿠키를 자동으로 전송
     * 로그아웃 시 session.invalidate()로 세션 삭제
     * DB에서도 user_sessions에서 삭제
     */

    public SessionResponse sessionLogin(HttpServletRequest request, SessionRequest sessionRequest) {

        // 사용자 조회
        UserEntity user = userRepository.findByUsername(sessionRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 세션 삭제 (중복 로그인 방지)
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            log.info("기존 세션 삭제 - username: {}", user.getUsername());
            oldSession.invalidate();
        }

        // 새로운 세션 생성 및 저장
        HttpSession session = request.getSession(true);
        String sessionId = session.getId(); // 세션 ID 가져오기
        session.setAttribute("username", user.getUsername());

        log.info("새로운 세션 생성 - sessionId: {}, username: {}", sessionId, user.getUsername());

        // 기존 `user_sessions` 테이블에서 해당 사용자의 세션 삭제 (중복 로그인 방지)
        userSessionRepository.findByUsername(user.getUsername()).ifPresent(existingSession -> {
            log.info("기존 user_sessions 테이블의 세션 삭제 - sessionId: {}, username: {}", existingSession.getSessionId(), user.getUsername());
            userSessionRepository.delete(existingSession);
        });

        // 새로운 `user_sessions` 데이터 저장
        UserSessionEntity userSession = UserSessionEntity.builder()
                .sessionId(sessionId)
                .username(user.getUsername())
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .build();

        userSessionRepository.save(userSession);
        log.info("user_sessions 테이블에 저장 완료 - sessionId: {}, username: {}", sessionId, user.getUsername());

        return new SessionResponse(user.getUsername(), user.getType());
    }

    public void sessionLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            String username = (String) session.getAttribute("username");

            log.info("로그아웃 요청 - sessionId: {}, username: {}", sessionId, username);

            // `user_sessions` 테이블에서도 세션 정보 삭제
            userSessionRepository.findBySessionId(sessionId).ifPresent(userSession -> {
                log.info("user_sessions 테이블에서 세션 삭제 - sessionId: {}, username: {}", userSession.getSessionId(), username);
                userSessionRepository.delete(userSession);
            });

            session.invalidate();
        }
    }
    // ============================== ✅ [2] 세션 로그인 (Spring Security 사용) ==============================
    /**
     *Spring Security를 사용하는 경우
     * (Spring Security 적용)
     *
     * 🔹 특징
     * Spring Security에서 자동으로 세션을 관리해줌
     * 로그인 시 AuthenticationManager를 통해 인증 수행
     * 인증이 성공하면 Spring Security의 SecurityContextHolder에 정보 저장
     * 로그인 후 자동으로 JSESSIONID를 쿠키로 발급
     * @PreAuthorize, @RolesAllowed 등의 권한 기반 접근 제어 가능
     * CSRF, CORS, 인증 필터 등 보안 기능 추가 지원
     * 🔹 로그인 흐름
     * 클라이언트가 /login 요청 (ID/PW 전송)
     * Spring Security의 UsernamePasswordAuthenticationFilter가 요청을 가로챔
     * AuthenticationManager가 UserDetailsService를 사용해 사용자 인증
     * 인증 성공 시 SecurityContextHolder에 저장
     * 자동으로 세션을 생성하고 JSESSIONID 쿠키를 발급
     * 사용자는 이후 JSESSIONID 쿠키로 인증된 요청을 보냄
     * 로그아웃 시 SecurityContextLogoutHandler를 통해 자동 처리됨
     */
    public boolean sessionSecurityLogin(HttpServletRequest request, SessionRequest sessionRequest) {
        log.debug("🔹 Session Security 로그인 요청 - username: {}", sessionRequest.getUsername());

        try {
            // 1️⃣ UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(sessionRequest.getUsername(), sessionRequest.getPassword());

            // 2️⃣ 인증 수행
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3️⃣ 기존 세션 제거 후 새로운 세션 생성
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                log.debug("🔄 기존 세션 삭제 - sessionId: {}", oldSession.getId());
                oldSession.invalidate();
            }

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            newSession.setAttribute("username", sessionRequest.getUsername());

            log.info("✅ 로그인 성공 - username: {}, sessionId: {}", sessionRequest.getUsername(), newSession.getId());
            return true;

        } catch (Exception e) {
            log.error("❌ 로그인 실패 - username: {}, 이유: {}", sessionRequest.getUsername(), e.getMessage());
            return false;
        }
    }

    public void sessionSecurityLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            String username = (String) session.getAttribute("username");
            log.info("🔹 Session Security 로그아웃 요청 - username: {}", username);

            session.invalidate(); // ✅ 세션 무효화
            SecurityContextHolder.clearContext(); // ✅ SecurityContext 클리어

            log.info("✅ 로그아웃 성공 - username: {}", username);
        } else {
            log.warn("⚠️ 로그아웃 요청했지만 세션이 존재하지 않음");
        }
    }
    // ============================== ✅ [3] JWT 로그인 (Spring Security 미사용) ==============================
    /**
     * ✅ JWT 로그인 - 액세스 토큰과 리프레시 토큰 발급 후, RefreshToken 저장
     */
    @Transactional
    public JwtTokenResponse jwtLogin(JwtTokenRequest jwtRequest) {
        log.debug("🔑 JWT 로그인 요청 - username: {}", jwtRequest.getUsername());

        // 1️⃣ 사용자 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2️⃣ 사용자 정보 조회
        UserEntity user = userRepository.findByUsername(jwtRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자를 찾을 수 없습니다."));

        // 3️⃣ 액세스 토큰 및 리프레시 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 4️⃣ 발급 시간 및 만료 시간 설정
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpiration()));

        // 5️⃣ 기존 리프레시 토큰 존재 여부 확인 후 업데이트 or 새로 저장
        refreshTokenRepository.findByUsername(user.getUsername()).ifPresentOrElse(
                existingToken -> {
                    log.info("🔄 기존 RefreshToken 갱신 - username: {}", user.getUsername());
                    existingToken.updateRefreshToken(refreshToken, expiresAt);
                    refreshTokenRepository.save(existingToken);
                },
                () -> {
                    log.info("🆕 새로운 RefreshToken 저장 - username: {}", user.getUsername());
                    RefreshTokenEntity newToken = RefreshTokenEntity.builder()
                            .username(user.getUsername())
                            .refreshToken(refreshToken)
                            .issuedAt(issuedAt)
                            .expiresAt(expiresAt)
                            .build();
                    refreshTokenRepository.save(newToken);
                }
        );

        log.info("✅ JWT 로그인 성공 - username: {}, refreshToken 저장 완료", user.getUsername());
        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType());
    }
    /**
     * ✅ JWT 로그아웃 - RefreshToken 삭제
     */
    @Transactional
    public void jwtLogout(String refreshToken) {
        log.debug("🚪 JWT 로그아웃 요청");

        // 1️⃣ RefreshToken 검증 후 사용자명 가져오기
        String username = jwtTokenProvider.validateRefreshToken(refreshToken);

        // 2️⃣ DB에서 RefreshToken 삭제
        refreshTokenRepository.deleteByUsername(username);

        // 3️⃣ Spring Security 컨텍스트 초기화
        SecurityContextHolder.clearContext();

        log.info("✅ JWT 로그아웃 성공 - username: {}", username);
    }

    public boolean isLoggedOut(String refreshToken) {
        try {
            String username = jwtTokenProvider.validateRefreshToken(refreshToken);

            // ✅ DB에서 RefreshToken 존재 여부 확인
            return !refreshTokenRepository.existsByUsername(username);
        } catch (Exception e) {
            return true; // 토큰이 유효하지 않거나, 이미 삭제된 경우 로그아웃 상태로 처리
        }
    }

    // ============================== ✅ [4] JWT 로그인 (Spring Security 사용) ==============================
    public JwtTokenResponse jwtSecurityLogin(JwtTokenRequest jwtRequest) {
        log.debug("JWT Security 로그인 요청 - username: {}", jwtRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity user = userRepository.findByUsername(jwtRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getType().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.info("✅ JWT Security 로그인 성공 - username: {}", user.getUsername());
        return new JwtTokenResponse(user.getUsername(), accessToken, refreshToken, user.getType());
    }

    // ✅ JWT 로그아웃 (Spring Security 사용)
    public void jwtSecurityLogout() {
        SecurityContextHolder.clearContext();
        log.info("✅ JWT Security 로그아웃 성공");
    }

    // ============================== ✅ [5] OAuth2 로그인 (Spring Security 미사용) ==============================
    public String oauth2Login() {
        return "OAuth2 로그인 (미사용)";
    }

    // ✅ OAuth2 로그아웃 (Spring Security 미사용)
    public void oauth2Logout() {
        log.info("✅ OAuth2 로그아웃 성공");
    }

    // ============================== ✅ [6] OAuth2 로그인 (Spring Security 사용) ==============================
    public String oauth2SecurityLogin() {
        return "OAuth2 로그인 (Spring Security 사용)";
    }

    // ✅ OAuth2 로그아웃 (Spring Security 사용)
    public void oauth2SecurityLogout() {
        SecurityContextHolder.clearContext();
        log.info("✅ OAuth2 Security 로그아웃 성공");
    }

}
