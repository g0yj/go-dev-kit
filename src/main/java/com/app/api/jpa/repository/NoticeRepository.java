package com.app.api.jpa.repository;

import com.app.api.jpa.entity.NoticeEntity;
import com.app.api.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, Long>,
        QuerydslPredicateExecutor<NoticeEntity> {

}
