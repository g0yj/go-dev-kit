package com.app.api.email;

import com.app.api.test.controller.dto.email.EmailResponse;
import com.app.api.test.controller.dto.email.SearchlRequestEmail;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.search.SearchTerm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Getter@Setter
@Slf4j
@RequiredArgsConstructor
public class EmailReceiverService {
    private final EmailService emailService;
    private final EmailUtils emailUtils;
    /**
     * 📌 이메일 전체 가져오기
     *  @param request 필터링 조건
     */
    public List<EmailResponse> getList(SearchlRequestEmail request) {
        log.debug("✅ [이메일 조회 시작] 조건: {}", request);
        List<Message> filteredMessages = new ArrayList<>();
        List<EmailResponse> responses = new ArrayList<>(); // javamail을 목록에 출력하기 위해서 변환이 필요
        Store store = null;
        Folder inbox = null;

        try {
            // 🔹 IMAP 서버 연결
            store = emailService.connectToImap(emailService.getUsername());
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

            Message[] messages;
            if(emailService.getUsername().contains("@gmail")){
                // 🔹 Gmail: 모든 이메일을 가져온 후 Java에서 직접 필터링
                messages = inbox.getMessages();
                filteredMessages = emailUtils.gmailFilterMessages(messages, request);//조건 추가/수정 필요
            } else {
                // 🔹 네이버 등 기타 IMAP 서버: 기본 `SearchTerm` 적용
                SearchTerm searchTerm = emailUtils.buildSearchTerm(request); //조건 추가/수정 필요)
                messages = (searchTerm != null) ? inbox.search(searchTerm) : inbox.getMessages();
                if(messages != null){
                    Collections.addAll(filteredMessages, messages);
                }
            }
            if(filteredMessages.isEmpty()){
                log.warn("⚠️ [이메일 없음] 검색 조건에 맞는 이메일이 없습니다.");
                return Collections.emptyList();
            }
            log.debug("📩 [조회된 이메일 개수]: {}개", filteredMessages.size());

            for(Message message: filteredMessages) {
                try {
                    byte[] fileData = emailUtils.extractAttachmentData(message);
                    responses.add(EmailResponse.from(message, fileData));
                } catch (Exception e){
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

}


