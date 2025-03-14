package com.app.api.batch.processor;

import com.app.api.payment.PaymentType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentDTO {
    Long id;
    PaymentType paymentType;
    Double amount;
    Double outstandingAmount;
    String description;
    String companyName; // ✅ `CompanyEntity`에서 가져온 데이터
    String userName; // ✅ `UserEntity`에서 가져온 데이터
    LocalDateTime payDate; // ✅ 날짜를 `yyyy-MM-dd HH:mm` 형식으로 변환

}
