package com.app.api.login.oauth2.go;

import com.app.api.login.oauth2.OAuth2Properties;
import com.app.api.login.oauth2.dto.OAuth2UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 카카오 API에서 사용자 정보를 가져오는 역할
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuth2Service {
    private final WebClient webClient;
    private final OAuth2Properties oAuth2Properties;

    /**
     * ✅ 카카오 로그인 URL 생성
     */
    public String getKakaoLoginUrl() {
        OAuth2Properties.OAuth2Client kakaoConfig = oAuth2Properties.getKakao();
        return "https://kauth.kakao.com/oauth/authorize" +
                "?client_id=" + kakaoConfig.getClientId() +
                "&redirect_uri=" + kakaoConfig.getRedirectUri() +
                "&response_type=code" +
                "&prompt=login";
    }

    /**
     * ✅ 카카오 Access Token 요청
     */
    public String getKakaoAccessToken(String authorizationCode) {
        OAuth2Properties.OAuth2Client kakaoConfig = oAuth2Properties.getKakao();
        return webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue("grant_type=authorization_code&client_id=" + kakaoConfig.getClientId() +
                        "&client_secret=" + kakaoConfig.getClientSecret() +
                        "&redirect_uri=" + kakaoConfig.getRedirectUri() +
                        "&code=" + authorizationCode)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .block();
    }

    /**
     * ✅ 카카오 사용자 정보 요청
     */
    public OAuth2UserInfo getKakaoUserInfo(String accessToken) {
        log.debug("카카오 사용자 정보 요청 -> {}", accessToken);
        JsonNode response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null) {
            return new OAuth2UserInfo(
                    response.get("id").asText(),
                    response.path("kakao_account").path("email").asText(),
                    response.path("properties").path("nickname").asText(),
                    "KAKAO"
            );
        } else {
            throw new RuntimeException("카카오 사용자 정보 요청 실패");
        }
    }
}
