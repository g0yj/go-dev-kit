package com.app.api.jpa.service;

import com.app.api.file.FileService;
import com.app.api.file.dto.FileInfo;
import com.app.api.jpa.controller.dto.CreateNoticeResponse;
import com.app.api.jpa.entity.NoticeEntity;
import com.app.api.jpa.entity.NoticeFileEntity;
import com.app.api.jpa.repository.NoticeFileRepository;
import com.app.api.jpa.repository.NoticeRepository;
import com.app.api.jpa.repository.UserRepository;
import com.app.api.jpa.service.dto.CreateNotice;
import com.app.api.login.jwt.go.JwtTokenProvider;
import com.app.api.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {
    private final BoardServiceMapper boardServiceMapper;
    private final NoticeRepository noticeRepository;
    private final FileService fileService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public CreateNoticeResponse createdNotice(CreateNotice createNotice) {
        log.debug("createNotice : {}", createNotice);

        // 1️⃣ JWT 토큰 검증 및 username 추출
        if (createNotice.getCreatedBy() == null || !createNotice.getCreatedBy().startsWith("Bearer ")) {
            throw new IllegalArgumentException("잘못된 인증 정보");
        }

        String token = createNotice.getCreatedBy().substring(7);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        log.info("📌 공지사항 작성자 (JWT 기반): {}", username);

        // ✅ 공지사항 엔티티 생성
        NoticeEntity noticeEntity = new NoticeEntity();
        noticeEntity.setTitle(createNotice.getTitle());
        noticeEntity.setContent(createNotice.getContent());
        noticeEntity.setCreatedBy(username);

        // ✅ 파일 업로드 및 정보 저장
        Map<String, FileInfo> files = fileService.upload(createNotice.getMultipartFiles());

        if (ObjectUtils.isNotEmpty(files)) {
            List<NoticeFileEntity> noticeFileEntities = new ArrayList<>();

            files.forEach((originalFileName, fileInfo) -> {
                NoticeFileEntity noticeFileEntity = new NoticeFileEntity();
                noticeFileEntity.setOriginalFileName(originalFileName); // 원본 파일명
                noticeFileEntity.setFileName(fileInfo.getFileName()); // 저장된 파일명
                noticeFileEntity.setFilePath(fileInfo.getFilePath()); // 저장 경로
                noticeFileEntity.setFileUrl(fileInfo.getFileUrl()); // 다운로드 URL
                noticeFileEntity.setCreatedBy(username);
                noticeFileEntity.setNoticeEntity(noticeEntity);
                noticeFileEntities.add(noticeFileEntity);
            });

            noticeEntity.setNoticeFileEntities(noticeFileEntities);

            // ✅ 공지사항 엔티티에도 첫 번째 파일 정보를 저장
            FileInfo firstFile = files.values().iterator().next();
            noticeEntity.setOriginalFileName(firstFile.getOriginalFileName());
            noticeEntity.setFileName(firstFile.getFileName());
        }

        // ✅ 공지사항 저장
        noticeRepository.save(noticeEntity);

        return CreateNoticeResponse.builder()
                .id(noticeEntity.getId())
                .build();
    }
}
