package com.app.api.login.jwt.dto;

import com.app.api.login.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * refresh 토큰 사용 X
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserType type;

}
