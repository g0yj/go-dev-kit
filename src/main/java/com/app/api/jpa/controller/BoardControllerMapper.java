package com.app.api.jpa.controller;

import com.app.api.jpa.controller.dto.CreateNoticeRequest;
import com.app.api.jpa.controller.dto.ListNoticeRequest;
import com.app.api.jpa.controller.dto.ListNoticeResponse;
import com.app.api.jpa.dto.PageResponse;
import com.app.api.jpa.mapper.ControllerMapper;
import com.app.api.jpa.mapper.ControllerMapperConfig;
import com.app.api.jpa.service.dto.CreateNotice;
import com.app.api.jpa.service.dto.SearchListNotice;
import com.app.api.jpa.service.dto.SearchNoticeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", config = ControllerMapperConfig.class, uses = {
        ControllerMapper.class})
public interface BoardControllerMapper {
    @Mapping(target = "list", source = "noticePage.content")
    @Mapping(target = "totalCount", source = "noticePage.totalElements")
    PageResponse<ListNoticeResponse> toListNoticeResponse(Page<SearchNoticeResponse> noticePage, SearchListNotice searchListNotice);

    ListNoticeResponse toListNoticeResponse(SearchNoticeResponse searchNoticeResponse);

    List<ListNoticeResponse> toListNoticeResponseList(List<SearchNoticeResponse> searchNoticeResponseList);

    @Mapping(target = "multipartFiles" , source = "request.files")
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "createdBy", source = "authHeader")
    CreateNotice toCreateNotice(String authHeader, CreateNoticeRequest request);

    SearchListNotice toSearchListNotice (ListNoticeRequest request);
}
