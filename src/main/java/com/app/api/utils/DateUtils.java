package com.app.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {
    /**
     * 📌 LocalDate → Date 변환
     */
    public static Date convertToDate(LocalDate localDate) {
        if (localDate == null) {
            throw new IllegalArgumentException("localDate가 null일 수 없습니다.");
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    /**
     * 📌 Date → LocalDate 변환 메서드
     */
    public static LocalDate convertToLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
