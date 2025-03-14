package com.app.api.email;

import com.app.api.email.dto.SendEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;

    /**
     * 📌 이메일 전송 (첨부 파일 포함)
     * @param emailRequest 이메일 전송 요청 DTO
     */
    public void sendEmailWithAttachment(SendEmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getEmailProperties().getUsername());
            helper.setTo(emailRequest.getToEmail());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), true);

            // ✅ 첨부 파일 추가
            if (emailRequest.getAttachments() != null) {
                for (File file : emailRequest.getAttachments()) {
                    if (file.exists()) {
                        FileSystemResource fileResource = new FileSystemResource(file);
                        helper.addAttachment(file.getName(), fileResource);
                    } else {
                        log.warn("⚠️ [MailService] 첨부 파일이 존재하지 않습니다: {}", file.getAbsolutePath());
                    }
                }
            }

            // ✅ 파일 다운로드 URL 포함 (링크를 본문에 추가)
            if (emailRequest.getAttachmentUrls() != null && !emailRequest.getAttachmentUrls().isEmpty()) {
                String links = String.join("<br>", emailRequest.getAttachmentUrls());
                helper.setText(emailRequest.getBody() + "<br><br><strong>📎 다운로드 링크:</strong><br>" + links, true);
            }

            mailSender.send(message);
            log.info("✅ [MailService] 이메일 전송 완료: {}", emailRequest.getToEmail());

        } catch (MessagingException e) {
            log.error("❌ [MailService] 이메일 전송 실패: {}", e.getMessage());
        } catch (org.springframework.messaging.MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
