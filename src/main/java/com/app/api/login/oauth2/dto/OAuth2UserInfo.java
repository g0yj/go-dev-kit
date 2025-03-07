package com.app.api.login.oauth2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2UserInfo {
    private String providerId;
    private String email;
    private String nickname;
    private String provider;
}
