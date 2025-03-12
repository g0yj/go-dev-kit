package com.app.api.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notices")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoticeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String title;  // 제목

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;  // 내용

    @Column(nullable = false)
    int views = 0;  // 조회수

    String fileName;
    String originalFileName;

    @Column(nullable = false)
    boolean isPinned = false;  // 상단 고정 여부


    @OneToMany(mappedBy = "noticeEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    List<NoticeFileEntity> noticeFileEntities = new ArrayList<>(); // 첨부파일 리스트
}

