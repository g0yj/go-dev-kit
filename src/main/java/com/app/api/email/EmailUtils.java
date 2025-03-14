package com.app.api.email;


import com.app.api.file.FileUtils;
import com.app.api.test.dto.email.SearchRequestEmail;
import com.app.api.utils.DateUtils;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    public List<Message> gmailFilterMessages(Message[] messages, SearchRequestEmail request) {
        List<Message> filteredMessages = new ArrayList<>();
        Date startDate = request.getStartDate() != null ? DateUtils.LocalDateToDate(request.getStartDate()) : null;
        Date endDate = request.getEndDate() != null ? DateUtils.LocalDateToDate(request.getEndDate()) : null;
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
    public SearchTerm buildSearchTerm(SearchRequestEmail emailInfo) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        if (emailInfo.getStartDate() != null) { // 시작일
            Date startDate = DateUtils.LocalDateToDate(emailInfo.getStartDate());
            searchTerms.add(new ReceivedDateTerm(ComparisonTerm.GE, startDate));
        }

        if (emailInfo.getEndDate() != null) { // 종료일
            Date endDate = DateUtils.LocalDateToDate(emailInfo.getEndDate());
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

    /**
     * 이메일 메시지(Message)에서 첨부파일을 찾아서 바이트 배열(byte[])로 변환
     * 첨부파일이 존재하면 byte[] 데이터 반환, 없거나 오류 발생 시 null 반환
     * @param message
     * @return
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

    /**
     * 📌 이메일 첨부파일을 바이트 배열로 변환하는 메서드
     */
    public static byte[] extractFileBytes(BodyPart part) {
        try {
            InputStream inputStream = part.getInputStream();
            if (inputStream == null) {
                log.warn("⚠️ [첨부파일 스트림 없음] 파일을 읽을 수 없습니다.");
                return null;
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("❌ [첨부파일 바이트 변환 실패] 파일명: {}, 오류: {}",
                    getSafeFileName(part),
                    e.getMessage());
            return null;
        }
    }


    /**
     * 📌 MIME 타입과 파일명을 기반으로 확장자 추출
     */
    public static String getFileExtension(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase(); // 소문자로 변환하여 비교

            return switch (contentType) {
                case "application/pdf" -> ".pdf";
                case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
                case "application/vnd.ms-excel" -> ".xls";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "text/plain" -> ".txt";
                case "application/zip" -> ".zip";
                case "application/octet-stream" -> ""; // ⚠️ 확장자 모를 경우 빈 값 반환
                default -> getExtensionFromFileName(fileName); // 파일명에서 확장자 추출
            };
        }

        // MIME 타입이 null이면 파일명에서 확장자 추출
        return getExtensionFromFileName(fileName);
    }

    /**
     * 📌 파일명에서 확장자 추출
     */
    private static String getExtensionFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return ""; // 확장자가 없으면 빈 문자열 반환
    }

    /**
     * 📌 파일명 정리 (특수문자 제거)
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown_file"; // 기본 파일명
        }

        // ✅ OS에서 허용되지 않는 문자 제거
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // ✅ 파일명이 너무 길 경우, 앞부분을 잘라내고 확장자 유지
        int maxLength = 100;
        if (fileName.length() > maxLength) {
            int lastDotIndex = fileName.lastIndexOf(".");
            String extension = (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
            fileName = fileName.substring(0, maxLength - extension.length()) + extension;
        }

        return fileName.trim();
    }

    /**
     * 📌 Date → LocalDate 변환
     */
    public static LocalDate convertToLocalDate(java.util.Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 📌 이메일 본문 추출
     */
    public static String extractBody(Message message) throws IOException, MessagingException {
        Object content = message.getContent();

        if (content instanceof String) {
            return (String) content;
        }
        else if (content instanceof InputStream inputStream) {
            return convertStreamToString(inputStream);
        }
        else if (message.isMimeType("multipart/*")) {
            return getTextFromMultipart((Multipart) content);
        }
        return "(이메일 본문 없음)";
    }

    /**
     * 📌 InputStream을 String으로 변환
     */
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString().trim();
        }
    }

    /**
     * 📌 멀티파트에서 본문 추출 (Gmail, Naver, Daum 대응)
     */
    private static String getTextFromMultipart(Multipart multipart) throws IOException, MessagingException {
        String textContent = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            if (part.isMimeType("multipart/alternative")) {
                String alternativeText = getTextFromMultipart((Multipart) part.getContent());
                if (alternativeText != null) return alternativeText;
            }
            else if (part.isMimeType("multipart/related")) {
                String relatedText = getTextFromMultipart((Multipart) part.getContent());
                if (relatedText != null) return relatedText;
            }
            else if (part.isMimeType("text/html")) {
                return (String) part.getContent();
            }
            else if (part.isMimeType("text/plain") && textContent == null) {
                textContent = (String) part.getContent();
            }
        }
        return textContent != null ? textContent : "(이메일 본문 없음)";
    }

    /**
     * 📌 `Message`와 `MimeMessage`에서 안전하게 파일명을 추출하는 메서드
     */
    public static String getSafeFileName(BodyPart part) {
        try {
            String fileName = part.getFileName();

            // ✅ MIME 인코딩된 파일명을 디코딩
            if (fileName != null) {
                fileName = EmailUtils.decodeMimeEncodedText(fileName);
            }

            // ✅ Content-Disposition에서 파일명 확인
            if (fileName == null && part instanceof MimeBodyPart mimePart) {
                String disposition = mimePart.getDisposition();
                if (disposition != null && disposition.toLowerCase().contains("filename=")) {
                    fileName = disposition.replaceFirst(".*filename=\"?", "").replaceFirst("\"?$", "");
                    fileName = EmailUtils.decodeMimeEncodedText(fileName);
                }
            }

            // ✅ Content-Type에서 파일명 확인 (Gmail 등에서 발생)
            if (fileName == null && part instanceof MimeBodyPart mimePart) {
                String contentType = mimePart.getContentType();
                if (contentType != null && contentType.toLowerCase().contains("name=")) {
                    fileName = contentType.replaceFirst(".*name=\"?", "").replaceFirst("\"?$", "");
                    fileName = EmailUtils.decodeMimeEncodedText(fileName);
                }
            }

            // ✅ 파일명이 없을 경우 기본값 설정
            if (fileName == null || fileName.trim().isEmpty()) {
                return "unknown_file";
            }

            // ✅ 파일명 정리 (특수문자 제거)
            return FileUtils.sanitizeFileName(fileName);

        } catch (Exception e) {
            log.warn("⚠️ [파일명 조회 실패]: {}", e.getMessage());
            return "unknown_file";
        }
    }


    /**
     * 📌 안전하게 이메일 제목을 가져오는 메서드
     */
    public static String getSafeSubject(Message message) {
        try {
            return (message.getSubject() != null) ? message.getSubject() : "(제목 없음)";
        } catch (MessagingException e) {
            log.warn("⚠️ [이메일 제목 조회 실패]: {}", e.getMessage());
            return "(제목 불러오기 실패)";
        }
    }

    /**
     * 📌 안전하게 이메일 발신자를 가져오는 메서드
     */
    public static String getSafeFrom(Message message) {
        try {
            Address[] fromAddresses = message.getFrom();
            return (fromAddresses != null && fromAddresses.length > 0) ?
                    fromAddresses[0].toString() : "(발신자 없음)";
        } catch (MessagingException e) {
            log.warn("⚠️ [이메일 발신자 조회 실패]: {}", e.getMessage());
            return "(발신자 불러오기 실패)";
        }
    }


    /**
     * 📌 MIME 인코딩된 문자열을 정상적인 UTF-8 문자열로 변환
     */
    public static String decodeMimeEncodedText(String encodedText) {
        if (encodedText == null || encodedText.isBlank()) {
            return "(파일명 없음)";
        }

        try {
            return MimeUtility.decodeText(encodedText);
        } catch (Exception e) {
            log.warn("⚠️ [MIME 디코딩 실패] 원본: {}, 오류: {}", encodedText, e.getMessage());
            return encodedText; // 디코딩 실패 시 원본 그대로 반환
        }
    }

}
