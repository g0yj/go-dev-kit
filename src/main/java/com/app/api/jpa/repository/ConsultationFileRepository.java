package com.app.api.jpa.repository;

import com.app.api.jpa.entity.ConsultationFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;


public interface ConsultationFileRepository extends JpaRepository<ConsultationFileEntity, Long>,
        QuerydslPredicateExecutor<ConsultationFileEntity> {

}
