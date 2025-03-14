package com.app.api.email.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.util.List;

/**
 * 📌 이메일 전송 요청 DTO
 */
@Getter
@Builder
@ToString
public class SendEmailRequest {
    private final String toEmail; // ✅ 수신자 이메일
    private final String subject; // ✅ 이메일 제목
    private final String body; // ✅ 이메일 본문 (HTML 가능)
    private final List<File> attachments; // ✅ 첨부 파일 리스트 (파일 객체 전달)
    private final List<String> attachmentUrls; // ✅ 다운로드 URL 리스트 (외부 접근 가능
}
