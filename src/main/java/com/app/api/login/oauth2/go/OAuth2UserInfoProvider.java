package com.app.api.login.oauth2.go;

import com.app.api.login.oauth2.dto.OAuth2UserInfo;

public interface OAuth2UserInfoProvider {
    OAuth2UserInfo getUserInfo(String accessToken);
}
