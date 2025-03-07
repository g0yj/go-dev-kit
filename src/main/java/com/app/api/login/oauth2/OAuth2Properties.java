package com.app.api.login.oauth2;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
@Getter
@Setter
public class OAuth2Properties {

    private OAuth2Client kakao;  // ✅ 기존 Map<String, OAuth2Client>에서 단일 객체로 변경
    private OAuth2Client naver;
    private OAuth2Client google;

    @Getter
    @Setter
    public static class OAuth2Client {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }
}
