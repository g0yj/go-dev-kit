package com.app.api.jpa.entity;

import com.app.api.login.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String username;

    private String password; // OAuth2 사용자는 null 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type; // 사용자 역할 추가

    @Column(nullable = false)
    private boolean isApproved; // ✅ 회원가입 승인 여부


    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentEntity> payments;

    /**
     * 회원가입 시 사용할 생성자 (로그인/로그아웃 테스트를 위해 추가)
     */
    public UserEntity(String username, String password, UserType type, boolean isApproved) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.isApproved = isApproved;
    }

}
