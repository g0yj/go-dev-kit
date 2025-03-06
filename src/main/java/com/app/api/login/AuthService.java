package com.app.api.login;


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

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 *  인증 및 로그인 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

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

}
