package com.app.api.jpa.repository;

import com.app.api.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>,
        QuerydslPredicateExecutor<UserEntity> {

    Optional<UserEntity> findByUsername(String username);

}
