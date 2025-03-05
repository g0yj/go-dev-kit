package com.app.api.login.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import okhttp3.Cookie;

/**
 * 로그인 시 Access Token과 Refresh Token을 응답하는 클래스
 */
@Getter
@AllArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
}