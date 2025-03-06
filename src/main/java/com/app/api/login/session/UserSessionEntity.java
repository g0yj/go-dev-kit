package com.app.api.login.session;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_sessions")
public class UserSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sessionId;  // 세션 ID (중복 로그인 방지용)

    @Column(unique = true, nullable = false)
    private String username;  // 사용자 이름 (한 계정당 하나의 세션 유지)

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 로그인 시간

    @Column(nullable = false)
    private LocalDateTime lastAccessed;  // 마지막 접속 시간

    // 세션 갱신 시 마지막 접속 시간 업데이트
    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }
}
