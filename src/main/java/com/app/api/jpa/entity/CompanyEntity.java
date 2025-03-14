package com.app.api.jpa.entity;

import com.app.api.login.UserType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "company")
public class CompanyEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String name;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    String address;

    @Column(nullable = false, unique = true)
    String businessNumber; // 사업자 번호 (고유 값)

    @OneToMany(mappedBy = "companyEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PaymentEntity> payments;
}
