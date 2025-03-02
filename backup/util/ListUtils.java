package com.app.api.common.util;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ListUtils extends CollectionUtils {

  public static boolean isNotEmpty(List<?> list) {
    return !isEmpty(list);
  }

  public static <T> List<T> of(Iterable<T> iterable) {
    List<T> list = new ArrayList<>();
    iterable.forEach(list::add);
    return list;
  }
}
