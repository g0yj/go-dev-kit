package com.app.api.email.dto;

import com.app.api.file.FileService;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
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
     * 📌 이메일 첨부파일 정보를 EmailAttachment로 변환
     */
    public static List<EmailAttachment> fromMessage(Message message, FileService fileService) {
        List<EmailAttachment> attachments = new ArrayList<>();
        try {
            if (message.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part instanceof MimeBodyPart && ((MimeBodyPart) part).getFileName() != null) {
                        String fileName = part.getFileName();
                        String fileUrl = fileService.getUrl(fileName, fileName);
                        long fileSize = part.getSize();

                        attachments.add(new EmailAttachment(fileName, fileUrl, fileSize));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [첨부파일 정보 추출 실패]: {}", e.getMessage());
        }
        return attachments;
    }
}
