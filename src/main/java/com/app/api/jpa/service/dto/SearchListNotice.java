package com.app.api.jpa.service.dto;

import com.app.api.jpa.dto.Search;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchListNotice extends Search {
    LocalDate dateFrom; // 검색 시작일
    LocalDate dateTo; // 검색 종료일
}
