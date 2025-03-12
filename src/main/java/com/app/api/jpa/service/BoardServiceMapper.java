package com.app.api.jpa.service;

import com.app.api.jpa.entity.NoticeEntity;
import com.app.api.jpa.mapper.ServiceMapper;
import com.app.api.jpa.mapper.ServiceMapperConfig;
import com.app.api.jpa.service.dto.SearchNoticeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", config = ServiceMapperConfig.class, uses = {ServiceMapper.class})
public interface BoardServiceMapper {

    SearchNoticeResponse toSearchNoticeResponse(NoticeEntity noticeEntity);
}
