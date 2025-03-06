package com.app.api.login.session.dto;

import com.app.api.login.UserType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionRequest {
    private String username;
    private String password;
    private UserType type; // Enum 타입 확인 필요
}
