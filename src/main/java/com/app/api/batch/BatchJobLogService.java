package com.app.api.batch;

import com.app.api.jpa.entity.BatchJobLogEntity;
import com.app.api.jpa.repository.BatchJobLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobLogService  {
    private final BatchJobLogRepository batchJobLogRepository;

    /**
     * 📌 배치 실행 시작 로그 저장
     */
    @Transactional
    public BatchJobLogEntity startBatchLog(BatchCode batchCode) {
        BatchJobLogEntity jobLog = BatchJobLogEntity.builder()
                .batchCode(batchCode)
                .startTime(LocalDateTime.now())
                .status(BatchStatus.FAILED) // 기본적으로 FAILED로 설정, 완료되면 SUCCESS로 업데이트
                .message("배치 실행 중") // 기본 메시지
                .createdAt(LocalDateTime.now())
                .build();

        jobLog = batchJobLogRepository.save(jobLog);
        log.info("🚀 [BatchJobLogService] 배치 실행 시작 로그 저장: ID={}, BATCH_CODE={}", jobLog.getId(), batchCode);
        return jobLog;
    }

    /**
     * 📌 특정 단계에서 오류 발생 시 오류 메시지 업데이트
     */
    @Transactional
    public void updateBatchLogMessage(Long logId, String errorMessage) {
        BatchJobLogEntity batchLog = batchJobLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 배치 실행 로그를 찾을 수 없습니다. ID=" + logId));

        batchLog.setMessage(errorMessage);
        batchJobLogRepository.save(batchLog);
        log.error("❌ [BatchJobLogService] 배치 오류 업데이트: ID={}, 메시지={}", logId, errorMessage);
    }

    /**
     * 📌 배치 실행 완료 로그 업데이트
     */
    @Transactional
    public void completeBatchLog(Long logId, int insertCount, int updateCount, int deleteCount, BatchStatus status, String message) {
        BatchJobLogEntity batchLog = batchJobLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 배치 실행 로그를 찾을 수 없습니다. ID=" + logId));

        batchLog.setEndTime(LocalDateTime.now());
        batchLog.setInsertCount(insertCount);
        batchLog.setUpdateCount(updateCount);
        batchLog.setDeleteCount(deleteCount);
        batchLog.setStatus(status);
        batchLog.setMessage(message);

        if (batchLog.getStartTime() != null) {
            batchLog.setDuration(Duration.between(batchLog.getStartTime(), batchLog.getEndTime()).toMillis());
        }

        batchJobLogRepository.save(batchLog);
        log.info("✅ [BatchJobLogService] 배치 실행 완료 로그 저장: ID={}, 상태={}, 수행시간={}ms", logId, status, batchLog.getDuration());
    }


}
