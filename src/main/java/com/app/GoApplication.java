package com.app;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling // ✅ 스케줄링 활성화
@EnableBatchProcessing // ✅ 배치 기능 활성화
//@EnableJpaRepositories
public class GoApplication {

  public static void main(String[] args) {
    SpringApplication.run(GoApplication.class, args);
  }

}
