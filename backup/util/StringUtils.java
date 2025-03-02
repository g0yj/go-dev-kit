package com.app.api.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class StringUtils extends org.springframework.util.StringUtils {

  public static String replaceAll(String text, String regex, Object... args) {
    if (args == null) {
      return text;
    }

    for (Object arg : args) {
      text = text.replaceFirst(regex, String.valueOf(arg));
    }

    return text;
  }

  public static boolean hasAllText(String... texts) {
    if (texts == null || texts.length == 0) {
      return false;
    }

    return Arrays.stream(texts).allMatch(StringUtils::hasText);
  }

  public static boolean hasNotText(String str) {
    return !hasText(str);
  }

  public static <T> String convertListToString(List<T> list, Function<T, String> mapper) {
    return list != null
        ? list.stream().map(mapper).collect(Collectors.joining(","))
        : null;
  }
}
