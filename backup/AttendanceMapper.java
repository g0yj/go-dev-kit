package com.app.api.common.mybatis;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.LocalDateTypeHandler;

@Mapper
public interface AttendanceMapper {

  @Select("""
      SELECT
          dates.attendance_date AS attendanceDate,
          t.teacher_id AS id,
          t.typ AS type,
          t.name AS name,
          t.sort AS sort,
          COALESCE(r.reservation_count, 0) AS reservationCount,
          COALESCE(s.schedule_count, 0) AS attendanceCount,
          COALESCE(ROUND((COALESCE(r.reservation_count, 0) / NULLIF(COALESCE(s.schedule_count, 0), 0)) * 100, 2), 0.00) AS attendanceRate
      FROM
          (SELECT
              DATE(#{startDate}) + INTERVAL (a.a + (10 * b.a)) DAY AS attendance_date
          FROM
              (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS a
          CROSS JOIN
              (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) AS b
          WHERE
              DATE(#{startDate}) + INTERVAL (a.a + (10 * b.a)) DAY <= LAST_DAY(DATE(#{startDate}))
          ) AS dates
      CROSS JOIN
          (SELECT
              t.user_id AS teacher_id,
              t.sort AS sort,
              t.`type` AS typ,
              u.name
          FROM
              teacher t
          JOIN
              user_ u ON t.user_id = u.id
          WHERE
              t.is_active = 1 ) AS t
      LEFT JOIN
          (SELECT
              r.teacher_id,
              r.`date` AS attendance_date,
              COUNT(*) AS reservation_count
          FROM
              reservation r
          WHERE
              r.attendance_status = #{status}
              AND YEAR(r.`date`) = YEAR(#{startDate})
              AND MONTH(r.`date`) = MONTH(#{startDate})
          GROUP BY
              r.teacher_id, r.`date`
          ) AS r
      ON
          t.teacher_id = r.teacher_id
          AND dates.attendance_date = r.attendance_date
      LEFT JOIN
          (SELECT
              s.teacher_id,
              s.`date` AS attendance_date,
              COUNT(*) AS schedule_count
          FROM
              schedule s
          WHERE
              YEAR(s.`date`) = YEAR(#{startDate})
              AND MONTH(s.`date`) = MONTH(#{startDate})
              AND MINUTE(s.start_time) IN (0, 30)
          GROUP BY
              s.teacher_id, s.`date`
          ) AS s
      ON
          t.teacher_id = s.teacher_id
          AND dates.attendance_date = s.attendance_date
      ORDER BY
          dates.attendance_date, t.sort;
      """)
  @Results({
      @Result(column = "attendanceDate", property = "attendanceDate", javaType = LocalDate.class, typeHandler = LocalDateTypeHandler.class)
  })
  List<Map<String, Object>> listAttendance(
      @Param("startDate") String startDate,
      @Param("status") String status);
}
