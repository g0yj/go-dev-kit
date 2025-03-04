package com.app.api.email;

import com.app.api.email.dto.EmailAttachment;
import com.app.api.email.dto.EmailResponse;
import com.app.api.file.FileService;
import com.app.api.test.controller.dto.email.SearchRequestEmail;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Getter@Setter
@Slf4j
@RequiredArgsConstructor
public class EmailReceiverService {
    private final EmailConfig emailConfig;
    private final EmailUtils emailUtils;
    private final EmailFileService emailFileService;
    private final FileService fileService;

    /**
     * 📌 특정 기간 내 이메일 조회
     *   클라이언트 측에서 파일에 대한 정보를 알기 위해서는 변환이 필요. javaMail은 Message타입을 반환하는데 이는 클라이언트쪽에서 확인 불가.
     * @param request 필터링 조건
     * @return EmailResponse 리스트
     */
    public List<EmailResponse> getList(SearchRequestEmail request) {
        log.debug("✅ [이메일 조회 시작] 조건: {}", request);

        List<Message> messages = getRawMessages(request);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmailResponse> responses = new ArrayList<>();

        for (Message message : messages) {
            try {
                // ✅ 안전하게 이메일 정보를 추출
                String subject = EmailUtils.getSafeSubject(message);
                String from = EmailUtils.getSafeFrom(message);
                LocalDate receivedDate = EmailUtils.convertToLocalDate(message.getReceivedDate());
                String body = EmailUtils.extractBody(message);
                List<EmailAttachment> attachments = EmailAttachment.extractAttachments(message, fileService);

                responses.add(EmailResponse.builder()
                        .subject(subject)
                        .from(from)
                        .receivedDate(receivedDate)
                        .body(body)
                        .attachments(attachments)
                        .build());

            } catch (Exception e) {
                log.warn("⚠️ [이메일 변환 오류 발생]: {}", e.getMessage());
            }
        }

        return responses;
    }

    public List<Message> getRawMessages(SearchRequestEmail request) {
        log.debug("✅ [이메일 조회 시작] 조건: {}", request);
        Store store = null;
        Folder inbox = null;
        List<Message> messagesList = new ArrayList<>();

        try {
            store = emailConfig.connectToImap(emailConfig.getUsername());
            if (store == null) {
                log.error("❌ [IMAP 연결 실패] Store가 null입니다.");
                return Collections.emptyList();
            }

            inbox = store.getFolder("INBOX");
            if (inbox == null) {
                log.error("❌ [INBOX 조회 실패]");
                return Collections.emptyList();
            }
            inbox.open(Folder.READ_ONLY);
            log.debug("📩 [INBOX 열기 성공]");

            Message[] messages;
            if (emailConfig.getUsername().contains("@gmail")) {
                messages = inbox.getMessages();
                messages = emailUtils.gmailFilterMessages(messages, request).toArray(new Message[0]);
            } else {
                SearchTerm searchTerm = emailUtils.buildSearchTerm(request);
                messages = (searchTerm != null) ? inbox.search(searchTerm) : inbox.getMessages();
            }

            if (messages != null && messages.length > 0) {
                messagesList.addAll(List.of(messages)); // ✅ Inbox를 닫지 않으므로 Message 직접 사용 가능
            }

            log.debug("📩 [조회된 이메일 개수]: {}개", messagesList.size());

        } catch (MessagingException e) {
            log.error("❌ [이메일 조회 중 오류 발생]: {}", e.getMessage(), e);
        }
        // 🔴 Inbox를 닫지 않음 → Message를 안전하게 유지 가능
        return messagesList;
    }

    /**
     * 📌 이메일에 첨부파일이 포함 되어 있는지 확인
     */
    private boolean hasAttachment(Message message) throws MessagingException, IOException {
        if(message.getContent() instanceof Multipart multipart){
            for( int i = 0; i < multipart.getCount(); i++ ){
                if(Part.ATTACHMENT.equalsIgnoreCase(multipart.getBodyPart(i).getDisposition())){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 📌 첨부파일 URL 추출 메서드
     */
    private List<String> extractFileUrls(Message message) throws MessagingException, IOException {
        List<String> fileUrls = new ArrayList<>();
        if (message.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain") && part.getContent() instanceof String content) {
                    if (content.startsWith("http")) {
                        fileUrls.add(content.trim());
                    }
                }
            }
        }
        return fileUrls;
    }

}


