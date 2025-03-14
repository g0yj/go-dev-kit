package com.app.api.jpa.entity;

import com.app.api.batch.BatchCode;
import com.app.api.batch.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BATCH_JOB_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJobLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 로그 ID

    @Column(name = "BATCH_CODE", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private BatchCode batchCode;  // 배치 코드 (잡 이름)

    @Column(name = "STATUS", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status;  // 실행 결과 (SUCCESS / FAILED)

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;  // 실행 시작 시간

    @Column(name = "END_TIME")
    private LocalDateTime endTime;  // 실행 종료 시간

    @Column(name = "DURATION")
    private Long duration;  // 수행 시간(ms)

    @Column(name = "INSERT_COUNT", columnDefinition = "INT DEFAULT 0")
    private int insertCount = 0;  // INSERT된 개수

    @Column(name = "UPDATE_COUNT", columnDefinition = "INT DEFAULT 0")
    private int updateCount = 0;  // UPDATE된 개수

    @Column(name = "DELETE_COUNT", columnDefinition = "INT DEFAULT 0")
    private int deleteCount = 0;  // DELETE된 개수

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;  // 실행 메시지

    @Column(name = "CREATED_AT", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;  // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
