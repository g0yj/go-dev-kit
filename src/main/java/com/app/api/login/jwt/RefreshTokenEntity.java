package com.app.api.login.jwt;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // ✅ 해당 토큰을 소유한 사용자

    @Column(nullable = false, unique = true)
    private String refreshToken; // ✅ Refresh Token 값

    @Column(nullable = false)
    private LocalDateTime issuedAt; // ✅ 발급 시간

    @Column(nullable = false)
    private LocalDateTime expiresAt; // ✅ 만료 시간

    public void updateRefreshToken(String newToken, LocalDateTime newExpiry) {
        this.refreshToken = newToken;
        this.expiresAt = newExpiry;
    }

    public RefreshTokenEntity(String username, String refreshToken, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.username = username;
        this.refreshToken = refreshToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
