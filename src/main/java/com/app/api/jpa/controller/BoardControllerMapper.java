package com.app.api.jpa.controller;

import com.app.api.jpa.controller.dto.CreateNoticeRequest;
import com.app.api.jpa.mapper.ControllerMapper;
import com.app.api.jpa.mapper.ControllerMapperConfig;
import com.app.api.jpa.service.dto.CreateNotice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = ControllerMapperConfig.class, uses = {
        ControllerMapper.class})
public interface BoardControllerMapper {
    @Mapping(target = "multipartFiles" , source = "request.files")
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "createdBy", source = "authHeader")
    CreateNotice toCreateNotice(String authHeader, CreateNoticeRequest request);
}
