package com.app.api.test.repository;

import com.app.api.test.entity.ConsultationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long>,
        QuerydslPredicateExecutor<ConsultationEntity> {

}
