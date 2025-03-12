package com.app.api.email.dto;

import com.app.api.email.EmailUtils;
import com.app.api.file.FileService;
import com.app.api.file.FileUtils;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 📌파일 정보를 별도로 저장
 *   클라이언트가 첨부파일 URL을 통해 다운로드 가능
 *  파일명, 다운로드 URL, 파일 크기 등 정보를 별도로 관리
 *  파일이 URL로 제공된 경우도 처리 가능 (FileService.getUrl())
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailAttachment {

    String fileName;
    String fileUrl; // ✅ 다운로드 가능한 URL 제공 (FileService에서 생성)
    long fileSize;


    /**
     * 📌 이메일 첨부파일을 안전하게 추출하는 메서드 (Message 지원)
     */
    public static List<EmailAttachment> extractAttachments(Message message, FileService fileService) {
        List<EmailAttachment> attachments = new ArrayList<>();

        try {
            Object content = message.getContent();
            if (!(content instanceof Multipart multipart)) {
                log.warn("⚠️ [첨부파일 없음] Multipart가 아닌 이메일입니다.");
                return attachments;
            }

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);

                // ✅ 첨부파일이 있는 경우
                if (isAttachment(part)) {
                    String rawFileName = EmailUtils.getSafeFileName(part);
                    String sanitizedFileName = EmailUtils.sanitizeFileName(rawFileName);
                    String extension = EmailUtils.getFileExtension(part.getContentType(), rawFileName);
                    long fileSize = part.getSize();

                    // ✅ 파일명 정리 + 확장자 추가
                    String finalFileName = sanitizedFileName + extension;
                    byte[] fileBytes = EmailUtils.extractFileBytes(part);
                    String fileUrl = (fileBytes == null) ? fileService.getUrl(finalFileName) : null;

                    attachments.add(EmailAttachment.builder()
                            .fileName(finalFileName)
                            .fileSize(fileSize)
                            .fileUrl(fileUrl)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [첨부파일 정보 추출 실패]: {}", e.getMessage());
        }
        return attachments;
    }

    /**
     * 📌 해당 BodyPart가 첨부파일인지 확인
     */
    private static boolean isAttachment(BodyPart part) throws Exception {
        return (part.getDisposition() != null && Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
                || (part instanceof MimeBodyPart && ((MimeBodyPart) part).getFileName() != null);
    }


}
