package com.app.api.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 📌 배치 작업을 실행하기 위한 Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfig {
    private final JobRepository jobRepository;
    private final StepConfig stepConfig;

    @Bean
    public Job paymentJob(JobExecutionListener jobExecutionListener) {
        log.info("🚀 [JobConfig] 결제 데이터 배치 Job 초기화");

        return new JobBuilder("paymentJob", jobRepository)
                .listener(jobExecutionListener)
                .start(stepConfig.paymentStep()) // ✅ Step이 등록되었는지 확인!
                .build();
    }
}
