package com.app.api.email;

import com.app.api.file.FileService;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailFileService {
    private final FileService fileService;

    /**
     * 📌 이메일에서 첨부파일 다운로드
     */
    public List<File> downloadAttachment(Message message) throws Exception {
        List<File> saveFiles = new ArrayList<>();

        if (!message.isMimeType("multipart/*")) {
            log.warn("⚠️ [첨부파일 없음] 이메일에 첨부파일이 없습니다.");
            return saveFiles; // ✅ 첨부파일이 없는 경우 빈 리스트 반환
        }

        Multipart multipart = (Multipart) message.getContent();
        String saveDir = fileService.getUploadDir() + "/" + LocalDate.now();
        fileService.createDirectoryIfNotExists(saveDir);

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            // ✅ 첨부파일이 존재하는 경우 (이메일에 직접 포함된 파일)
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                File savedFile = saveEmailAttachment(part, saveDir);
                if (savedFile != null) {
                    saveFiles.add(savedFile);
                }
            }

            // ✅ URL로 제공되는 첨부파일 처리
            else if (part.isMimeType("text/plain")) {
                extractAndDownloadUrl(part, saveDir, saveFiles);
            }
        }
        return saveFiles;
    }

    /**
     * 📌 첨부파일이 URL로 제공된 경우 다운로드 처리
     */
    private void extractAndDownloadUrl(BodyPart part, String saveDir, List<File> saveFiles) {
        try {
            Object content = part.getContent();
            if (content instanceof String textContent && textContent.startsWith("http")) {
                File downloadedFile = fileService.downloadFromUrl(textContent.trim(), saveDir);
                if (downloadedFile != null) {
                    saveFiles.add(downloadedFile);
                }
            }
        } catch (Exception e) {
            log.error("❌ [URL 첨부파일 다운로드 실패] 오류: {}", e.getMessage());
        }
    }
    /**
     * 📌 이메일 첨부파일 저장 (이메일에 직접 포함된 경우)
     */
    private File saveEmailAttachment(BodyPart part, String saveDir) {
        try {
            if (part.getFileName() == null) {
                log.warn("⚠️ [첨부파일 없음] 파일명이 존재하지 않습니다.");
                return null;
            }

            // ✅ 파일명 정리 및 저장 경로 설정
            String fileName = System.currentTimeMillis() + "_" + sanitizeFileName(part.getFileName());
            File file = new File(saveDir + "/" + fileName);

            // ✅ InputStream으로 파일 저장
            try (InputStream is = part.getInputStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            log.info("✅ [첨부파일 저장 완료] {}", file.getAbsolutePath());
            return file;

        } catch (Exception e) {
            log.error("❌ [첨부파일 저장 실패] 오류: {}", e.getMessage());
            return null;
        }
    }
    /**
     * 📌 파일명에서 불필요한 문자 제거
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown_file"; // 파일명이 없는 경우 기본값 설정
        }

        // ✅ 특수문자 제거 (OS에서 허용되지 않는 문자 제거)
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // ✅ 파일명이 너무 길 경우, 앞부분을 잘라내고 확장자 유지
        int maxLength = 100;
        if (fileName.length() > maxLength) {
            int lastDotIndex = fileName.lastIndexOf(".");
            String extension = (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
            fileName = fileName.substring(0, maxLength - extension.length()) + extension;
        }

        return fileName.trim();
    }

}
