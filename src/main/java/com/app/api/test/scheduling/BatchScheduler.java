package com.app.api.test.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * 📌 배치 작업을 자동 실행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job paymentJob; // ✅ 결제 데이터 처리 배치 Job

    /**
     * 📌 매일 오후 9시 5분에 배치 작업 실행 (스케줄링 실행 테스트)
     */
    @Scheduled(cron = "0 19 01 * * ?") // 매일 21:05 실행
    public void runPaymentBatch() {
        try {
            log.info("🚀 [BatchScheduler] 결제 데이터 배치 작업 시작!");

            // ✅ 배치 실행 파라미터 설정 (중복 실행 방지)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 실행 시마다 새로운 JobInstance 생성
                    .toJobParameters();

            // ✅ 배치 실행
            JobExecution jobExecution = jobLauncher.run(paymentJob, jobParameters);

            log.info("✅ [BatchScheduler] 배치 작업 완료! 상태: {}", jobExecution.getStatus());
        } catch (Exception e) {
            log.error("❌ [BatchScheduler] 배치 실행 중 오류 발생: {}", e.getMessage());
        }
    }
}
