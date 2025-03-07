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
public class NaverOAuth2Service {
    private final WebClient webClient;
    private final OAuth2Properties oAuth2Properties;

    /**
     * ✅ Naver 로그인 URL 생성
     */
    public String getNaverLoginUrl(String state) {
        OAuth2Properties.OAuth2Client naverConfig = oAuth2Properties.getNaver();
        return "https://nid.naver.com/oauth2.0/authorize" +
                "?client_id=" + naverConfig.getClientId() +
                "&redirect_uri=" + naverConfig.getRedirectUri() +
                "&response_type=code" +
                "&state=" + state;
    }

    /**
     * ✅ Naver Access Token 요청
     */
    public String getNaverAccessToken(String authorizationCode, String state) {
        OAuth2Properties.OAuth2Client naverConfig = oAuth2Properties.getNaver();
        return webClient.post()
                .uri("https://nid.naver.com/oauth2.0/token")
                .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue("grant_type=authorization_code&client_id=" + naverConfig.getClientId() +
                        "&client_secret=" + naverConfig.getClientSecret() +
                        "&redirect_uri=" + naverConfig.getRedirectUri() +
                        "&code=" + authorizationCode +
                        "&state=" + state)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .block();
    }

    /**
     * ✅ Naver 사용자 정보 요청
     */
    public OAuth2UserInfo getNaverUserInfo(String accessToken) {
        JsonNode response = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
                .path("response");

        return new OAuth2UserInfo(
                response.get("id").asText(),
                response.get("email").asText(),
                response.get("name").asText(),
                "NAVER"
        );
    }
}
