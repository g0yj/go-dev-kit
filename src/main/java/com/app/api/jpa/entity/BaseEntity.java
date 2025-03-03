package com.app.api.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter@Setter@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseEntity {
    @Column(updatable = false)
    String createdBy;

    @Column(updatable = false)
    @CreatedDate
    LocalDateTime createdOn;

    String modifiedBy;

    @LastModifiedDate
    LocalDateTime modifiedOn;

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        this.modifiedBy = createdBy;
    }
}
