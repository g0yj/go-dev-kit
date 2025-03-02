package com.app.api.common.service;

import static org.junit.jupiter.api.Assertions.*;

import com.app.api.admin.code.SearchReservationCode.ReportCondition;
import com.app.api.admin.controller.dto.reservation.ListReportResponse;
import com.app.api.admin.service.dto.reservation.SearchReport;
import com.app.api.common.entity.CourseEntity;
import com.app.api.common.entity.UserEntity;
import com.app.api.common.repository.CourseRepository;
import com.app.api.common.repository.TeacherRepository;
import com.app.api.common.repository.UserRepository;
import com.app.api.common.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class ReservationCommonServiceTest {

  @Autowired
  JdbcTemplate jdbcTemplate;
  @Autowired
  ReservationCommonService reservationCommonService;
  @Autowired
  UserRepository userRepository;
  @Autowired
  TeacherRepository teacherRepository;
  @Autowired
  CourseRepository courseRepository;

  @Test
  @Transactional
  void test() {
    UserEntity teacher = userRepository.findByLoginId("parangoly@gmail.com").get();
    UserEntity user = userRepository.findByLoginId("puruna123@gmail.com").get();
    CourseEntity course = courseRepository.findById(69502L).get();
    assertNotNull(course);
    log.debug("## course:{}", course.getId());

    String insertSql = """
            INSERT INTO reservation_ (id, course_id, user_id, teacher_id, date, start_time, end_time, 
            report_yn, attendance_status, is_cancel, created_on, modified_on)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """;

    jdbcTemplate.update(insertSql, 1, course.getId(), user.getId(), teacher.getId(),
        LocalDate.of(2024, 1, 1),
        LocalTime.of(6, 0), LocalTime.of(6, 30), 0, "R", 0);
    jdbcTemplate.update(insertSql, 2, course.getId(), user.getId(), teacher.getId(),
        LocalDate.of(2024, 1, 1),
        LocalTime.of(6, 30), LocalTime.of(7, 0), 0, "R", 0);
    jdbcTemplate.update(insertSql, 3, course.getId(), user.getId(), teacher.getId(),
        LocalDate.of(2024, 1, 1),
        LocalTime.of(7, 0), LocalTime.of(7, 30), 0, "R", 0);
    jdbcTemplate.update(insertSql, 4, course.getId(), user.getId(), teacher.getId(),
        LocalDate.of(2024, 1, 1),
        LocalTime.of(11, 0), LocalTime.of(11, 30), 0, "Y", 0);
    jdbcTemplate.update(insertSql, 5, course.getId(), user.getId(), teacher.getId(),
        LocalDate.of(2024, 1, 1),
        LocalTime.of(11, 30), LocalTime.of(12, 0), 0, "Y", 0);

    SearchReport searchReport = SearchReport.builder()
        .page(1)
        .pageSize(10)
        .limit(10)
        .dateFrom(DateUtils.parseDate("2024-01-01"))
        .dateTo(DateUtils.parseDate("2024-01-02"))
        .order("date")
        .reportCondition(ReportCondition.REPORT)
        .teacherId(teacher.getId())
        .build();

    List<ListReportResponse> reportResponseList = reservationCommonService.getReservations(
        searchReport);
    assertEquals(reportResponseList.size(), 5);
    log.debug("## reportResponseList:{}", reportResponseList);

    int rowsAffected = jdbcTemplate.update("UPDATE reservation_ SET report_yn = 1 WHERE id = 1");
    assertEquals(rowsAffected, 1);

    reportResponseList = reservationCommonService.getReservations(
        searchReport);
//    assertEquals(reportResponseList.size(), 2);
    log.debug("## reportResponseList:{}", reportResponseList);
  }
}