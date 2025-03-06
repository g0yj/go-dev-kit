package com.app.api.login.jwt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    // ✅ username으로 RefreshToken 조회
    Optional<RefreshTokenEntity> findByUsername(String username);

    // ✅ RefreshToken 값으로 조회 (토큰 검증용)
    Optional<RefreshTokenEntity> findByRefreshToken(String refreshToken);

    // ✅ username으로 RefreshToken 삭제 (로그아웃 시 사용)
    void deleteByUsername(String username);

    boolean existsByUsername(String username);
}
