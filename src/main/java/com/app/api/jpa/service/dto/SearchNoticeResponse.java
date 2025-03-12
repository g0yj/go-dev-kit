package com.app.api.jpa.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchNoticeResponse {
    String title;
    String view;
    LocalDateTime createdOn;
    String modifiedBy;
}
