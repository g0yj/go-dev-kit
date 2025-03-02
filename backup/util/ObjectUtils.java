package com.app.api.common.util;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ObjectUtils extends org.springframework.util.ObjectUtils {

  public static boolean isAllEmpty(Object... objects) {
    return Arrays.stream(objects).allMatch(org.springframework.util.ObjectUtils::isEmpty);
  }

  /**
   * nullSafeEquals()
   */
  public static boolean equals(Object obj1, Object obj2) {
    return org.springframework.util.ObjectUtils.nullSafeEquals(obj1, obj2);
  }

  /**
   * !nullSafeEquals()
   */
  public static boolean notEquals(Object obj1, Object obj2) {
    return !equals(obj1, obj2);
  }

  public static boolean isNotEmpty(Object obj) {
    return !isEmpty(obj);
  }
}
