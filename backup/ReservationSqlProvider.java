package com.app.api.common.mybatis;

import com.app.api.admin.code.SearchReservationCode.ReportCondition;
import com.app.api.admin.service.dto.reservation.SearchReport;
import org.apache.ibatis.jdbc.SQL;

public class ReservationSqlProvider {

  /**
   * 학사보고서 리스트 조회 쿼리 생성
   */
  public String getReservations(SearchReport searchReport) {
    if (searchReport.getReportCondition().equals(ReportCondition.REPORT)) {
      return getConsecutiveReservations(searchReport);
    } else {
      return new SQL() {{
        SELECT("r.id AS id");
        SELECT("r.date AS date");
        SELECT("r.start_time AS startTime");
        SELECT("r.end_time AS endTime");
        SELECT("u_t.name AS teacherName");
        SELECT("u.name AS userName");
        SELECT("p.name AS courseName");
        SELECT("c.lesson_count AS lessonCount");
        SELECT("(c.lesson_count - c.attendance_count) AS remainCount");
        SELECT("c.assignment_count AS assignmentCount");
        SELECT("r.attendance_status AS attendanceStatus");
        SELECT("r.report AS report");
        SELECT("r.today_lesson AS todayLesson");
        SELECT("r.next_lesson AS nextLesson");

//        FROM("reservation r");
        FROM("reservation_ r");
        INNER_JOIN("course c ON r.course_id = c.id");
        INNER_JOIN("order_product op ON c.order_product_id = op.id");
        INNER_JOIN("product p ON op.product_id = p.id");
        INNER_JOIN("user_ u ON r.user_id = u.id");
        INNER_JOIN("teacher t ON r.teacher_id = t.user_id");
        INNER_JOIN("user_ u_t ON t.user_id = u_t.id");

        // 강사 필터링
        if (searchReport.getTeacherId() != null) {
          WHERE("r.teacher_id = #{searchReport.teacherId}");
        }

        // 수강기간 시작일 필터링
        if (searchReport.getDateFrom() != null) {
          WHERE("r.date >= #{searchReport.dateFrom}");
        }

        // 수강기간 종료일 필터링
        if (searchReport.getDateTo() != null) {
          WHERE("r.date <= #{searchReport.dateTo}");
        }

        // 사용자 이름 검색 필터링
        if (searchReport.hasSearch()) {
          WHERE("u.name LIKE CONCAT('%', #{searchReport.keyword}, '%')");
        }

        // 출석 여부 필터링
        if (searchReport.getReportCondition().equals(ReportCondition.ATTENDANCE)) {
          WHERE("r.attendance_status = 'Y'");
        }

        // 정렬 조건
        if (searchReport.getOrder() != null && searchReport.getOrder().equals("teacher")) {
          ORDER_BY("r.teacher_id ASC, r.date DESC, r.start_time DESC");
        } else {
          ORDER_BY("r.date DESC, r.start_time DESC");
        }

      }}.toString();
    }
  }

  /**
   * 학사보고서 리스트에 대한 총 카운트 조회 쿼리 생성
   */
  public String getReservationCount(SearchReport searchReport) {
    return new SQL() {{
      SELECT("COUNT(*)");

//      FROM("reservation r");
      FROM("reservation_ r");
      INNER_JOIN("course c ON r.course_id = c.id");
      INNER_JOIN("order_product op ON c.order_product_id = op.id");
      INNER_JOIN("product p ON op.product_id = p.id");
      INNER_JOIN("user_ u ON r.user_id = u.id");
      INNER_JOIN("teacher t ON r.teacher_id = t.user_id");
      INNER_JOIN("user_ u_t ON t.user_id = u_t.id");

      // 강사 필터링
      if (searchReport.getTeacherId() != null) {
        WHERE("r.teacher_id = #{searchReport.teacherId}");
      }

      // 수강기간 시작일 필터링
      if (searchReport.getDateFrom() != null) {
        WHERE("r.date >= #{searchReport.dateFrom}");
      }

      // 수강기간 종료일 필터링
      if (searchReport.getDateTo() != null) {
        WHERE("r.date <= #{searchReport.dateTo}");
      }

      // 사용자 이름 검색 필터링
      if (searchReport.hasSearch()) {
        WHERE("u.name LIKE CONCAT('%', #{searchReport.keyword}, '%')");
      }

      // 출석 여부 필터링
      if (searchReport.getReportCondition().equals(ReportCondition.ATTENDANCE)) {
        WHERE("r.attendance_status = 'Y'");
      }

    }}.toString();
  }

  /**
   * 연속된 예약 쿼리 생성
   */
  public String getConsecutiveReservations(SearchReport searchReport) {
    StringBuilder subQuery = new StringBuilder();
    subQuery.append("SELECT r.id, r.course_id, r.user_id, r.teacher_id, r.date, r.start_time, r.end_time, r.report_yn, ");
    subQuery.append("r.attendance_status, r.report, r.today_lesson, r.next_lesson, ");
    subQuery.append("LAG(r.end_time) OVER (PARTITION BY r.date, r.teacher_id, r.user_id ORDER BY r.start_time) AS prev_end_time ");
    subQuery.append("FROM reservation_ r ");
    subQuery.append("JOIN course c ON r.course_id = c.id ");

    // 동적 WHERE 조건 추가
    if (searchReport.getTeacherId() != null) {
      subQuery.append("WHERE r.teacher_id = #{searchReport.teacherId} ");
    }
    if (searchReport.getDateFrom() != null) {
      subQuery.append("AND r.date >= #{searchReport.dateFrom} ");
    }
    if (searchReport.getDateTo() != null) {
      subQuery.append("AND r.date <= #{searchReport.dateTo} ");
    }
    subQuery.append("AND r.report_yn = 0 ");

    return new SQL() {{
      SELECT("cr.id AS id, cr.date AS date, cr.start_time AS startTime, cr.end_time AS endTime");
      SELECT("u_t.name AS teacherName, u.name AS userName, p.name AS courseName");
      SELECT("c.lesson_count AS lessonCount, (c.lesson_count - c.attendance_count) AS remainCount");
      SELECT("c.assignment_count AS assignmentCount, cr.attendance_status AS attendanceStatus");
      SELECT("cr.report AS report, cr.today_lesson AS todayLesson, cr.next_lesson AS nextLesson");

      FROM("(" + subQuery.toString() + ") cr");
      INNER_JOIN("course c ON cr.course_id = c.id");
      INNER_JOIN("order_product op ON c.order_product_id = op.id");
      INNER_JOIN("product p ON op.product_id = p.id");
      INNER_JOIN("user_ u ON cr.user_id = u.id");
      INNER_JOIN("teacher t ON cr.teacher_id = t.user_id");
      INNER_JOIN("user_ u_t ON t.user_id = u_t.id");

      WHERE("cr.prev_end_time IS NULL OR cr.prev_end_time <= cr.start_time");
      ORDER_BY("cr.teacher_id ASC, cr.date DESC, cr.start_time DESC");
    }}.toString();
  }

  /**
   * 연속된 예약의 총 카운트 조회 쿼리
   */
  public String getConsecutiveReservationCount(SearchReport searchReport) {
    // 서브쿼리를 StringBuilder로 생성
    StringBuilder subQuery = new StringBuilder();
    subQuery.append("SELECT r.id, r.course_id, r.user_id, r.teacher_id, r.date, r.start_time, r.end_time, ");
    subQuery.append("r.report_yn, r.attendance_status, r.report, r.today_lesson, r.next_lesson, ");
    subQuery.append("LAG(r.end_time) OVER (PARTITION BY r.date, r.teacher_id, r.user_id ORDER BY r.start_time) AS prev_end_time ");
    subQuery.append("FROM reservation_ r ");
    subQuery.append("JOIN course c ON r.course_id = c.id ");
    subQuery.append("WHERE r.report_yn = 0 ");

    // 동적 WHERE 조건 추가
    if (searchReport.getTeacherId() != null) {
      subQuery.append("AND r.teacher_id = #{searchReport.teacherId} ");
    }
    if (searchReport.getDateFrom() != null) {
      subQuery.append("AND r.date >= #{searchReport.dateFrom} ");
    }
    if (searchReport.getDateTo() != null) {
      subQuery.append("AND r.date <= #{searchReport.dateTo} ");
    }

    return new SQL() {{
      SELECT("COUNT(*)");
      FROM("(" + subQuery.toString() + ") cr");
      WHERE("cr.prev_end_time IS NULL OR cr.prev_end_time <= cr.start_time");
    }}.toString();
  }
}