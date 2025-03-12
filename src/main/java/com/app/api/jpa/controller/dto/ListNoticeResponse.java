package com.app.api.jpa.controller.dto;

import com.app.api.jpa.dto.PageResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListNoticeResponse {
    String title;
    String modifiedBy;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime createdOn;
}
