package com.app.api.jpa.entity;

import com.app.api.payment.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "payment")
public class PaymentEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    PaymentType paymentType;

    @Column(nullable = false)
    Double amount; // 결제 금액

    @Column(nullable = false)
    Double outstandingAmount; // 미수금

    @Column(nullable = false, length = 255)
    String description; // 결제 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    CompanyEntity companyEntity; // 결제와 연관된 회사

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity userEntity; // 결제와 연관된 사용자
}
