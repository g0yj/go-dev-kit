package com.app.api.common.mybatis;

import com.app.api.admin.controller.dto.reservation.ListReportResponse;
import com.app.api.admin.service.dto.reservation.SearchReport;
import com.app.api.common.dto.ReservationNotification;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReservationMapper {

  /**
   * 내일 예약 정보 리스트
   */
  @Select("""
        SELECT
            r.user_id,
            r.teacher_id,
            r.date,
            r.start_time,
            r.end_time,
            u.name,
            u.cell_phone
        FROM
            reservation r
        JOIN
            user_ u ON r.user_id = u.id  -- 수강생 정보 테이블과 조인
        WHERE
            r.date = CURDATE() + INTERVAL 1 DAY  -- 수업 전일을 기준으로 예약 정보 조회
            AND r.attendance_status = 'R'        -- 출결 상태가 예약(R)인 경우
            AND r.is_cancel = 0                  -- 취소되지 않은 경우
    """)
  List<ReservationNotification> findReservationsForTomorrow();

  /**
   * 학사보고서 리스트 조회
   */
  @SelectProvider(type = ReservationReportSqlProvider.class, method = "getReservations")
  List<ListReportResponse> getReservations(@Param("searchReport") SearchReport searchReport);

  /**
   * 학사보고서 카운트 조회
   */
  @SelectProvider(type = ReservationReportSqlProvider.class, method = "getReservationsCount")
  int getReservationCount(@Param("searchReport") SearchReport searchReport);

}
