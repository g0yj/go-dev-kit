package com.app.api.common.util;

public interface NumberUtils {
  static boolean isPositive(Number number) {
    return number != null && number.doubleValue() > 0;
  }
}
