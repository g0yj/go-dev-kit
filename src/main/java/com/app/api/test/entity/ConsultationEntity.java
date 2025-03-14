package com.app.api.test.entity;

import com.app.api.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultation")
@Getter@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultationEntity extends BaseEntity {

    @Id
    @Column(updatable = false)
    Long id;

    String name;
    String phone;
    String content;

    @ToString.Exclude
    @OneToMany(mappedBy = "consultationEntity", cascade = CascadeType.PERSIST, orphanRemoval = false, fetch = FetchType.LAZY)
    List<ConsultationFileEntity> consultationFileEntities = new ArrayList<>();
}
