package com.app.api.email;


import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Getter
@Setter
@Configuration
@RequiredArgsConstructor
public class EmailConfig {

    private final EmailProperties emailProperties;

    /**
     * 📌 SMTP 설정을 포함한 JavaMailSender Bean 생성
     */
    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailProperties.getHost());
        mailSender.setPort(emailProperties.getPort());
        mailSender.setUsername(emailProperties.getUsername());
        mailSender.setPassword(emailProperties.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", emailProperties.getSmtp().isAuth());
        props.put("mail.smtp.starttls.enable", emailProperties.getSmtp().getStarttls().isEnable());
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.debug", "true"); // ✅ 디버깅 활성화 (테스트 시 사용)

        mailSender.setJavaMailProperties(props);
        return mailSender;
    }


    /**
     * 📌 IMAP 서버 연결 (다양한 메일 서비스 지원)
     */
    public Store connectToImap(String username) throws MessagingException {
        log.debug("📩 [IMAP 서버 연결 시도] 이메일: {}", username);

        Properties properties = getImapProperties(username);
        String imapHost = properties.getProperty("mail.imaps.host");

        Session session = Session.getInstance(properties);
        Store store = session.getStore("imaps");

        try {
            store.connect(imapHost, username, emailProperties.getPassword());
            log.info("✅ [IMAP 연결 성공] 서버: {}", username);
            return store;
        } catch (MessagingException e) {
            log.error("❌ [IMAP 연결 실패] 서버: {}, 원인: {}", imapHost, e.getMessage());
            throw e;
        }
    }

    /**
     * 📌 메일 서버 자동 감지 및 IMAP 설정 반환
     */
    private Properties getImapProperties(String email) {
        String provider = detectEmailProvider(email);
        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", "imaps");

        switch (provider) {
            case "GMAIL" -> {
                properties.setProperty("mail.imaps.host", "imap.gmail.com");
                properties.setProperty("mail.imaps.port", "993");
            }
            case "NAVER" -> {
                properties.setProperty("mail.imaps.host", "imap.naver.com");
                properties.setProperty("mail.imaps.port", "993");
            }
            case "OUTLOOK" -> {
                properties.setProperty("mail.imaps.host", "outlook.office365.com");
                properties.setProperty("mail.imaps.port", "993");
            }
            case "YAHOO" -> {
                properties.setProperty("mail.imaps.host", "imap.mail.yahoo.com");
                properties.setProperty("mail.imaps.port", "993");
            }
            default -> {
                log.warn("⚠️ [알 수 없는 메일 서버] 기본 설정 사용");
                properties.setProperty("mail.imaps.host", "imap.unknown.com");
                properties.setProperty("mail.imaps.port", "993");
            }
        }

        properties.setProperty("mail.imaps.ssl.enable", String.valueOf(emailProperties.getImap().getSsl().isEnable()));
        return properties;
    }



    /**
     * 📌 이메일 제공업체 자동 감지
     */
    private String detectEmailProvider(String email) {
        if (email.endsWith("@gmail.com")) return "GMAIL";
        if (email.endsWith("@naver.com")) return "NAVER";
        if (email.endsWith("@outlook.com") || email.endsWith("@hotmail.com")) return "OUTLOOK";
        if (email.endsWith("@yahoo.com")) return "YAHOO";
        return "UNKNOWN";
    }
}