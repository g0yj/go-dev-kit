package com.app.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler({ServletRequestBindingException.class, BindException.class,
            HttpMediaTypeNotSupportedException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<AppErrorResponse> handleServletRequestBindingException(Exception e) {
        log.warn(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AppErrorResponse.builder()
                        .code("admin-api-9900")
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppErrorResponse> handleException(Exception e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AppErrorResponse.builder()
                        .code("admin-api-9999")
                        .message(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .build());
    }


    @ExceptionHandler(AppException.class)
    public ResponseEntity<AppErrorResponse> handleLmsException(AppErrorCode e) {
        if (e.getHttpStatusCode().is4xxClientError()) {
            log.warn(e.getMessage(), e);
        } else {
            log.error(e.getMessage(), e);
        }

        return ResponseEntity.status(e.getHttpStatusCode())
                .body(AppErrorResponse.builder()
                        .code(e.getCode())
                        .message(e.getMessage())
                        .build());
    }
}
