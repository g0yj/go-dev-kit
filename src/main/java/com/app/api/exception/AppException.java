package com.app.api.exception;

import com.app.api.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppException extends RuntimeException {

    HttpStatusCode httpStatusCode;
    String code;
    String message;
    Object[] args;


    public AppException(HttpStatusCode httpStatusCode, String code, String message, Object[] args) {
        super(replaceMessage(message, args));
        this.httpStatusCode = httpStatusCode;
        this.code = code;
        this.message = super.getMessage();
        this.args = args;
    }

    public AppException (Throwable cause , HttpStatusCode httpStatusCode, String code, String message, Object... args){
        super(replaceMessage(message, args), cause);
        this.httpStatusCode = httpStatusCode;
        this.code = code;
        this.message = super.getMessage();
        this.args = args;
    }

    public AppException(AppError error, Object... args){
        this(error.getHttpStatusCode(), error.getCode(), error.getMessage(), args);
    }

    public AppException(Throwable cause, AppError error, Object... args){
        this(cause, error.getHttpStatusCode(), error.getCode(), error.getMessage(), args);
    }
    private static String replaceMessage(String message, Object... args) {
        return StringUtils.replaceAll(message, "\\{\\}", args);
    }

}