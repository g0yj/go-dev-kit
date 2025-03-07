package com.app.api.login.oauth2;

import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.oauth2.dto.OAuth2UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OAuth2Properties oAuth2Properties;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ✅ 카카오 로그인 URL 생성
     */
    public String getKakaoLoginUrl() {
        OAuth2Properties.OAuth2Client kakaoConfig = oAuth2Properties.getKakao();

        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize" +
                "?client_id=" + kakaoConfig.getClientId() +
                "&redirect_uri=" + kakaoConfig.getRedirectUri() +
                "&response_type=code" +
                "&prompt=login";  // ✅ 강제 로그인 옵션 추가

        log.info("🔗 생성된 카카오 로그인 URL: {}", kakaoAuthUrl);
        return kakaoAuthUrl;
    }


    /**
     * ✅ 카카오 Access Token 요청
     */
    public String getKakaoAccessToken(String authorizationCode) {
        log.info("🔑 카카오 OAuth2 Access Token 요청: {}", authorizationCode);

        OAuth2Properties.OAuth2Client kakaoConfig = oAuth2Properties.getKakao();

        return webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .bodyValue("grant_type=authorization_code&client_id=" + kakaoConfig.getClientId() +
                        "&client_secret=" + kakaoConfig.getClientSecret() +
                        "&redirect_uri=" + kakaoConfig.getRedirectUri() +
                        "&code=" + authorizationCode)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText()) // ✅ Access Token 추출
                .block(); // ✅ 동기 방식 (비동기 사용 시 제거 가능)
    }
    /**
     * ✅ 카카오 사용자 정보 요청
     */
    public OAuth2UserInfo getKakaoUserInfo(String accessToken) {
        log.info("🔍 카카오 사용자 정보 요청");

        String kakaoUserInfoUri = "https://kapi.kakao.com/v2/user/me";

        JsonNode response = webClient.get()
                .uri(kakaoUserInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null) {
            String id = response.get("id").asText();
            String nickname = response.path("properties").path("nickname").asText();
            String email = response.path("kakao_account").path("email").asText();

            return new OAuth2UserInfo(id, email, nickname, "KAKAO");
        } else {
            log.error("❌ 카카오 사용자 정보 요청 실패: 응답이 없음");
            throw new RuntimeException("카카오 사용자 정보 요청 실패");
        }
    }
    /**
     * ✅ JWT 생성 및 반환
     */
    public Map<String, String> generateTokens(OAuth2UserInfo userInfo) {
        log.info("🔑 JWT 생성 - 사용자: {}", userInfo.getEmail());

        // ✅ JWT AccessToken & RefreshToken 생성
        String accessToken = jwtTokenProvider.generateAccessToken(userInfo.getEmail(), "USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userInfo.getEmail());

        // ✅ 토큰을 Map 형태로 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    /**
     * ✅ 리프레시 토큰을 통해 새로운 AccessToken 생성
     */
    public String refreshAccessToken(String refreshToken) {
        log.info("🔑 리프레시 토큰을 통해 새로운 AccessToken 생성");

        // ✅ 리프레시 토큰 검증 및 이메일 추출 (여기서는 간단히 `jwtTokenProvider`가 검증)
        String email = jwtTokenProvider.validateRefreshToken(refreshToken);

        // ✅ 이메일을 통해 새로운 AccessToken 발급
        return jwtTokenProvider.generateAccessToken(email, "USER");
    }
}
