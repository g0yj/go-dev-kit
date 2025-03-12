package com.app.api.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "notice_files")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoticeFileEntity extends BaseEntity {

    @Id
    @Column(updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String filePath; // 파일이 저장된 서버 내 경로
    String fileName; //파일명
    String originalFileName; // 업로드한 사용자의 원본 파일명
    String fileUrl; // 클라이언트가 다운로드할 수 있는 url

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    NoticeEntity noticeEntity;
}
