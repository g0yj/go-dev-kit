package com.app.api.batch.reader;

import com.app.api.jpa.entity.PaymentEntity;
import com.app.api.jpa.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ✅ ItemReader는 DB에서 데이터를 읽어오는 역할
 * ✅ Step에서 reader() 메서드를 통해 연결
 */
@Slf4j
@Component
public class PaymentItemReader implements ItemReader<PaymentEntity> {
    private final PaymentRepository paymentRepository;
    private Iterator<PaymentEntity> paymentIterator; // ✅ Iterator를 사용하여 순차적으로 데이터 제공

    public PaymentItemReader(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentEntity read() {
        try {
            // ✅ 첫 번째 호출 시 DB에서 데이터를 가져와 Iterator 초기화
            if (paymentIterator == null) {
                log.info("🚀 [PaymentItemReader] 결제 데이터를 DB에서 조회합니다.");
                LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
                List<PaymentEntity> payments = paymentRepository.findByCreatedOnAfter(lastMonth);
                paymentIterator = payments.iterator();
                log.info("✅ [PaymentItemReader] {}개의 데이터를 찾았습니다.", payments.size());
            }

            // ✅ 모든 데이터를 다 읽었으면 `null` 반환 (배치 종료)
            if (paymentIterator.hasNext()) {
                PaymentEntity payment = paymentIterator.next();
                log.info("👉 [PaymentItemReader] 데이터 반환: ID={}, 금액={}", payment.getId(), payment.getAmount());
                return payment;
            } else {
                log.info("🚀 [PaymentItemReader] 모든 데이터를 처리했습니다. (배치 종료)");
                return null; // ✅ 배치 종료 신호
            }

        } catch (Exception e) {
            log.error("❌ [PaymentItemReader] 데이터 읽기 중 오류 발생: {}", e.getMessage());
            return null; // ✅ 오류 발생 시 배치 중단 방지
        }
    }
}

/**
 * 1️⃣ throws가 자동 추가된 이유는 스프링 배치의 ItemReader 기본 인터페이스가 예외를 던지도록 설계되었기 때문
 * 2️⃣ 하지만 배치가 예외로 중단되지 않게 하려면 throws를 제거하고 try-catch로 처리하는 게 좋음.
 * 3️⃣ 배치가 멈춰야 하는 중요한 예외는 throws를 사용하고, 오류를 무시할 경우 try-catch를 활용하면 됨.
 */