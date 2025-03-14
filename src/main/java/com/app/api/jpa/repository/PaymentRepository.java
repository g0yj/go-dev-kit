package com.app.api.jpa.repository;

import com.app.api.jpa.entity.PaymentEntity;
import com.app.api.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long>,
        QuerydslPredicateExecutor<PaymentEntity> {

    /**
     * 📌 특정 날짜 이후에 생성된 결제 데이터를 조회
     * - `createdOn` 필드를 기준으로 조회 (JPA에서 자동 변환)
     */
    List<PaymentEntity> findByCreatedOnAfter(LocalDateTime createdOn);

}
