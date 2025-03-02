package com.app.api.common.mybatis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.app.api.admin.controller.dto.ListTeacherAttendancesResponse;
import com.app.api.common.code.TeacherType;
import com.app.api.common.util.DateUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AttendanceMapperTest {

  @Autowired
  private AttendanceMapper attendanceMapper;

  @Test
  public void listAttendanceTest() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // 쿼리 결과를 가져옵니다.
    List<Map<String, Object>> list = attendanceMapper.listAttendance("2024-01-01", "Y");

    // Response 객체 초기화
    ListTeacherAttendancesResponse.ListTeacherAttendancesResponseBuilder responseBuilder = ListTeacherAttendancesResponse.builder();
    List<ListTeacherAttendancesResponse.Schedule> schedules = new ArrayList<>();
    List<ListTeacherAttendancesResponse.Attendance> htAttendances = new ArrayList<>();
    List<ListTeacherAttendancesResponse.Attendance> ltAttendances = new ArrayList<>();

    Map<String, ListTeacherAttendancesResponse.Attendance> totalAttendanceMap = new LinkedHashMap<>();

    LocalDate compareDate = DateUtils.parseDate("2024-01-01");

    // 평균을 계산하기 위한 변수
    long htReservationSum = 0, htAttendanceSum = 0;
    long ltReservationSum = 0, ltAttendanceSum = 0;
    int htCount = 0, ltCount = 0;

    for (Map<String, Object> map : list) {

      LocalDate attendanceDate = (LocalDate) map.get("attendanceDate");
      String name = (String) map.get("name");
      String type = (String) map.get("type");
      long reservationCount = (long) map.get("reservationCount");
      long attendanceCount = (long) map.get("attendanceCount");
      BigDecimal attendanceRate = calculateRate(reservationCount, attendanceCount);

      if (!compareDate.equals(attendanceDate)) {
        // HT Avg. 추가
        if (htCount > 0) {
          htAttendances.add(
              ListTeacherAttendancesResponse.Attendance.builder()
                  .name("HT Avg.")
                  .type("HT")
                  .reservationCount(htReservationSum)
                  .attendanceCount(htAttendanceSum)
                  .attendanceRate(calculateRate(htReservationSum, htAttendanceSum))
                  .build()
          );
        }
        // LT Avg. 추가
        if (ltCount > 0) {
          ltAttendances.add(
              ListTeacherAttendancesResponse.Attendance.builder()
                  .name("LT Avg.")
                  .type("LT")
                  .reservationCount(ltReservationSum)
                  .attendanceCount(ltAttendanceSum)
                  .attendanceRate(calculateRate(ltReservationSum, ltAttendanceSum))
                  .build()
          );
        }

        // 모든 Attendances 리스트를 schedules에 추가
        List<ListTeacherAttendancesResponse.Attendance> combinedAttendances = new ArrayList<>();
        combinedAttendances.addAll(htAttendances);
        combinedAttendances.addAll(ltAttendances);

        schedules.add(
            ListTeacherAttendancesResponse.Schedule.builder()
                .date(compareDate)
                .attendances(combinedAttendances)
                .build()
        );

        // 새로운 날짜에 대한 attendance 리스트 초기화
        htAttendances = new ArrayList<>();
        ltAttendances = new ArrayList<>();
        compareDate = attendanceDate;

        // 평균 계산을 위한 변수 초기화
        htReservationSum = htAttendanceSum = ltReservationSum = ltAttendanceSum = 0;
        htCount = ltCount = 0;
      }

      // Attendance 객체를 생성하여 해당 리스트에 추가
      if ("HT".equals(type)) {
        htAttendances.add(
            ListTeacherAttendancesResponse.Attendance.builder()
                .name(name)
                .type("HT")
                .reservationCount(reservationCount)
                .attendanceCount(attendanceCount)
                .attendanceRate(attendanceRate)
                .build()
        );
        htReservationSum += reservationCount;
        htAttendanceSum += attendanceCount;
        htCount++;
      } else if ("LT".equals(type)) {
        ltAttendances.add(
            ListTeacherAttendancesResponse.Attendance.builder()
                .name(name)
                .type("LT")
                .reservationCount(reservationCount)
                .attendanceCount(attendanceCount)
                .attendanceRate(attendanceRate)
                .build()
        );
        ltReservationSum += reservationCount;
        ltAttendanceSum += attendanceCount;
        ltCount++;
      }

      // Update the totalAttendances
      totalAttendanceMap.compute(name, (k, v) -> {
        if (v == null) {
          return ListTeacherAttendancesResponse.Attendance.builder()
              .name(name)
              .type(type)
              .reservationCount(reservationCount)
              .attendanceCount(attendanceCount)
              .attendanceRate(attendanceRate)
              .build();
        } else {
          v.setReservationCount(v.getReservationCount() + reservationCount);
          v.setAttendanceCount(v.getAttendanceCount() + attendanceCount);
          v.setAttendanceRate(calculateRate(v.getReservationCount(), v.getAttendanceCount()));
          return v;
        }
      });
    }

    // 마지막 날짜의 데이터를 schedules에 추가
    // HT Avg. 추가
    if (htCount > 0) {
      htAttendances.add(
          ListTeacherAttendancesResponse.Attendance.builder()
              .name("HT Avg.")
              .type("HT")
              .reservationCount(htReservationSum)
              .attendanceCount(htAttendanceSum)
              .attendanceRate(calculateRate(htReservationSum, htAttendanceSum))
              .build()
      );
    }
    // LT Avg. 추가
    if (ltCount > 0) {
      ltAttendances.add(
          ListTeacherAttendancesResponse.Attendance.builder()
              .name("LT Avg.")
              .type("LT")
              .reservationCount(ltReservationSum)
              .attendanceCount(ltAttendanceSum)
              .attendanceRate(calculateRate(ltReservationSum, ltAttendanceSum))
              .build()
      );
    }

    // 마지막 날짜의 모든 Attendances 리스트를 schedules에 추가
    List<ListTeacherAttendancesResponse.Attendance> combinedAttendancesFinal = new ArrayList<>();
    combinedAttendancesFinal.addAll(htAttendances);
    combinedAttendancesFinal.addAll(ltAttendances);

    schedules.add(
        ListTeacherAttendancesResponse.Schedule.builder()
            .date(compareDate)
            .attendances(combinedAttendancesFinal)
            .build()
    );

    AtomicInteger idx = new AtomicInteger();
    // totalAttendances 계산
    List<ListTeacherAttendancesResponse.Attendance> totalAttendances = new ArrayList<>();
    totalAttendanceMap.forEach((name, attendance) -> {
      totalAttendances.add(attendance);
      if (attendance.getType().equals(TeacherType.HT.name())) {
        idx.getAndIncrement();
      }
    });

    // HT와 LT Avg를 적절한 위치에 추가
    totalAttendances.add(idx.get(), ListTeacherAttendancesResponse.Attendance.builder()
        .name("HT Avg.")
        .type("HT")
        .reservationCount(htReservationSum)
        .attendanceCount(htAttendanceSum)
        .attendanceRate(calculateRate(htReservationSum, htAttendanceSum))
        .build()
    );

    totalAttendances.add(totalAttendances.size(), ListTeacherAttendancesResponse.Attendance.builder()
        .name("LT Avg.")
        .type("LT")
        .reservationCount(ltReservationSum)
        .attendanceCount(ltAttendanceSum)
        .attendanceRate(calculateRate(ltReservationSum, ltAttendanceSum))
        .build()
    );

    // Response 객체에 schedules와 totalAttendances 리스트를 설정
    ListTeacherAttendancesResponse response = ListTeacherAttendancesResponse
        .builder()
        .schedules(schedules)
        .totalAttendances(totalAttendances)
        .build();

    log.debug("idx: {}", idx.get());
    log.debug("response: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
  }

  private BigDecimal calculateRate(long reservationCount, long attendanceCount) {
    if (reservationCount == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf((double) reservationCount / attendanceCount * 100).setScale(2, RoundingMode.HALF_UP);
  }

}