package com.app.api.login.oauth2.go;

import com.app.api.login.UserType;
import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.oauth2.dto.OAuth2UserInfo;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 각 OAuth2 제공자를 선택하여 호출하는 역할
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service2 implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final KakaoOAuth2Service kakaoOAuth2Service;
    private final GoogleOAuth2Service googleOAuth2Service;
    private final NaverOAuth2Service naverOAuth2Service;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
       log.debug("loadUser 진입 -> OAuth2UserRequest : {}", userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // OAuth2 제공자 구분
        log.debug("registrationId : {}", registrationId);
        // 1️⃣ 제공자별 사용자 정보 가져오기
        OAuth2UserInfo userInfo;
        if ("kakao".equals(registrationId)) {
            log.debug("kakao if문 들어옴? registrationId: {}" , registrationId);
            userInfo = kakaoOAuth2Service.getKakaoUserInfo(userRequest.getAccessToken().getTokenValue());
        } else if ("google".equals(registrationId)) {
            userInfo = googleOAuth2Service.getGoogleUserInfo(userRequest.getAccessToken().getTokenValue());
        } else if ("naver".equals(registrationId)) {
            userInfo = naverOAuth2Service.getNaverUserInfo(userRequest.getAccessToken().getTokenValue());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }

        // 2️⃣ 사용자 저장 또는 업데이트
        Optional<UserEntity> existingUser = userRepository.findByUsername(userInfo.getEmail());
        if (existingUser.isEmpty()) {
            UserEntity newUser = UserEntity.builder()
                    .type(UserType.A)
                    .username(userInfo.getEmail())
                    .password(null)
                    .build();
            userRepository.save(newUser);
            existingUser = Optional.of(newUser);
        }

        // 3️⃣ 사용자 인증 객체 생성
        UserEntity userEntity = existingUser.get();
        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userEntity.getType().name())),
                Map.of("email", userInfo.getEmail()),
                "email"
        );
    }

    /**
     * ✅ OAuth2 Access Token 요청 (카카오, 구글, 네이버 구분)
     */
    public String getAccessToken(String authorizationCode, String provider) {
        return getAccessToken(authorizationCode, provider, null); // 기본적으로 state는 null로 설정
    }
    public String getAccessToken(String authorizationCode, String provider, String state) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> kakaoOAuth2Service.getKakaoAccessToken(authorizationCode);
            case "google" -> googleOAuth2Service.getGoogleAccessToken(authorizationCode);
            case "naver" -> {
                if (state == null) {
                    throw new IllegalArgumentException("네이버 로그인 시 state 값이 필요합니다.");
                }
                yield naverOAuth2Service.getNaverAccessToken(authorizationCode, state);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + provider);
        };
    }

    /**
     * ✅ OAuth2 사용자 정보 요청 (카카오, 구글, 네이버 구분)
     */
    public OAuth2UserInfo getUserInfo(String accessToken, String provider) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> kakaoOAuth2Service.getKakaoUserInfo(accessToken);
            case "google" -> googleOAuth2Service.getGoogleUserInfo(accessToken);
            case "naver" -> naverOAuth2Service.getNaverUserInfo(accessToken);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + provider);
        };
    }

    /**
     * ✅ JWT 생성 및 반환
     */
    public Map<String, String> generateTokens(OAuth2UserInfo userInfo) {
        String accessToken = jwtTokenProvider.generateAccessToken(userInfo.getEmail(), "USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userInfo.getEmail());
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }
}
