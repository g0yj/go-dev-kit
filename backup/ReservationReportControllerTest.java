package com.app.api.admin.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.api.admin.code.SearchReservationCode.ReportCondition;
import com.app.api.common.code.UserType;
import com.app.api.common.entity.CourseEntity;
import com.app.api.common.entity.UserEntity;
import com.app.api.common.mybatis.ReservationMapper;
import com.app.api.common.repository.CourseRepository;
import com.app.api.common.repository.TeacherRepository;
import com.app.api.common.repository.UserRepository;
import com.app.api.support.ControllerTestSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("학사보고서")
@ActiveProfiles("test")
public class ReservationReportControllerTest extends ControllerTestSupport {

  @Autowired
  JdbcTemplate jdbcTemplate;
  @Autowired
  UserRepository userRepository;
  @Autowired
  TeacherRepository teacherRepository;
  @Autowired
  CourseRepository courseRepository;
  @Autowired
  private ReservationMapper reservationMapper;

  @Test
  @Transactional
  void getList() throws Exception {
    String token = login("jenchae@naver.com", "1111", UserType.A);

    UserEntity teacher = userRepository.findByLoginId("parangoly@gmail.com").get();
    UserEntity user = userRepository.findByLoginId("puruna123@gmail.com").get();
    CourseEntity course = courseRepository.findById(69502L).get();
    assertNotNull(course);
    log.debug("## course:{}", course.getId());

    String insertSql =
        """
    INSERT INTO reservation_ (id, course_id, user_id, teacher_id, date, start_time, end_time,
    report_yn, attendance_status, is_cancel, created_on, modified_on)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
""";

    jdbcTemplate.update(insertSql, 1, course.getId(), user.getId(), teacher.getId(), LocalDate.of(2024, 1, 1),
        LocalTime.of(6, 0), LocalTime.of(6, 30), 0, "R", 0);
    jdbcTemplate.update(insertSql, 2, course.getId(), user.getId(), teacher.getId(), LocalDate.of(2024, 1, 1),
        LocalTime.of(6, 30), LocalTime.of(7, 0), 0, "R", 0);
    jdbcTemplate.update(insertSql, 3, course.getId(), user.getId(), teacher.getId(), LocalDate.of(2024, 1, 1),
        LocalTime.of(7, 0), LocalTime.of(7, 30), 0, "R", 0);
    jdbcTemplate.update(insertSql, 4, course.getId(), user.getId(), teacher.getId(), LocalDate.of(2024, 1, 1),
        LocalTime.of(11, 0), LocalTime.of(11, 30), 0, "Y", 0);
    jdbcTemplate.update(insertSql, 5, course.getId(), user.getId(), teacher.getId(), LocalDate.of(2024, 1, 1),
        LocalTime.of(11, 30), LocalTime.of(12, 0), 0, "Y", 0);

    // ALL: 5건 (변경 없음)
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-10-01")
            .param("dateTo", "2024-10-02")
            .param("order", "date")
            .param("reportCondition", ReportCondition.ALL.name())
            .param("teacherId", teacher.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.list", hasSize(5)))
        .andDo(print());

    // 미작성: 5건 (변경 없음)
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-10-01")
            .param("dateTo", "2024-10-02")
            .param("order", "date")
            .param("reportCondition", ReportCondition.REPORT.name())
            .param("teacherId", teacher.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.list", hasSize(5)))
        .andDo(print());

    // 6:00 에 학사보고서 등록
    int rowsAffected = jdbcTemplate.update("UPDATE reservation SET report = 'test' WHERE id = 1");
    assertEquals(rowsAffected, 1);

    // 미작성: 2건 (11:00, 11:30)
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-10-01")
            .param("dateTo", "2024-10-02")
            .param("order", "date")
            .param("reportCondition", ReportCondition.REPORT.name())
            .param("teacherId", teacher.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.list", hasSize(2)))
        .andExpect(jsonPath("$.list[0].startTime").value("11:00"))
        .andExpect(jsonPath("$.list[1].startTime").value("11:30"))
        .andDo(print());

    // 7:00 에 학사보고서 등록
    rowsAffected = jdbcTemplate.update("UPDATE reservation SET report = 'test' WHERE id = 3");
    assertEquals(rowsAffected, 1);

    // 미작성: 2건 (11:00, 11:30) - 변경 없음
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-10-01")
            .param("dateTo", "2024-10-02")
            .param("order", "date")
            .param("reportCondition", ReportCondition.REPORT.name())
            .param("teacherId", teacher.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.list", hasSize(2)))
        .andExpect(jsonPath("$.list[0].startTime").value("11:00"))
        .andExpect(jsonPath("$.list[1].startTime").value("11:30"))
        .andDo(print());

    // 11:00 에 학사보고서 등록
    rowsAffected = jdbcTemplate.update("UPDATE reservation SET report = 'test' WHERE id = 4");
    assertEquals(rowsAffected, 1);

    // 미작성: 0건
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-10-01")
            .param("dateTo", "2024-10-02")
            .param("order", "date")
            .param("reportCondition", ReportCondition.REPORT.name())
            .param("teacherId", teacher.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.list", hasSize(0)))
        .andDo(print());
  }

  @Test
  public void getListAsIs() throws Exception {
    String token = login("jenchae@naver.com", "1111", UserType.A);

    // 다양한 조건에 대한 테스트
    mockMvc.perform(get("/admin/v1/reservations/report")
            .header(AUTHORIZATION, token)
            .param("dateFrom", "2024-01-01")
            .param("dateTo", "2024-01-02")
            .param("reportCondition", "ALL")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(print());
  }

}