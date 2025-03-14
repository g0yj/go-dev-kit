package com.app.api.batch.processor;

import com.app.api.jpa.entity.PaymentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 🚀 읽어온 데이터를 가공하는 역할
 * 🚀 예: null 값 제거, 마이너스 금액 검증 등
 */
@Slf4j
@Component
public class PaymentItemProcessor implements ItemProcessor<PaymentEntity, PaymentDTO> {
    @Override
    public PaymentDTO process(PaymentEntity payment) {
        log.info("👉 [PaymentItemProcessor] 결제 데이터 처리 중: ID={}, 금액={}", payment.getId(), payment.getAmount());

        // 변환 로직 수행 후 PaymentDTO 반환
        return PaymentDTO.builder()
                .id(payment.getId())
                .paymentType(payment.getPaymentType())
                .amount(payment.getAmount())
                .outstandingAmount(payment.getOutstandingAmount())
                .description(payment.getDescription())
                .companyName(payment.getCompanyEntity().getName())
                .userName(payment.getUserEntity().getUsername())
                .payDate(payment.getCreatedOn())
                .build();

    }
}
