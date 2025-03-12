package com.app.api.jpa.controller.dto;

import com.app.api.jpa.dto.PageRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListNoticeRequest extends PageRequest {

    LocalDate dateFrom;
    LocalDate dateTo;

    public ListNoticeRequest(Integer page, Integer limit, Integer pageSize, String order, String direction, String search, String keyword,
                             LocalDate dateFrom, LocalDate dateTo) {
        super(page, limit, pageSize, order, direction, search, keyword);
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }
}
