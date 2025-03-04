package com.app.api.email;

import com.app.api.email.dto.EmailAttachment;
import com.app.api.email.dto.EmailResponse;
import com.app.api.file.FileService;
import com.app.api.test.controller.dto.email.SearchRequestEmail;
import jakarta.mail.*;
import jakarta.mail.search.SearchTerm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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
     * @param request 필터링 조건
     * @return EmailResponse 리스트
     */
    public List<EmailResponse> getList(SearchRequestEmail request) {
        log.debug("✅ [이메일 조회 시작] 조건: {}", request);
        List<Message> filteredMessages = new ArrayList<>();
        List<EmailResponse> responses = new ArrayList<>(); // javamail을 목록에 출력하기 위해서 변환이 필요
        Store store = null;
        Folder inbox = null;

        try {
            // 🔹 IMAP 서버 연결
            store = emailConfig.connectToImap(emailConfig.getUsername());
            if (store == null) {
                log.error("❌ [IMAP 연결 실패] Store가 null입니다.");
                return Collections.emptyList();
            }
            // 🔹 받은 편지함(INBOX) 가져오기
            inbox = store.getFolder("INBOX");
            if (inbox == null) {
                log.error("❌ [받은 편지함(INBOX) 조회 실패]");
                return Collections.emptyList();
            }

            inbox.open(Folder.READ_ONLY);
            log.debug("📩 [이메일 조회] 받은 편지함 열기 성공!");

            // 🔹 이메일 필터링
            Message[] messages;
            if(emailConfig.getUsername().contains("@gmail")){
                //  Gmail: 모든 이메일을 가져온 후 Java에서 직접 필터링
                messages = inbox.getMessages();
                filteredMessages = emailUtils.gmailFilterMessages(messages, request);//조건 추가/수정 필요
            } else {
                //  네이버 등 기타 IMAP 서버: 기본 `SearchTerm` 적용
                SearchTerm searchTerm = emailUtils.buildSearchTerm(request); //조건 추가/수정 필요)
                messages = (searchTerm != null) ? inbox.search(searchTerm) : inbox.getMessages();
                if(messages != null){
                    Collections.addAll(filteredMessages, messages);
                }
            }

            if (filteredMessages.isEmpty()){
                log.warn("⚠️ [이메일 없음] 검색 조건에 맞는 이메일이 없습니다.");
                return Collections.emptyList();
            }
            log.debug("📩 [조회된 이메일 개수]: {}개", filteredMessages.size());

            // 🔹 이메일 정보를 `EmailResponse` 객체로 변환
            for (Message message : filteredMessages) {
                try {
                    // ✅ 이메일 본문 정보 생성
                    EmailResponse emailResponse = EmailResponse.from(message, fileService);

                    // ✅ 첨부파일 다운로드 및 변환
                    List<File> savedFiles = emailFileService.downloadAttachment(message);
                    List<EmailAttachment> attachments = new ArrayList<>();
                    for (File file : savedFiles) {
                        String fileUrl = fileService.getUrl(file.getName(), file.getName());
                        attachments.add(new EmailAttachment(file.getName(), fileUrl, file.length()));
                    }
                    // ✅ 응답 객체에 첨부파일 정보 추가
                    emailResponse.setAttachments(attachments);
                    responses.add(emailResponse);

                } catch (Exception e) {
                    log.warn("⚠️ [이메일 변환 오류 발생]: {}", e.getMessage());
                }
            }

        } catch (MessagingException e) {
            log.error("❌ [이메일 조회 중 오류 발생]: {}", e.getMessage(), e);
        } finally {
            emailUtils.closeResources(inbox, store);
        }

        return responses;
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


