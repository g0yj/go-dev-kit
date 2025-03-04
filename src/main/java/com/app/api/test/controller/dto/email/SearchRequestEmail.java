package com.app.api.test.controller.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter@Setter@Builder
@AllArgsConstructor@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "이메일 요청 DTO (필터링 요청 시 사용)")
public class SearchRequestEmail {


    @Schema(description = "수신자 목록", example = "[\"user1@example.com\", \"user2@example.com\"]")
    List<String> to;

    @Schema(description = "발신자 이메일", example = "sender@example.com")
    String from;

    @Schema(description = "메일 제목", example = "안녕하세요, 공지사항 안내드립니다.")
    String subject;

    @Schema(description = "메일 내용 (HTML 가능)", example = "<h1>이메일 내용</h1>")
    String body;

    @Schema(description = "첨부 파일 리스트", example = "[\"file1.pdf\", \"image.png\"]")
    List<String> attachments;

    @Schema(description = "특정 날짜 이후의 메일 조회 (검색용)", example = "2024-12-31")
    LocalDate startDate;

    @Schema(description = "특정 날짜 이전의 메일 조회 (검색용)", example = "2024-12-31")
    LocalDate endDate;

    @Schema(description = "특정 날짜 이전의 메일 조회 (검색용)", example = "상담.csv")
    List<File> fileNames;

    /**
     * 📌 수신자 목록을 InternetAddress 배열로 변환 (이메일 전송 시 사용)
     */
    public InternetAddress[] getToAddresses() {
        return to != null
                ? to.stream()
                .map(this::toInternetAddress)
                .filter(Objects::nonNull) // null 제거
                .toArray(InternetAddress[]::new)
                : new InternetAddress[0];
    }

    /**
     * 📌 문자열 이메일 주소를 InternetAddress 객체로 변환 (예외 처리 포함)
     */
    private InternetAddress toInternetAddress(String email) {
        try {
            return new InternetAddress(email, true); // true: 이메일 주소 검증
        } catch (AddressException e) {
            System.err.println("⚠️ 잘못된 이메일 주소: " + email + " (" + e.getMessage() + ")");
            return null;
        }
    }

}

/**
 * 1️⃣ 왜 String이 아니라 InternetAddress를 사용해야 할까?
 * ✅ JavaMail API에서는 이메일 주소를 String으로 직접 사용하지 않아요!
 * ✅ javax.mail.internet.InternetAddress 클래스를 사용해서 이메일 주소를 검증하고, 올바른 형식으로 변환해야 해요.
 * ✅ 메일을 보낼 때 Message.setRecipients() 메서드는 InternetAddress[] 타입을 요구합니다.
 *
 * 2️⃣ 주요 기능
 * 이메일 주소 형식 검증 (올바른 이메일인지 확인)
 * InternetAddress 객체로 변환 (JavaMail API에서 요구하는 타입으로 변환)
 * 잘못된 이메일 예외 처리 (형식이 틀린 경우 예외 발생)
 */