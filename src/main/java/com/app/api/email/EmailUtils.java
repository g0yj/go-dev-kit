package com.app.api.email;


import com.app.api.test.controller.dto.email.SearchlRequestEmail;
import com.app.api.utils.DateUtils;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class EmailUtils {

    /**
     *  📌 IMAP 세션을 안전하게 종료하는 역할
     */
    public void closeResources(Folder inbox, Store store){
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false); // 변경 사항 없이 닫기
                log.debug("📩 [받은 편지함(INBOX) 닫힘]");
            }
        } catch (MessagingException e) {
            log.warn("⚠️ [받은 편지함 닫기 실패]: {}", e.getMessage());
        }

        try {
            if (store != null && store.isConnected()) {
                store.close();
                log.debug("📩 [IMAP 세션 종료]");
            }
        } catch (MessagingException e) {
            log.warn("⚠️ [IMAP 세션 종료 실패]: {}", e.getMessage());
        }
    }


    /**
     * 📌 Gmail에서 날짜 필터링을 위한 추가 메서드(추가/삭제 가능합니다)
     *      error: A4 BAD Could not parse command 해결을 위함
     */
    public List<Message> gmailFilterMessages(Message[] messages, SearchlRequestEmail request) {
        List<Message> filteredMessages = new ArrayList<>();
        Date startDate = request.getStartDate() != null ? DateUtils.convertToDate(request.getStartDate()) : null;
        Date endDate = request.getEndDate() != null ? DateUtils.convertToDate(request.getEndDate()) : null;
        String keyword = request.getSubject() != null ? request.getSubject().toLowerCase() : null;

        for (Message message : messages) {
            try {
                Date receivedDate = message.getReceivedDate();
                String subject = message.getSubject() != null ? message.getSubject().toLowerCase() : "";

                // ✅ 날짜 조건 확인
                boolean dateMatches = (startDate == null || !receivedDate.before(startDate)) &&
                        (endDate == null || !receivedDate.after(endDate));

                // ✅ 제목 키워드 포함 여부 확인
                boolean subjectMatches = (keyword == null || subject.contains(keyword));

                if (dateMatches && subjectMatches) {
                    filteredMessages.add(message);
                    log.debug("📩 [필터링된 이메일] 제목: {} , 수신일: {}", message.getSubject(), receivedDate);
                }
            } catch (MessagingException e) {
                log.warn("⚠️ [이메일 필터링 중 오류 발생]: {}", e.getMessage());
            }
        }
        return filteredMessages;
    }

    /**
     * 📌 검색 필터(SearchTerm) 생성 (추가/삭제 가능합니다)
     * - 특정 기간 (시작일 ~ 종료일)
     * - 특정 발신자 이메일 필터링
     * - 특정 제목 포함 메일 검색
     */
    public SearchTerm buildSearchTerm(SearchlRequestEmail emailInfo) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        if (emailInfo.getStartDate() != null) { // 시작일
            Date startDate = DateUtils.convertToDate(emailInfo.getStartDate());
            searchTerms.add(new ReceivedDateTerm(ComparisonTerm.GE, startDate));
        }

        if (emailInfo.getEndDate() != null) { // 종료일
            Date endDate = DateUtils.convertToDate(emailInfo.getEndDate());
            searchTerms.add(new ReceivedDateTerm(ComparisonTerm.LE, endDate));
        }
        if (emailInfo.getFrom() != null && !emailInfo.getFrom().isEmpty()) {
            try {
                searchTerms.add(new FromTerm(new InternetAddress(emailInfo.getFrom())));
            } catch (AddressException e) {
                log.warn("⚠️ [잘못된 이메일 주소] 발신자 필터링 제외됨: {}", emailInfo.getFrom());
            }
        }
        if (emailInfo.getSubject() != null && !emailInfo.getSubject().isEmpty()) {
            searchTerms.add(new SubjectTerm(emailInfo.getSubject()));
        }

        // ✅ 검색 조건이 하나라도 존재하면 AND 조건으로 결합(아래 설명)
        if (!searchTerms.isEmpty()) {
            return searchTerms.size() == 1 ? searchTerms.get(0) : new AndTerm(searchTerms.toArray(new SearchTerm[0]));
        }

        return null; // 검색 조건 없으면 전체 이메일 반환
    }
/**
 * searchTerms 리스트에 여러 개의 SearchTerm이 존재해도, Folder.search()는 하나의 SearchTerm만 받음.
 * 따라서, 여러 개의 SearchTerm이 있을 경우 AndTerm을 사용하여 하나로 묶어야 함.
 * 또한 날짜의 경우 Date로 비교해야됨. Dayutils에 있는 메소드 사용.
 */

    public  byte[] extractAttachmentData(Message message) {
        try {
            if (message.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);

                    // 🔹 첨부파일인지 확인
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) ||
                            StringUtils.isNotBlank(part.getFileName())) {

                        try (InputStream inputStream = part.getInputStream();
                             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            return outputStream.toByteArray(); // 첨부파일을 byte[]로 변환
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [첨부파일 데이터 추출 실패]: {}", e.getMessage());
        }
        return null; // 첨부파일이 없거나 오류가 발생한 경우 null 반환
    }

}
