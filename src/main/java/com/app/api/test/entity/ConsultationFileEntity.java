package com.app.api.test.entity;

import com.app.api.test.entity.ConsultationEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "consultation_file")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultationFileEntity {
    @Id
    @Column(updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String file;
    String originalFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultationId", nullable = false)
    ConsultationEntity consultationEntity;
}
