package com.app.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class StringUtils extends org.springframework.util.StringUtils {

    /**
     * text 문자열에서 regex 패턴을 찾아 args의 값으로 하나씩 치환하는 함수
     */
    public static String replaceAll(String text, String regex, Object... args) {
        if (args == null) {
            return text;
        }

        for (Object arg : args) {
            text = text.replaceFirst(regex, String.valueOf(arg));
        }

        return text;
    }

    /**
     * 모든 문자열이 null이 아니고 공백이 아닌 경우(hasText) true를 반환.
     * null, 빈 문자열(""), 공백 문자열(" ")이 하나라도 있으면 false.
     */

    public static boolean hasAllText(String... texts) {
        if (texts == null || texts.length == 0) {
            return false;
        }

        return Arrays.stream(texts).allMatch(StringUtils::hasText);
    }

    /**
     * 문자열이 null이거나 공백이라면 true 반환.
     */
    public static boolean hasNotText(String str) {
        return !hasText(str);
    }

    /**
     * 리스트의 요소를 특정 문자열 형식으로 변환 후, ,로 연결된 문자열로 반환
     */
    public static <T> String convertListToString(List<T> list, Function<T, String> mapper) {
        return list != null
                ? list.stream().map(mapper).collect(Collectors.joining(","))
                : null;
    }
}
