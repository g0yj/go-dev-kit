package com.app.api.test.controller.dto.email;

import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Builder
@Getter@Setter
@AllArgsConstructor@NoArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailResponse {

    List<String> to;
    String from;
    String subject;
    String body;
    LocalDate receivedDate;

    // 파일 업로드 방식에 따라 달라짐
    List<String> fileUrls; // 첨부파일 URL 리스트
    byte[] fileBytes; // 첨부파일 실제 데이터

    /**
     * 📌 이메일 메시지를 EmailResponse 객체로 변환
     */
    public static EmailResponse from(Message message, byte[] fileData) {
        try {
            return EmailResponse.builder()
                    .subject(message.getSubject()) // 제목
                    .from(message.getFrom() != null ? message.getFrom()[0].toString() : "Unknown") // 발신자
                    .receivedDate(convertToLocalDate(message.getReceivedDate())) // 받은 날짜 변환
                    .fileUrls(extractFileUrls(message)) // 첨부파일 URL 추출
                    .fileBytes(fileData) // 파일 데이터 저장
                    .build();
        } catch (MessagingException e) {
            log.warn("⚠️ [이메일 변환 오류]: {}", e.getMessage());
            return new EmailResponse(
                    new ArrayList<>(), // to
                    "Unknown", // from
                    "읽을 수 없음", // subject
                    "", // body
                    null, // receivedDate
                    new ArrayList<>(), // fileUrls
                    null // fileBytes
            );
        }
    }

    /**
     * 📌 Date → LocalDate 변환 메서드
     */
    private static LocalDate convertToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 📌 첨부파일 URL 추출 메서드
     */
    private static List<String> extractFileUrls(Message message) {
        List<String> fileUrls = new ArrayList<>();
        try {
            if (message.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part instanceof MimeBodyPart && ((MimeBodyPart) part).getFileName() != null) {
                        fileUrls.add(part.getFileName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [첨부파일 URL 추출 실패]: {}", e.getMessage());
        }
        return fileUrls;
    }

}
