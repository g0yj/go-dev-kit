package com.app.api.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AppErrorCode implements AppError {
    // common error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9999", "서버에 문제가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "9900", "잘못된 요청입니다. {}"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "9901", "잘못된 파라미터입니다. ({})"),
    PARAMETER_REQUIRED(HttpStatus.BAD_REQUEST, "9901", "{} 파라미터는 필수입니다.");


    HttpStatusCode httpStatusCode;
    String code;
    String message;

    public String getCode() {
        return "api-" + code;
    }

}