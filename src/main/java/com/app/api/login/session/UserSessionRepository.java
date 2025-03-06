package com.app.api.login.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
    Optional<UserSessionEntity> findByUsername(String username);
    Optional<UserSessionEntity> findBySessionId(String sessionId);
    void deleteByUsername(String username); // 중복 로그인 방지를 위해 기존 세션 삭제
}
