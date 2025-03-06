package com.app.api.login.jwt.dto;

import com.app.api.login.UserType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtTokenResponse {
    String username;
    String accessToken;
    String refreshToken;
    UserType type;
}
