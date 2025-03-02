package com.app.api.common.service;

import com.app.api.admin.controller.dto.reservation.ListReportResponse;
import com.app.api.admin.service.dto.reservation.SearchReport;
import com.app.api.common.code.AttendanceStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull; // 또는 import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationCommonService {

  private final JdbcTemplate jdbcTemplate;

  /**
   * 학사보고서 목록 조회
   */
  public List<ListReportResponse> getReservations(SearchReport searchReport) {
    String sql = buildGetReservationsSQL(searchReport);
    return jdbcTemplate.query(sql, new ReservationRowMapper(), getParameters(searchReport));
  }

  public int getReservationCount(SearchReport searchReport) {
    String sql = buildGetReservationsCountSQL(searchReport);
    return jdbcTemplate.queryForObject(sql, Integer.class, getParameters(searchReport));
  }

  private String buildGetReservationsSQL(SearchReport searchReport) {
    StringBuilder sql = new StringBuilder(
        "SELECT r.id, r.date, r.start_time AS startTime, r.end_time AS endTime, " +
        "u_t.name AS teacherName, u.name AS userName, p.name AS courseName, " +
        "c.lesson_count AS lessonCount, (c.lesson_count - c.attendance_count) AS remainCount, " +
        "c.assignment_count AS assignmentCount, r.attendance_status AS attendanceStatus, " +
        "r.report, r.today_lesson AS todayLesson, r.next_lesson AS nextLesson " +
        "FROM reservation_ r " +
        "INNER JOIN course c ON r.course_id = c.id " +
        "INNER JOIN order_product op ON c.order_product_id = op.id " +
        "INNER JOIN product p ON op.product_id = p.id " +
        "INNER JOIN user_ u ON r.user_id = u.id " +
        "INNER JOIN teacher t ON r.teacher_id = t.user_id " +
        "INNER JOIN user_ u_t ON t.user_id = u_t.id " +
        "WHERE r.is_cancel = 0"
    );

    if (searchReport.getTeacherId() != null) {
      sql.append(" AND r.teacher_id = ?");
    }
    if (searchReport.getDateFrom() != null) {
      sql.append(" AND r.date >= ?");
    }
    if (searchReport.getDateTo() != null) {
      sql.append(" AND r.date <= ?");
    }
    // 추가 조건들...

    sql.append(" ORDER BY r.date, r.start_time");

    Integer page = searchReport.getPage();
    Integer size = searchReport.getPageSize();
    if (page != null && size != null && page > 0 && size > 0) {
      sql.append(" LIMIT ? OFFSET ?");
    }

    return sql.toString();
  }

  private String buildGetReservationsCountSQL(SearchReport searchReport) {
    StringBuilder sql = new StringBuilder(
        "SELECT COUNT(*) FROM reservation_ r " +
        "INNER JOIN course c ON r.course_id = c.id " +
        "WHERE r.is_cancel = 0"
    );

    if (searchReport.getTeacherId() != null) {
      sql.append(" AND r.teacher_id = ?");
    }
    if (searchReport.getDateFrom() != null) {
      sql.append(" AND r.date >= ?");
    }
    if (searchReport.getDateTo() != null) {
      sql.append(" AND r.date <= ?");
    }
    // 추가 조건들...

    return sql.toString();
  }

  private Object[] getParameters(SearchReport searchReport) {
    List<Object> params = new ArrayList<>();

    if (searchReport.getTeacherId() != null) {
      params.add(searchReport.getTeacherId());
    }
    if (searchReport.getDateFrom() != null) {
      params.add(searchReport.getDateFrom());
    }
    if (searchReport.getDateTo() != null) {
      params.add(searchReport.getDateTo());
    }
    // 추가 파라미터들...

    Integer page = searchReport.getPage();
    Integer size = searchReport.getPageSize();
    if (page != null && size != null && page > 0 && size > 0) {
      params.add(size);
      params.add((page - 1) * size);
    } else {
      // 페이지가 null이거나 0 이하인 경우 처리
      params.add(size);
      params.add(0); // 첫 페이지부터 시작
    }

    return params.toArray();
  }

  private static class ReservationRowMapper implements RowMapper<ListReportResponse> {
    @Override
    public ListReportResponse mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
      return ListReportResponse.builder()
        .id(rs.getLong("id"))
        .date(rs.getDate("date").toLocalDate())
        .startTime(rs.getTime("startTime").toLocalTime())
        .endTime(rs.getTime("endTime").toLocalTime())
        .teacherName(rs.getString("teacherName"))
        .userName(rs.getString("userName"))
        .courseName(rs.getString("courseName"))
        .lessonCount(rs.getInt("lessonCount"))
        .remainCount(rs.getInt("remainCount"))
        .assignmentCount(rs.getInt("assignmentCount"))
        .attendanceStatus(AttendanceStatus.valueOf(rs.getString("attendanceStatus")))
        .report(rs.getString("report"))
        .todayLesson(rs.getString("todayLesson"))
        .nextLesson(rs.getString("nextLesson"))
        .build();
    }
  }
}
