package com.app.api.jpa.repository;

import com.app.api.jpa.entity.BatchJobLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobLogRepository extends JpaRepository<BatchJobLogEntity, Long>,
        QuerydslPredicateExecutor<BatchJobLogEntity> {

}
