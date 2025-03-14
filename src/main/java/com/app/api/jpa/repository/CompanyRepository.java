package com.app.api.jpa.repository;

import com.app.api.jpa.entity.CompanyEntity;
import com.app.api.jpa.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long>,
        QuerydslPredicateExecutor<CompanyEntity> {
}
