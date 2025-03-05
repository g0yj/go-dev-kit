package com.app.api.login.jwt;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String username;
    private String password;
}
