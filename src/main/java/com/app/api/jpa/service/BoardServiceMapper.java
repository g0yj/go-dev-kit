package com.app.api.jpa.service;

import com.app.api.jpa.mapper.ServiceMapper;
import com.app.api.jpa.mapper.ServiceMapperConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", config = ServiceMapperConfig.class, uses = {ServiceMapper.class})
public interface BoardServiceMapper {
}
