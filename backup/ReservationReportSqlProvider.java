package com.app.api.common.mybatis;

import com.app.api.admin.code.SearchReservationCode.ReportCondition;
import com.app.api.admin.service.dto.reservation.SearchReport;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.jdbc.SQL;

public class ReservationReportSqlProvider {

  /** 전체 및 출석 */
  public String getReservations(SearchReport searchReport) {
    if (searchReport.getReportCondition().equals(ReportCondition.REPORT)) {
      return getConsecutiveReservationsSQL(searchReport);
    } else {
      return getReservationsSQL(searchReport);
    }
  }

  /** 미작성 */
  public String getReservationsCount(SearchReport searchReport) {
    if (searchReport.getReportCondition().equals(ReportCondition.REPORT)) {
      return getCountConsecutiveReservationsSQL(searchReport);
    } else {
      return getCountReservationsSQL(searchReport);
    }
  }

  private String getReservationsSQL(SearchReport searchReport) {
    return new SQL() {
      {
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
        SELECT("r.report_yn AS reportYn");
        SELECT("r.today_lesson AS todayLesson");
        SELECT("r.next_lesson AS nextLesson");

        FROM("reservation_ r");
        INNER_JOIN("course c ON r.course_id = c.id");
        INNER_JOIN("order_product op ON c.order_product_id = op.id");
        INNER_JOIN("product p ON op.product_id = p.id");
        INNER_JOIN("user_ u ON r.user_id = u.id");
        INNER_JOIN("teacher t ON r.teacher_id = t.user_id");
        INNER_JOIN("user_ u_t ON t.user_id = u_t.id");

        applyFilters(this, searchReport);

        // 정렬 조건
        if (searchReport.getOrder() != null && searchReport.getOrder().equals("teacher")) {
          ORDER_BY("r.teacher_id ASC, r.date DESC, r.start_time DESC");
        } else {
          ORDER_BY("r.date DESC, r.start_time DESC");
        }
      }
    }.toString();
  }

  private String getConsecutiveReservationsSQL(SearchReport searchReport) {
    String baseQuery = """
        WITH grouped_reservations AS (
            SELECT 
                r.*,
                u.name AS user_name,
                u_t.name AS teacher_name,
                p.name AS course_name,
                c.lesson_count,
                (c.lesson_count - c.attendance_count) AS remain_count,
                c.assignment_count,
                DATE_ADD(r.date, INTERVAL HOUR(r.start_time) HOUR) + INTERVAL (FLOOR(MINUTE(r.start_time) / 30) * 30) MINUTE AS rounded_start,
                CASE 
                    WHEN LAG(DATE_ADD(r.date, INTERVAL HOUR(r.end_time) HOUR) + INTERVAL (FLOOR(MINUTE(r.end_time) / 30) * 30) MINUTE) 
                        OVER (PARTITION BY r.user_id, r.teacher_id ORDER BY r.date, r.start_time) 
                        = DATE_ADD(r.date, INTERVAL HOUR(r.start_time) HOUR) + INTERVAL (FLOOR(MINUTE(r.start_time) / 30) * 30) MINUTE
                    THEN 0
                    ELSE 1
                END AS is_new_group
            FROM reservation_ r 
            INNER JOIN course c ON r.course_id = c.id
            INNER JOIN order_product op ON c.order_product_id = op.id
            INNER JOIN product p ON op.product_id = p.id
            INNER JOIN user_ u ON r.user_id = u.id
            INNER JOIN teacher t ON r.teacher_id = t.user_id
            INNER JOIN user_ u_t ON t.user_id = u_t.id
            WHERE r.is_cancel = 0 
        """;

    String filters = applyFilters(searchReport);

    String groupingQuery = """
        ),
        reservation_groups AS (
            SELECT 
                *,
                SUM(is_new_group) OVER (PARTITION BY user_id, teacher_id ORDER BY date, start_time) AS group_id
            FROM grouped_reservations
        ),
        group_report_status AS (
            SELECT 
                group_id,
                MAX(report_yn) AS group_report_yn
            FROM reservation_groups
            GROUP BY group_id
        )
        SELECT DISTINCT
            rg.id,
            rg.date,
            rg.start_time AS startTime,
            rg.end_time AS endTime,
            rg.teacher_name AS teacherName,
            rg.user_name AS userName,
            rg.course_name AS courseName,
            rg.lesson_count AS lessonCount,
            rg.remain_count AS remainCount,
            rg.assignment_count AS assignmentCount,
            rg.attendance_status AS attendanceStatus,
            rg.report,
            rg.report_yn AS reportYn,
            rg.today_lesson AS todayLesson,
            rg.next_lesson AS nextLesson
        FROM reservation_groups rg
        JOIN group_report_status grs ON rg.group_id = grs.group_id
        WHERE grs.group_report_yn = 0
        ORDER BY rg.date, rg.start_time
        """;

    return baseQuery + (filters.isEmpty() ? "" : " AND " + filters) + groupingQuery;
  }

  /** 카운트 */
  private String getCountReservationsSQL(SearchReport searchReport) {
    return new SQL() {
      {
        SELECT("COUNT(*)");
        FROM("reservation_ r");
        INNER_JOIN("course c ON r.course_id = c.id");
        INNER_JOIN("order_product op ON c.order_product_id = op.id");
        INNER_JOIN("product p ON op.product_id = p.id");
        INNER_JOIN("user_ u ON r.user_id = u.id");
        INNER_JOIN("teacher t ON r.teacher_id = t.user_id");
        INNER_JOIN("user_ u_t ON t.user_id = u_t.id");

        applyFilters(this, searchReport);
      }
    }.toString();
  }

  private String getCountConsecutiveReservationsSQL(SearchReport searchReport) {
    String baseQuery = """
        WITH grouped_reservations AS (
            SELECT 
                r.*,
                DATE_ADD(date, INTERVAL HOUR(start_time) HOUR) + INTERVAL (FLOOR(MINUTE(start_time) / 30) * 30) MINUTE AS rounded_start,
                CASE 
                    WHEN LAG(DATE_ADD(date, INTERVAL HOUR(end_time) HOUR) + INTERVAL (FLOOR(MINUTE(end_time) / 30) * 30) MINUTE)
                        OVER (PARTITION BY user_id, teacher_id ORDER BY date, start_time)
                        = DATE_ADD(date, INTERVAL HOUR(start_time) HOUR) + INTERVAL (FLOOR(MINUTE(start_time) / 30) * 30) MINUTE
                    THEN 0
                    ELSE 1
                END AS is_new_group
            FROM reservation_ r 
            JOIN course c ON r.course_id = c.id
            JOIN user_ u ON r.user_id = u.id
            WHERE r.is_cancel = 0 
        """;

    String filters = applyFilters(searchReport);

    String countQuery = """
        ),
        reservation_groups AS (
            SELECT 
                *,
                SUM(is_new_group) OVER (PARTITION BY user_id, teacher_id ORDER BY date, start_time) AS group_id
            FROM grouped_reservations
        ),
        group_report_status AS (
            SELECT 
                group_id,
                MAX(report_yn) AS group_report_yn
            FROM reservation_groups
            GROUP BY group_id
        )
        SELECT COUNT(DISTINCT rg.id) 
        FROM reservation_groups rg 
        JOIN group_report_status grs ON rg.group_id = grs.group_id 
        WHERE grs.group_report_yn = 0
        """;

    return baseQuery + (filters.isEmpty() ? "" : " AND " + filters) + countQuery;
  }

  /** 검색 조건 */
  private void applyFilters(SQL sql, SearchReport searchReport) {
    // 강사 필터링
    if (searchReport.getTeacherId() != null) {
      sql.WHERE("r.teacher_id = #{searchReport.teacherId}");
    }

    // 수강기간 시작일 필터링
    if (searchReport.getDateFrom() != null) {
      sql.WHERE("r.date >= #{searchReport.dateFrom}");
    }

    // 수강기간 종료일 필터링
    if (searchReport.getDateTo() != null) {
      sql.WHERE("r.date <= #{searchReport.dateTo}");
    }

    // 사용자 이름 검색 필터링
    if (searchReport.hasSearch()) {
      sql.WHERE("u.name LIKE CONCAT('%', #{searchReport.keyword}, '%')");
    }

    // 출석 여부 필터링
    if (searchReport.getReportCondition().equals(ReportCondition.ATTENDANCE)) {
      sql.WHERE("r.attendance_status = 'Y'");
    }
  }

  /** 검색 조건 */
  private String applyFilters(SearchReport searchReport) {
    List<String> conditions = new ArrayList<>();

    if (searchReport.getTeacherId() != null) {
        conditions.add("r.teacher_id = #{searchReport.teacherId}");
    }
    if (searchReport.getDateFrom() != null) {
        conditions.add("r.date >= #{searchReport.dateFrom}");
    }
    if (searchReport.getDateTo() != null) {
        conditions.add("r.date <= #{searchReport.dateTo}");
    }
    if (searchReport.hasSearch()) {
        conditions.add("u.name LIKE CONCAT('%', #{searchReport.keyword}, '%')");
    }
    if (searchReport.getReportCondition().equals(ReportCondition.ATTENDANCE)) {
        conditions.add("r.attendance_status = 'Y'");
    }

    return String.join(" AND ", conditions);
  }
}