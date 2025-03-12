package com.app.api.jpa.dto;

import com.app.api.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Search {

    @Builder.Default
    int page = 1;

    @Builder.Default
    int limit = 10;

    @Builder.Default
    int pageSize = 10;

    @Builder.Default
    String order = "createdOn";

    @Builder.Default
    String direction = "desc";

    String search;
    String keyword;

    public PageRequest toPageRequest() {
        return PageRequest.of(page - 1, limit, Sort.Direction.valueOf(direction.toUpperCase()), order);
    }

    public boolean hasSearch() {
        return StringUtils.hasAllText(search, keyword);
    }
}