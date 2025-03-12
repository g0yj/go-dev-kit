package com.app.api.jpa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {

  List<T> list;
  long totalCount;
  int page;
  int limit;
  int pageSize;

  @Getter
  @Setter
  public static class PageResponseData {
    private long listNumber;
  }

  public List<T> getList() {
    if (list == null) {
      return List.of();
    }

    if (list.isEmpty() || !(list.get(0) instanceof PageResponseData)) {
      return list;
    }

    for (int i = 0; i < list.size(); i++) {
      ((PageResponseData) list.get(i)).setListNumber(totalCount - (long) (page - 1) * limit - i);
    }

    return list;
  }

  public int getTotalPage() {
    return (int) Math.ceil((double) totalCount / limit);
  }

  public int getStartPage() {
    return ((page - 1) / pageSize) * pageSize + 1;
  }

  public int getEndPage() {
    return Math.min(getStartPage() + pageSize - 1, getTotalPage());
  }

  @JsonProperty("hasPrev")
  public boolean hasPrev() {
    return getStartPage() > 1;
  }

  @JsonProperty("hasNext")
  public boolean hasNext() {
    return getEndPage() < getTotalPage();
  }

  @JsonProperty("isFirst")
  public boolean isFirst() {
    return getStartPage() == 1;
  }

  @JsonProperty("isLast")
  public boolean isLast() {
    return getEndPage() == getTotalPage();
  }
}
