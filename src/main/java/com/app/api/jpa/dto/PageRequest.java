package com.app.api.jpa.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageRequest {

  Integer page;
  Integer limit;
  Integer pageSize;
  String order;
  String direction; // asc, desc
  String search;
  String keyword;
}
