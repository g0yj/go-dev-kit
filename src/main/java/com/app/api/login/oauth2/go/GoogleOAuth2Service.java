package com.app.api.login.oauth2.go;

import com.app.api.login.oauth2.OAuth2Properties;
import com.app.api.login.oauth2.dto.OAuth2UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2Service {
    private final WebClient webClient;
    private final OAuth2Properties oAuth2Properties;

    /**
     * ✅ Google 로그인 URL 생성
     */
    public String getGoogleLoginUrl() {
        OAuth2Properties.OAuth2Client googleConfig = oAuth2Properties.getGoogle();
        return "https://accounts.google.com/o/oauth2/auth" +
                "?client_id=" + googleConfig.getClientId() +
                "&redirect_uri=" + googleConfig.getRedirectUri() +
                "&response_type=code" +
                "&scope=email%20profile";
    }

    /**
     * ✅ Google Access Token 요청
     */
    public String getGoogleAccessToken(String authorizationCode) {
        OAuth2Properties.OAuth2Client googleConfig = oAuth2Properties.getGoogle();
        return webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue("grant_type=authorization_code&client_id=" + googleConfig.getClientId() +
                        "&client_secret=" + googleConfig.getClientSecret() +
                        "&redirect_uri=" + googleConfig.getRedirectUri() +
                        "&code=" + authorizationCode)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .block();
    }

    /**
     * ✅ Google 사용자 정보 요청
     */
    public OAuth2UserInfo getGoogleUserInfo(String accessToken) {
        JsonNode response = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null) {
            return new OAuth2UserInfo(
                    response.get("id").asText(),
                    response.get("email").asText(),
                    response.get("name").asText(),
                    "GOOGLE"
            );
        } else {
            throw new RuntimeException("Google 사용자 정보 요청 실패");
        }
    }
}
