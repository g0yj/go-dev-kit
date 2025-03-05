package com.app.api.test.repository;

import com.app.api.test.entity.ConsultationFileEntity;
import com.app.api.test.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long>,
        QuerydslPredicateExecutor<ConsultationFileEntity> {

    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username); // ✅ 추가
}
