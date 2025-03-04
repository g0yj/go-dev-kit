package com.app.api.email.dto;

import com.app.api.file.FileService;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 📩 이메일 본문과 첨부파일 정보를 포함하는 DTO
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailResponse {

    List<String> to;
    String from;
    String subject;
    String body; // ✅ 이메일 본문 내용
    LocalDate receivedDate;
    List<EmailAttachment> attachments; // ✅ 첨부파일 정보 추가

    /**
     * 📌 이메일 메시지를 EmailResponse 객체로 변환 (첨부파일 정보 포함)
     */
    public static EmailResponse from(Message message, FileService fileService) {
        if (message == null) {
            log.warn("⚠️ [이메일 변환 실패] 메시지가 null입니다.");
            return getDefaultEmailResponse();
        }

        if (fileService == null) {
            log.error("❌ [파일 서비스 주입 실패] FileService가 null입니다.");
            throw new IllegalStateException("FileService가 null입니다. DI(의존성 주입)를 확인하세요.");
        }

        try {
            log.debug("📩 [이메일 변환 시작] 메시지 제목: {}", message.getSubject());

            return EmailResponse.builder()
                    .subject(getSafeSubject(message)) // ✅ 제목 가져오기 (예외 처리)
                    .from(getSafeFrom(message)) // ✅ 발신자 가져오기 (예외 처리)
                    .receivedDate(convertToLocalDate(message.getReceivedDate())) // ✅ LocalDate 변환
                    .body(extractBody(message)) // ✅ 이메일 본문 추가
                    .attachments(EmailAttachment.extractAttachments(message, fileService)) // ✅ 첨부파일 정보 포함
                    .build();
        } catch (MessagingException | IOException e) {
            log.error("❌ [이메일 변환 오류]: {}", e.getMessage(), e);
            return getDefaultEmailResponse();
        }
    }

    /**
     * 📌 Date → LocalDate 변환 메서드 추가
     */
    private static LocalDate convertToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 📌 이메일 본문 추출 메서드 (Gmail, Naver, Daum 모두 지원)
     */
    private static String extractBody(Message message) throws IOException, MessagingException {
        if (message.isMimeType("text/plain") || message.isMimeType("text/html")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            return getTextFromMultipart((Multipart) message.getContent());
        }
        return "(이메일 본문 없음)";
    }

    /**
     * 📌 멀티파트에서 본문 추출 (Gmail, Naver, Daum 대응)
     */
    private static String getTextFromMultipart(Multipart multipart) throws IOException, MessagingException {
        String textContent = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            // ✅ `multipart/alternative` (가장 많이 사용됨)
            if (part.isMimeType("multipart/alternative")) {
                String alternativeText = getTextFromMultipart((Multipart) part.getContent());
                if (alternativeText != null) return alternativeText;
            }
            // ✅ `multipart/related` (네이버, 다음에서 사용)
            else if (part.isMimeType("multipart/related")) {
                String relatedText = getTextFromMultipart((Multipart) part.getContent());
                if (relatedText != null) return relatedText;
            }
            // ✅ HTML 본문이 있으면 HTML을 우선 반환 (Gmail, Naver, Daum 모두 적용)
            else if (part.isMimeType("text/html")) {
                return (String) part.getContent();
            }
            // ✅ 일반 텍스트가 있으면 저장
            else if (part.isMimeType("text/plain") && textContent == null) {
                textContent = (String) part.getContent();
            }
        }
        return textContent != null ? textContent : "(이메일 본문 없음)";
    }

    /**
     * 📌 안전하게 이메일 제목을 가져오는 메서드
     */
    private static String getSafeSubject(Message message) {
        try {
            return message.getSubject() != null ? message.getSubject() : "(제목 없음)";
        } catch (MessagingException e) {
            log.warn("⚠️ [이메일 제목 조회 실패]: {}", e.getMessage());
            return "(제목 불러오기 실패)";
        }
    }

    /**
     * 📌 안전하게 발신자 정보를 가져오는 메서드
     */
    private static String getSafeFrom(Message message) {
        try {
            return (message.getFrom() != null && message.getFrom().length > 0) ?
                    message.getFrom()[0].toString() : "(발신자 없음)";
        } catch (MessagingException e) {
            log.warn("⚠️ [발신자 조회 실패]: {}", e.getMessage());
            return "(발신자 불러오기 실패)";
        }
    }

    /**
     * 📌 기본 값이 설정된 EmailResponse 객체 반환
     */
    private static EmailResponse getDefaultEmailResponse() {
        return new EmailResponse(
                new ArrayList<>(), // to
                "(발신자 없음)", // from
                "(제목 없음)", // subject
                "(내용을 불러올 수 없습니다.)", // body
                null, // receivedDate
                new ArrayList<>() // attachments
        );
    }
}
