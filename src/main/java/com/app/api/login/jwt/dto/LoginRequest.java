package com.app.api.login.jwt.dto;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String username;
    private String password;
}
